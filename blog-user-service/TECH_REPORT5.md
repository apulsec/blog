# blog-user-service 技术报告

## 服务概览

- **定位**：博客平台的用户域微服务，负责用户注册、认证凭证存取、基础资料维护，以及文章互动通知的产生与提供。
- **技术栈**：Spring Boot 3.2.5、Spring Cloud 2023 (Eureka、OpenFeign)、MyBatis-Plus、Spring Security (仅提供 `PasswordEncoder`)、Spring AMQP、JJWT、PostgreSQL、RabbitMQ。默认 Java 17。
- **工程结构**：核心入口 `BlogUserServiceApplication`，通过 `@MapperScan` 注册 MyBatis-Plus Mapper 并启用服务注册发现。配置类覆盖安全策略、RabbitMQ 队列绑定以及静态资源映射。

## 配置与基础设施

- `SecurityConfig`
  - 全部接口默认放行，适用于受网关保护或内网环境。
  - 提供 `BCryptPasswordEncoder` 供注册时加密密码。
  - CORS 开放本地前端域。
- `RabbitMQConfig`
  - 声明 `blog.article.notifications` 主题交换机、`article.interaction` 路由键、持久化队列 `blog.notifications.article-interactions`，用于消费文章互动消息。
  - 配置 `Jackson2JsonMessageConverter`，`RabbitTemplate` 备用（当前服务主要消费消息）。
- `WebConfig`
  - 为 `/uploads/**` 添加静态资源映射；根据 `app.avatar.upload-dir` 自动创建目录，提供头像上传访问路径。
- `application.yml`
  - Postgres 数据源、RabbitMQ 凭证、文件上传大小限制、Eureka 注册配置、MyBatis-Plus 全局配置、JWT Base64 秘钥、头像存储路径等均可参数化。

## 数据模型与持久化

- **表划分**：
  - `t_user`（`User` 实体）：存储公共档案（邮箱、头像、简介、状态、时间戳）。
  - `t_user_auth`（`UserAuth` 实体）：按 `identityType` 存储认证信息（用户名/邮箱 -> BCrypt 哈希），支持多种登录方式。
  - `t_user_notification`（`Notification` 实体）：记录通知（收件人、触发者、关联文章、内容、已读标记、时间）。
- **访问层**：`UserMapper`、`UserAuthMapper`、`NotificationMapper` 均继承 MyBatis-Plus `BaseMapper`，使用 `QueryWrapper`/`UpdateWrapper` 构建查询与更新。

## 核心业务逻辑

### 用户服务 (`UserServiceImpl`)

- **注册 (`registerUser`)**：
  1. 校验用户名、邮箱在 `t_user_auth` 唯一（`identity_type` + `identifier`）。
  2. 插入 `User` 记录（状态初始 0=正常），再写入 `UserAuth`：用户名必填、邮箱可选；密码使用 BCrypt hash。
  3. 使用事务确保 `User` 与 `UserAuth` 同步写入；任何异常回滚。
  4. 返回 `UserDTO`（包含邮箱、头像、简介、用户名等公共信息）。
- **用户查询 (`getUserById`)**：按 ID 取 `User`，并通过 `userAuthMapper` 查找用户名凭证，装配成 `UserDTO`。未找到直接抛 `RuntimeException`。
- **认证详情 (`findUserAuthDetailsByIdentifier`)**：
  1. 依据 `identityType` + `identifier` 查询 `UserAuth`。
  2. 回补 `User` 资料并创建 `UserAuthDetailsDTO`（含 userId、hashed credential、账户状态）。供 `blog-auth-service` 登录时校验。
  3. 若关联 `User` 不存在则抛 `IllegalStateException`，保证数据一致性。
- **头像更新 (`updateAvatar`)**：
  1. 验证文件类型（必须 `image/*`），并读取用户记录。
  2. `storeAvatarFile` 按 `userId` + 时间戳生成文件名，保存到 `uploads/avatars`。
  3. 删除旧头像（若在 `/uploads/` 下），更新用户记录并返回最新 `UserDTO`。
- **用户名更新 (`updateUsername`)**：
  1. 校验新用户名非空、长度合法、与原用户名不同。
  2. 通过 `UserAuth` 检查唯一性（排除当前用户）。
  3. 更新 `t_user_auth` 的用户名记录及 `t_user` 的更新时间，返回 `UserDTO`。

