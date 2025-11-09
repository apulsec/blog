# blog-article-service 技术报告

## 组件概览

- **运行时栈**：Spring Boot 3.2 + Spring Cloud 2023，Java 17。集成 MyBatis-Plus、Spring Data JPA、Spring Data MongoDB、Spring AMQP、OpenFeign、Resilience4j。
- **构建与依赖**：`pom.xml` 引入 `spring-boot-starter-web`、JPA/Mongo、MyBatis-Plus、Netflix Eureka、OpenFeign、RabbitMQ、Resilience4j、JJWT 等，统一由 Spring Boot Maven 插件打包。
- **模块入口**：`src/main/java/com/example/blog/article/BlogArticleServiceApplication.java`，启用 `@EnableDiscoveryClient`、`@EnableFeignClients`、`@MapperScan`，同时加载配置类 `MyBatisPlusConfig`、`RabbitMQConfig`。
- **主要子系统**：
  - REST 控制层：`controller` 包内四个控制器覆盖文章、点赞、评论、指标读接口。
  - 服务层：`ArticleService` / `ArticleServiceImpl` 汇总业务流程。
  - 数据访问：MyBatis-Plus Mapper（文章、标签、指标）、JPA Repository（点赞、评论）、MongoRepository（文章正文），多存储协同。
  - 外部交互：Feign 客户端访问 `blog-user-service` 与通知 API；RabbitMQ 发布互动消息。
  - 实用工具：`JwtUtil` 负责 JWT 解析，提供服务端认证上下文。

## 配置与基础设施

- `MyBatisPlusConfig` 注册 `MybatisPlusInterceptor`，附加 `PaginationInnerInterceptor(DbType.POSTGRE_SQL)`，保证分页 SQL 自动适配 PostgreSQL。
- `RabbitMQConfig` 定义主题交换机 `blog.article.notifications` 与统一路由键 `article.interaction`，同时配置 `Jackson2JsonMessageConverter` + `RabbitTemplate`，用于发布 JSON 消息。
- `application.yml` 聚合多数据源与云组件设置：
  - PostgreSQL、MongoDB、RabbitMQ 均通过环境变量可配置。
  - `spring.cloud.openfeign.circuitbreaker.enabled=true` 打开 Feign 层断路保护。
  - Resilience4j 针对 `userService` 定制阈值与窗口。
  - JWT Base64 秘钥、Eureka 注册、Feign 超时等均集中于此。

## 数据模型与持久化策略

- **关系数据库 (PostgreSQL)**：
  - `Article`（`t_article`）使用 MyBatis-Plus 管理，附带 `likes_count`、`comments_count` 计数列，且保留 JPA `@Column` 兼容性。
  - `Tag`（`t_tag`）与 `ArticleTag`（`t_article_tag`）形成多对多关系，利用 MyBatis-Plus 维护。
  - `ArticleLike`、`Comment` 采用 Spring Data JPA（`JpaRepository`），便于实体图维护与事务管理。
  - `ArticleMetrics`（`article_metrics`）映射 Spark 生成的快照，MyBatis-Plus 提供自定义 SQL。
- **文档数据库 (MongoDB)**：`ArticleContent` 文档存储长文本内容，`ArticleContentRepository` 以 articleId 字符串对照关系库主键。
- **跨存储一致性**：`ArticleServiceImpl#createArticle` / `updateArticle` / `deleteArticle` 通过 Spring `@Transactional` 保证 PostgreSQL 操作一致；MongoDB 写入/删除位于同事务方法中，但 Mongo 不参与 XA，需要外层流程处理潜在不一致（当前实现顺序提交）。

## 服务层关键逻辑

- **分页读取**：`getPublishedArticles`、`getArticlesByStatus`、`getArticlesByTag`、`searchArticles`、`getArticlesByAuthor` 均构造 MyBatis `QueryWrapper` + `Page`，查询后通过 `buildArticleDTOPage` 装配作者信息与标签列表。
  - 作者信息通过 `getAuthorInfoWithCircuitBreaker`（`@CircuitBreaker` 注解）调用 `UserServiceClient`，失败时由 `getAuthorInfoFallback` 返回占位数据。
  - 标签装载：`loadArticleTags` -> 读取 `ArticleTag` -> 批量 `Tag` -> DTO。
- **文章创建 (`createArticle`)**：
  1. 构建 `Article` 记录，初始化互动计数，落库 PostgreSQL。
  2. 若带正文，则写入 `ArticleContent` 文档。
  3. `saveArticleTags` 逐个标签执行 `findOrCreateTag`（按名称生成稳定颜色），建立关系列。
  4. 初始化 `ArticleMetrics` 快照（调用 `ArticleMetricsMapper.insertSnapshot`），为后续分析提供基线。