### 通知服务 (`NotificationServiceImpl`)

- **通知创建 (`createNotification`)**：
  1. 将 `CreateNotificationRequest` 映射为 `Notification`，若未提供自定义内容，则用 `resolveContent` 根据类型生成默认文本（含触发者昵称、文章标题）。
  2. 插入记录后调用 `enrich`：批量加载触发者 `User` 与用户名 `UserAuth`，补充头像与昵称，返回 `NotificationDTO`。
- **查询 (`getNotificationsForUser`)**：
  - 支持 `unreadOnly` 过滤和 `limit`（缺省 20，上限 100）；按创建时间倒序查询并调用 `enrich`。
- **标记已读 (`markAsRead`/`markAllAsRead`)**：使用 `UpdateWrapper` 修改 `is_read` 字段；若单条更新返回 0，抛 `IllegalArgumentException` 表示不存在或无权限。

## 控制器与外部接口

- `UserController`
  - `POST /api/users/register`：公开注册，JSR-303 校验，冲突由 `IllegalStateException` 驱动统一错误响应。
  - `GET /api/users/{id}`：返回公共资料，供前端及 `blog-article-service` 获取作者信息。
  - `GET /api/users/internal/auth-details`：内部凭证接口，返回 `UserAuthDetailsDTO`；需部署时通过网络隔离或网关保护。
  - `POST /api/users/me/avatar`、`PUT /api/users/me/username`：需 `Authorization` 头，利用 `JwtUtil` 从 token 读取 `userId`。
- `NotificationController`
  - `POST /api/notifications/internal`：供其他服务（或 RabbitMQ 消费器备用）直接写通知。
  - `GET /api/notifications/me`、`POST /api/notifications/{id}/read`、`POST /api/notifications/read-all`：面向登录用户，需 JWT 解码用户身份。

## 消息集成与跨服务协同

- `ArticleNotificationListener`
  - `@RabbitListener` 监听 `ARTICLE_NOTIFICATION_QUEUE`，收到 `CreateNotificationRequest` 后调用 `NotificationService.createNotification`，失败记录错误不重试。
- 与 `blog-article-service` 协作：文章点赞/评论时在对方服务中使用 `NotificationClient` 调用 `createNotification` 或发 MQ 消息，本服务负责持久化与查询。
- 与 `blog-auth-service` 协作：登录流程通过 Feign 调用 `getUserAuthDetails`；本服务提供 BCrypt hash 与账号状态。

## JWT 工具

- `JwtUtil` 使用配置中的 Base64 秘钥解析 token，提取 `userId` 供控制器认证；解析失败返回 `null` 并触发 `401`。

## 异常处理与校验

- `GlobalExceptionHandler` 对 `MethodArgumentNotValidException`、`IllegalStateException`、`IllegalArgumentException`、`ResponseStatusException`、其他异常分别生成统一 JSON（含 message、status、可选 error）。
- 控制层往往捕获业务异常后转为 `ResponseStatusException`（如头像上传失败、通知未找到）。

## 测试现状

- `NotificationServiceImplTest` 使用 Mockito 验证通知创建/查询的丰富化逻辑、已读操作异常分支。尚未覆盖用户注册、头像上传、RabbitMQ 监听等功能，可按优先级扩展。

## 代码关系总结

- 控制器 → 服务 → Mapper：形成典型三层架构，服务层封装业务事务，Mapper 层使用 MyBatis-Plus 操作 Postgres。
- 服务间调用：`blog-auth-service` 通过 Feign 拉取凭证；`blog-article-service` 通过 RabbitMQ/REST 写入通知；JWT 秘钥在多个服务中保持一致。
- 文件系统：头像上传目录由 `UserServiceImpl` 与 `WebConfig` 协同管理，对外通过 `/uploads/**` 暴露。

## 可扩展与注意事项

- 注册时邮箱凭证与用户名共用同一密码 hash，未来若支持重置需同步两条 `UserAuth` 记录。
- 内部接口（auth-details、notifications/internal）需在部署侧加防护，避免被公网访问。
- 当前使用 JJWT 0.9.1（旧版 API），若与其他服务统一升级 0.12.x 需调整代码。
- 头像文件删除失败被忽略，如需审计可加日志或异步清理。
- 通知内容生成支持中文模板；若需国际化，可引入 i18n 服务或模板引擎。