- **文章更新/删除**：更新同步刷新 MongoDB 正文与标签关系；删除会先清关系表、再删主表、最后删文档。
- **点赞/取消点赞**：
  - 利用 `ArticleLikeRepository` 防重复，新增后调用 `ArticleMapper.incrementLikesCount` 更新计数。
  - `adjustArticleMetricsCounters` 根据 delta 调用 `ArticleMetricsMapper.upsertInteractionSnapshot`，以当日为维度增量更新热度。
  - `sendArticleInteractionNotification` 做通知过滤：作者自身操作或缺少收件人不推送。
- **评论 CRUD**：
  - `createComment` 关联父评论（若传 `parentId`），保存后更新文章计数 + 指标。
  - 查询走 `CommentRepository.findByArticleIdAndParentIsNull` 分页，递归收集子评论并映射成 `CommentDTO`（保持创建时间排序）。
  - 删除限定作者本人操作，撤销计数和指标。
- **指标读取**：`getLatestArticleMetrics` 查询最近快照；`getHotArticles` 规范化窗口与数量后，调用 `ArticleMetricsMapper.findHotArticles` 汇总热度排名。

## 控制层与 API 设计

- `ArticleController`：
  - `GET /api/articles` 多条件查询（分页、状态、标签、关键词、作者）；内部统一使用 1-based page 的 MyBatis。
  - `POST /api/articles` 创建文章，`JwtUtil` 自 JWT 提取 `authorId` 并覆盖请求体，失败返回 401。
  - `GET /api/articles/{id}`、`PUT /api/articles/{id}`、`DELETE /api/articles/{id}` 分别读取、更新、删除；更新同样校验 JWT。
  - `GET /api/articles/tags` 返回全部标签。
- `LikeController`：`POST` 点赞、`DELETE` 取消、`GET /status` 查询当前用户点赞状态；三者均要求 `Authorization` 头并从 Token 提取 `userId`。
- `CommentController`：`POST` 创建评论（payload 包含 `content`、可选 `parentId`），`GET` 分页列表，`DELETE` 删除评论；同样基于 JWT。
- `ArticleMetricsController`：
  - `GET /api/articles/{id}/metrics` 提供最新快照。
  - `GET /api/articles/hot` 读取热门文章榜单（默认近 7 天 Top 5）。

## 外部交互与消息流

- **Feign 客户端**：
  - `UserServiceClient` / `NotificationClient` 均通过 `@FeignClient` 声明，`fallbackFactory` 日志化异常并返回默认值。
  - `UserServiceClientFallbackFactory` 记录降级原因，返回默认作者资料。
  - `NotificationClientFallbackFactory` 在通知失败时记录警告但不抛错，保证业务路径继续。
- **RabbitMQ 通知**：`RabbitArticleNotificationPublisher.publish` 将 `CreateNotificationRequest` 序列化发送；失败捕获 `AmqpException` 仅记录警告，不重试。
- **与 analytics-spark-job 集成**：
  - 创作与互动事件通过 `ArticleMetricsMapper` 紧凑更新 `article_metrics`，与 Spark 作业生成的每日全量快照共享表结构。
  - `ArticleMetricsController` 对外暴露 Spark 结果，用于前端消费。

## 安全与鉴权

- `JwtUtil` 从 Base64 编码密钥解析 JWT，提供 `getUserIdFromToken`、`getUsernameFromToken`。控制器检查 `Authorization` 是否 `Bearer ` 开头并提取 userId。
- 异常处理：若 Token 无效，`ArticleController` 返回 `401 UNAUTHORIZED`；点赞、评论控制器在 token 解析失败时会抛出 NPE（当前实现假定 header 总是存在，可考虑增强校验）。

## 断路器与容错

- `ArticleServiceImpl#getAuthorInfoWithCircuitBreaker` 使用 `@CircuitBreaker(name = "userService")` 绑定到 `resilience4j.circuitbreaker.instances.userService` 配置。
- Fallback (`getAuthorInfoFallback`) 记录错误并返回占位作者，避免文章列表加载失败。
- Feign fallback 工厂确保外部依赖不可用时，通知、用户数据流降级但不中断主流程。

## 测试覆盖

- `src/test/java/.../ArticleServiceImplTest.java` 使用 Mockito：
  - 验证点赞/评论会触发通知、生成内容预览、处理异常路径。
  - 校验重复点赞与作者自点赞不会重复通知。
  - 确认查询结果包含互动计数字段。
- 单测尚未覆盖 Mongo 写入、标签生成、指标 upsert 等逻辑，可按需扩展。

## 代码协作关系总结

- 控制层 -> `ArticleServiceImpl` 汇总业务；服务层串联多存储与外部系统。
- PostgreSQL 既由 MyBatis-Plus 操作（文章、标签、指标），也由 JPA 操作（点赞、评论），两套 ORM 在 `@Transactional` 场景中共同工作。
- MongoDB 存放大文本，RabbitMQ 负责事件驱动通知，Feign 调用用户服务补全展示信息。
- Spark 批处理写入的 `article_metrics` 表体现在 REST API 中，为指标消费提供只读视图。
