# 博客平台技术报告合集

## analytics-spark-job 技术报告

### 模块概览

- 项目类型：Maven 构建的单模块 Java 17 应用，产出可执行 JAR（`maven-shade-plugin` 指定入口 `com.example.blog.analytics.ArticleMetricsJob`）。
- 主要依赖：Apache Spark 3.5.1（`spark-core_2.12` 与 `spark-sql_2.12`，标记为 `provided` 以适配集群运行时提供的 Spark）、PostgreSQL JDBC 驱动 42.7.3。
- 代码结构：`src/main/java/com/example/blog/analytics/ArticleMetricsJob.java` 为唯一业务类，实现从博客数据库读取交互数据、聚合并写回 `article_metrics` 表的批任务。

### 入口流程（`main` 方法）

1. 调用 `parseArgs` 解析命令行参数，支持 `--jdbc-url`、`--db-user`、`--db-password`、`--metrics-table`、`--master`、`--metric-date`、`--shuffle-partitions`、`--log-level` 等键值对。
2. 设置默认参数：
   - JDBC：`jdbc:postgresql://localhost:15432/blog_article_db`，用户 `postgres`，密码 `password`。
   - Spark master：`local[*]`，shuffle 分区默认 `1`。
   - 指标日期：若未指定 `metric-date`，使用当前 UTC 日期。
3. 调用 `validateTableName` 确保指标表名仅包含字母数字下划线，避免 SQL 注入。
4. 构造 `SparkSession`：显式配置 `spark.sql.shuffle.partitions` 与 `spark.sql.session.timeZone = UTC`，随后设置日志级别。
5. 执行 `runAggregation`，捕获异常并在失败时输出堆栈、返回非零退出码。
6. 始终执行 `spark.stop()` 释放资源。

### 数据读取与转换

`runAggregation` 负责核心计算流程：

- **读取基础文章列表**：通过 JDBC 读取子查询 `SELECT id AS article_id FROM t_article`，确保未有交互的文章也纳入结果集。
- **读取点赞聚合**：子查询 `SELECT article_id, COUNT(*) AS likes_count FROM article_likes GROUP BY article_id`。
- **读取评论聚合**：子查询 `SELECT article_id, COUNT(*) AS comments_count FROM comments GROUP BY article_id`。
- Spark 以 `Dataset<Row>` 形式加载三张临时表，均使用 PostgreSQL 驱动和统一的连接参数。

#### 聚合逻辑

1. 左连接基础文章集与点赞、评论聚合（键 `article_id`），保留所有文章。
2. 利用 `functions.coalesce` 将缺失的 `likes_count`、`comments_count` 填充为 `0`。
3. 添加派生字段：
   - `metric_date`：恒为当前批次日期（`java.sql.Date`）。
   - `hot_score`：业务评分=点赞数 ×2 + 评论数。
   - `updated_at`：写入时间戳（`current_timestamp`）。
4. 调整字段类型：`likes_count`、`comments_count` 强制为 `int`，`hot_score` 为 `double`，输出列顺序为 `article_id, metric_date, likes_count, comments_count, hot_score, updated_at`。
5. `aggregated.count()` 获取计划写入的行数；若无文章则记录提示并跳过写入。

### 数据写入与幂等设计

- 在写入前调用 `purgeExistingSnapshots`：使用 JDBC `PreparedStatement` 执行 `DELETE FROM <metrics_table> WHERE metric_date = ?`，确保同一 `metric_date` 的历史数据被清除，实现基于日期的幂等覆盖。
- 使用 `SaveMode.Append` 经 JDBC 将 DataFrame 写入目标表，避免 Spark 在 DDL 上产生附加副作用。
- 完成后打印写入行数、目标日期和表名，便于运维审计。

### 辅助工具方法

- `parseArgs`：遍历 `--key value` 形式的参数列表；支持无值参数（值记为 `null`），结果存入 `HashMap`。
- `validateTableName`：基于正则 `[a-zA-Z0-9_]+` 校验，若失败抛出 `IllegalArgumentException`。
- `parseDate`：对空串返回 `null`，否则按 `yyyy-MM-dd` 默认格式解析为 `LocalDate`。

### 运行时交互关系

- **Spark ↔ 数据库**：所有读写都通过 JDBC 完成；Spark 端不缓存中间结果，仅依赖 DataFrame 操作链执行分布式查询与写入。
- **本地 Java ↔ 数据库**：删除旧快照操作不走 Spark，直接用 JDBC 执行，避免受 Spark 重试机制影响，确保删除一次即可生效。
- **配置参数**：Spark session 配置仅影响当前任务；数据库连接参数贯穿读取与写入阶段，需保持一致性。

### 可扩展性提示（纯技术角度）

- 如需支持更多指标（如浏览量），可在 JDBC 子查询中添加对应聚合并在 DataFrame 转换阶段扩展列。
- 若目标库为分区表，可将 `metric_date` 作为分区键，并在写入时增加 `option("truncate", false)` 等 JDBC 配置。
- 当前 `hot_score` 为线性规则，若需复用可抽象为 UDF 或配置化权重。
- 对于大规模数据，可将 `shuffle-partitions` 参数外部化，以适配集群资源。

---

## blog-article-service 技术报告

### 组件概览

- **运行时栈**：Spring Boot 3.2 + Spring Cloud 2023，Java 17。集成 MyBatis-Plus、Spring Data JPA、Spring Data MongoDB、Spring AMQP、OpenFeign、Resilience4j。
- **构建与依赖**：`pom.xml` 引入 `spring-boot-starter-web`、JPA/Mongo、MyBatis-Plus、Netflix Eureka、OpenFeign、RabbitMQ、Resilience4j、JJWT 等，统一由 Spring Boot Maven 插件打包。
- **模块入口**：`src/main/java/com/example/blog/article/BlogArticleServiceApplication.java`，启用 `@EnableDiscoveryClient`、`@EnableFeignClients`、`@MapperScan`，同时加载配置类 `MyBatisPlusConfig`、`RabbitMQConfig`。
- **主要子系统**：
  - REST 控制层：`controller` 包内四个控制器覆盖文章、点赞、评论、指标读接口。
  - 服务层：`ArticleService` / `ArticleServiceImpl` 汇总业务流程。
  - 数据访问：MyBatis-Plus Mapper（文章、标签、指标）、JPA Repository（点赞、评论）、MongoRepository（文章正文），多存储协同。
  - 外部交互：Feign 客户端访问 `blog-user-service` 与通知 API；RabbitMQ 发布互动消息。
  - 实用工具：`JwtUtil` 负责 JWT 解析，提供服务端认证上下文。

### 配置与基础设施

- `MyBatisPlusConfig` 注册 `MybatisPlusInterceptor`，附加 `PaginationInnerInterceptor(DbType.POSTGRE_SQL)`，保证分页 SQL 自动适配 PostgreSQL。
- `RabbitMQConfig` 定义主题交换机 `blog.article.notifications` 与统一路由键 `article.interaction`，同时配置 `Jackson2JsonMessageConverter` + `RabbitTemplate`，用于发布 JSON 消息。
- `application.yml` 聚合多数据源与云组件设置：
  - PostgreSQL、MongoDB、RabbitMQ 均通过环境变量可配置。
  - `spring.cloud.openfeign.circuitbreaker.enabled=true` 打开 Feign 层断路保护。
  - Resilience4j 针对 `userService` 定制阈值与窗口。
  - JWT Base64 秘钥、Eureka 注册、Feign 超时等均集中于此。

### 数据模型与持久化策略

- **关系数据库 (PostgreSQL)**：
  - `Article`（`t_article`）使用 MyBatis-Plus 管理，附带 `likes_count`、`comments_count` 计数列，且保留 JPA `@Column` 兼容性。
  - `Tag`（`t_tag`）与 `ArticleTag`（`t_article_tag`）形成多对多关系，利用 MyBatis-Plus 维护。
  - `ArticleLike`、`Comment` 采用 Spring Data JPA（`JpaRepository`），便于实体图维护与事务管理。
  - `ArticleMetrics`（`article_metrics`）映射 Spark 生成的快照，MyBatis-Plus 提供自定义 SQL。
- **文档数据库 (MongoDB)**：`ArticleContent` 文档存储长文本内容，`ArticleContentRepository` 以 articleId 字符串对照关系库主键。
- **跨存储一致性**：`ArticleServiceImpl#createArticle` / `updateArticle` / `deleteArticle` 通过 Spring `@Transactional` 保证 PostgreSQL 操作一致；MongoDB 写入/删除位于同事务方法中，但 Mongo 不参与 XA，需要外层流程处理潜在不一致（当前实现顺序提交）。

### 服务层关键逻辑

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

### 控制层与 API 设计

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

### 外部交互与消息流

- **Feign 客户端**：
  - `UserServiceClient` / `NotificationClient` 均通过 `@FeignClient` 声明，`fallbackFactory` 日志化异常并返回默认值。
  - `UserServiceClientFallbackFactory` 记录降级原因，返回默认作者资料。
  - `NotificationClientFallbackFactory` 在通知失败时记录警告但不抛错，保证业务路径继续。
- **RabbitMQ 通知**：`RabbitArticleNotificationPublisher.publish` 将 `CreateNotificationRequest` 序列化发送；失败捕获 `AmqpException` 仅记录警告，不重试。
- **与 analytics-spark-job 集成**：
  - 创作与互动事件通过 `ArticleMetricsMapper` 紧凑更新 `article_metrics`，与 Spark 作业生成的每日全量快照共享表结构。
  - `ArticleMetricsController` 对外暴露 Spark 结果，用于前端消费。

### 安全与鉴权

- `JwtUtil` 从 Base64 编码密钥解析 JWT，提供 `getUserIdFromToken`、`getUsernameFromToken`。控制器检查 `Authorization` 是否 `Bearer ` 开头并提取 userId。
- 异常处理：若 Token 无效，`ArticleController` 返回 `401 UNAUTHORIZED`；点赞、评论控制器在 token 解析失败时会抛出 NPE（当前实现假定 header 总是存在，可考虑增强校验）。

### 断路器与容错

- `ArticleServiceImpl#getAuthorInfoWithCircuitBreaker` 使用 `@CircuitBreaker(name = "userService")` 绑定到 `resilience4j.circuitbreaker.instances.userService` 配置。
- Fallback (`getAuthorInfoFallback`) 记录错误并返回占位作者，避免文章列表加载失败。
- Feign fallback 工厂确保外部依赖不可用时，通知、用户数据流降级但不中断主流程。

### 测试覆盖

- `src/test/java/.../ArticleServiceImplTest.java` 使用 Mockito：
  - 验证点赞/评论会触发通知、生成内容预览、处理异常路径。
  - 校验重复点赞与作者自点赞不会重复通知。
  - 确认查询结果包含互动计数字段。
- 单测尚未覆盖 Mongo 写入、标签生成、指标 upsert 等逻辑，可按需扩展。

### 代码协作关系总结

- 控制层 -> `ArticleServiceImpl` 汇总业务；服务层串联多存储与外部系统。
- PostgreSQL 既由 MyBatis-Plus 操作（文章、标签、指标），也由 JPA 操作（点赞、评论），两套 ORM 在 `@Transactional` 场景中共同工作。
- MongoDB 存放大文本，RabbitMQ 负责事件驱动通知，Feign 调用用户服务补全展示信息。
- Spark 批处理写入的 `article_metrics` 表体现在 REST API 中，为指标消费提供只读视图。

---

## blog-auth-service 技术报告

### 组件概览

- **运行时栈**：Spring Boot 3.2.5 + Spring Security 6，配合 Spring Cloud 2023 (OpenFeign、Eureka) 与 Spring Data Redis；Java 17 目标。
- **系统职责**：集中处理登录、注销、令牌刷新、令牌校验，产出 JWT 并维护 Redis 黑名单；所有业务入口集中在 `AuthController` → `AuthService` → `AuthServiceImpl` 调度。
- **构建依赖**：`pom.xml` 引入 JJWT 0.12.5（拆分 api/impl/jackson）、Spring Boot Web/Validation/Actuator、Spring Security、Redis Starter、OpenFeign、Netflix Eureka；测试层仅保留 `spring-boot-starter-test`（暂无落地用例）。

### 配置层设计

- `SecurityConfig`
  - 公开 `/api/auth/**` 与 `/actuator/**`，其余路径默认拒绝（本服务内部 API）；关闭 CSRF，启用 `SessionCreationPolicy.STATELESS`，明确 JWT 模式。
  - 配置全局 `PasswordEncoder = BCryptPasswordEncoder` 及 `AuthenticationManager`（注入 Spring Security 默认实现）。
  - CORS 策略允许本地前端域名（`5173`/`3000`），同源信任可按环境调整。
- `application.yml`
  - 通过变量注入 Redis、Eureka、Feign 超时、JWT 秘钥与过期时间（1h access + 7d refresh），默认 profile `local`。
  - `spring.cloud.openfeign.circuitbreaker.enabled=true` 保证 Feign 整合 Resilience4j。
- `RedisHealthIndicator` 实现 Actuator `HealthIndicator`，运行时 ping Redis，若失败在 `/actuator/health` 展示错误并提示 JWT 黑名单功能受阻。

### 安全/认证管线

- `UserDetailsServiceImpl`
  - 使用 Feign `UserServiceClient` 查询 `blog-user-service` 内部鉴权信息 `/api/users/internal/auth-details`。
  - 根据用户名包含 `@` 判定 `identityType`（email vs username）。
  - 若状态非 0（启用），抛出 `UsernameNotFoundException` 终止登录。
  - 自定义 `CustomUserDetails` 扩展 `User`，附带 `userId` 供后续 JWT claim 使用。
- `AuthenticationManager` 结合 `CustomUserDetails` 与密码编码器完成登录校验，失败统一由 `GlobalExceptionHandler` 捕获转换为中文提示。

### JWT 管理 (`JwtTokenProvider`)

- 使用 Base64 编码秘钥初始化 `SecretKey`（`@PostConstruct`）。
- **Access Token**：payload 包含 `sub=username`、`jti` 随机 UUID、`userId`、签发/过期时间；`jti` 为黑名单索引。
- **Refresh Token**：payload 仅含 `sub` 与时效，过期时间更长。
- 公开工具方法：`getUsernameFromToken`、`getJtiFromToken`、`getExpirationDateFromToken`、`validateToken`；验证时捕获并记录具体异常类型。

### 认证业务流程 (`AuthServiceImpl`)

- **登录 (`login`)**
  1. 构造 `UsernamePasswordAuthenticationToken` 调用 `authenticationManager.authenticate`。
  2. 将认证对象放入 `SecurityContextHolder`（方便下游过滤器访问）。
  3. 从 `CustomUserDetails` 取出 `username` 与 `userId`，生成 access/refresh token。
- **注销 (`logout`)**
  1. 从 `Authorization` Bearer Header 截取 token。
  2. 通过 `validateToken` 签名校验后获取 `jti` 与过期时间。
  3. 委托 `JwtBlacklistService.blacklistToken` 将 `jti` 写入 Redis (`jwt:blacklist:<jti>`)，TTL=剩余有效期。
- **刷新令牌 (`refreshToken`)**
  1. 校验 refresh token；基于 `sub` 决定 identityType。
  2. 调用 `UserServiceClient.getUserAuthDetails` 获取 `userId`（保障 access token claim 完整性）。
  3. 生成新一对 access/refresh token。
- **令牌校验 (`validateToken`)**
  1. 签名/时效验证失败直接抛错。
  2. 读取 `jti` 检查 Redis 黑名单，存在则拒绝。
  3. 返回 `sub=username` 供网关或下游服务反查用户上下文。

### Redis 黑名单 (`JwtBlacklistService`)

- 键前缀 `jwt:blacklist:`，value 固定 `blacklisted`。
- 使用 `StringRedisTemplate` 的 `set` + TTL 防止无界膨胀；校验通过 `hasKey`。
- 与 `RedisHealthIndicator` 联动，Redis 不可用时功能降级：黑名单写/读失败但流程仍能进行（未显式捕获，依赖 Redis 可用性）。

### 对外接口 (`AuthController`)

- `POST /api/auth/login`：入参 `LoginRequest`（JSR 303 校验），回返 `AuthResponse`；异常走全局处理。
- `POST /api/auth/logout`：需 `Authorization` 头。
- `POST /api/auth/refresh`：刷新请求体 `TokenRefreshRequest`。
- `GET /api/auth/validate`：网关内部调用，返回用户名字符串或 401。
- 控制器仅承担参数解析与响应包装，所有业务分支封装在 `AuthService`。

### 异常与降级策略

- `GlobalExceptionHandler` 提供统一 JSON：校验错误聚合字段消息，登录失败/用户不存在/非法状态等映射成明确中文提示，其余异常返回 500，并携带 `error` 字段方便排查。
- Feign Fallback (`UserServiceClientFallback`) 记录错误并返回 `null`；`UserDetailsServiceImpl` 根据空值抛 `UsernameNotFoundException`，确保认证流程一致终止。

### 健康监控与运维

- Actuator 暴露 `health`、`info`，Redis 检查合并展示。
- `SecurityConfig` 允许匿名访问 `/actuator/**`，便于容器探针；若需限制可后续加 IP 白名单或 key。

### 代码协作关系

- `AuthController` -> `AuthServiceImpl` -> `AuthenticationManager` / `JwtTokenProvider` / `JwtBlacklistService` / `UserServiceClient`。
- `UserDetailsServiceImpl` 与 `UserServiceClient` 构成 Spring Security 内部认证桥梁。
- `JwtBlacklistService` 与 Redis 紧耦合，配合 `validateToken` 完成注销后的即时失效。
- `RedisHealthIndicator` 与 Actuator 集成，辅助定位 Redis 故障对黑名单能力的影响。

### 未覆盖与可扩展点

- 当前缺少集成/单元测试，可针对 `AuthServiceImpl`、`JwtBlacklistService` 编写基于 Mock 的校验用例。
- 登录接口未实现多因子/验证码；若需可在 `AuthServiceImpl.login` 前插入额外校验。
- 刷新令牌流程每次调用都会重新下发 refresh token，需保证客户端替换；若需单次刷新，可在 Redis 存储 refresh token 状态。
- 黑名单机制依赖 Redis，可通过熔断或 fallback 增强不可用时的降级策略（例如拒绝注销或标记只读模式）。

---

## blog-eureka-server 技术报告

### 概述

- 基于 Spring Boot 3.2.5 的应用，依托 Netflix Eureka 为博客微服务套件提供服务发现能力。
- 单一入口 `BlogEurekaServerApplication` 同时使用 `@SpringBootApplication` 与 `@EnableEurekaServer` 完成注册中心装配。
- 运行环境定位为 Java 17，并依赖 Spring Cloud 2023.0.1（Kilburn）BOM 对 Netflix 生态进行版本对齐。

### 构建与依赖

- `pom.xml` 通过 Spring Boot Parent 管理依赖版本，并导入 Spring Cloud BOM 保持 Netflix 相关组件一致。
- 关键依赖：
  - `spring-cloud-starter-netflix-eureka-server` 提供注册中心 Servlet 端点、注册表存储及租约管理。
  - `spring-boot-starter-web` 引入内嵌 Tomcat 与 MVC 基础设施（控制台视图由该 starter 附带的 JSP/Thymeleaf 模板渲染）。
  - `spring-boot-starter-actuator` 暴露健康检查与信息端点，便于外部监控。
  - `spring-boot-starter-test` 在测试范围启用（当前暂无测试），为后续扩展提供基础。
  - 可选依赖 Lombok 已引入但尚未使用，方便未来增加配置类或工具类。
- 构建插件 `spring-boot-maven-plugin` 产出可执行的 fat JAR，供 Docker 运行阶段使用。

### 应用启动

- `src/main/java/com/example/blog/eureka/BlogEurekaServerApplication.java`
  ```java
  @SpringBootApplication
  @EnableEurekaServer
  public class BlogEurekaServerApplication {
      public static void main(String[] args) {
          SpringApplication.run(BlogEurekaServerApplication.class, args);
      }
  }
  ```
  - `@EnableEurekaServer` 激活自动配置，暴露 `/eureka/*` REST 端点、托管注册表，并提供 HTML 控制台。
  - 应用类位于 `com.example.blog.eureka` 包下，Spring 组件扫描覆盖该命名空间（当前未定义额外 Bean）。

### 配置（`application.yml`）

- `server.port: 8761` 使用 Eureka 常用端口。
- `spring.application.name: blog-eureka-server` 使日志、Actuator 元数据中的应用标识保持一致。
- 管理端点开放 `health` 与 `info`，满足监控集成需求。
- Eureka 设置：
  - `client.register-with-eureka=false` 与 `client.fetch-registry=false` 禁止服务器自注册或从其他节点同步注册表，适合单节点开发环境。
  - `client.serviceUrl.defaultZone=http://localhost:8761/eureka/` 定义客户端注册的目标地址，同时用于控制台生成访问链接。
  - `server.enable-self-preservation=false` 关闭自我保护机制，一旦心跳缺失即快速驱逐实例，方便本地调试时及时更新路由信息（生产环境应重新评估）。

### Docker 打包

- 多阶段 Dockerfile：
  1. 构建阶段使用 `maven:3.9.6-eclipse-temurin-17` 运行 `mvn -B -DskipTests package` 生成 fat JAR。
  2. 运行阶段基于 `eclipse-temurin:17-jre`，拷贝产物、暴露 8761 端口，并以 `java -jar` 启动。
- 此方案保持镜像精简，同时确保运行时 Java 版本与构建阶段一致。

### 运行职责与交互

- 托管 Eureka 控制台（`/`）及 `/eureka` 下的 REST 端点，接受文章、认证、用户等服务的注册、心跳与状态更新。
- 配合前端网关与后端微服务，可通过逻辑服务 ID 实现位置透明，避免硬编码主机地址。
- 关闭自我保护后，客户端必须保持及时心跳；一旦中断即被驱逐，可防止迭代开发中出现陈旧路由。

### 扩展点

- 可在 `application.yml` 中追加对等节点、认证等配置，或在同包下新增 Spring `@Configuration` 类强化功能。
- 通过调整管理端点暴露列表，可扩展 Actuator（如 `/metrics`、`/prometheus`）。
- 若面向生产硬化，建议：
  - 引入 Spring Security 保护控制台访问。
  - 在反向代理或内嵌 Tomcat 中配置 TLS。
  - 部署多节点集群（在 `eureka.client.serviceUrl.defaultZone` 中列出多个对等地址），并重新启用自我保护机制。

---

## blog-frontend 技术报告

### 概述

- 基于 Vue 3（script setup）与 Vite 5 构建的单页应用，对接博客微服务体系。
- 选用 Element Plus 作为 UI 组件库，使用 Pinia 管理状态，Vue Router 负责路由导航，Axios 封装服务请求。
- 通过 Vite 开发代理接入后端服务：文章服务（8082）、认证服务（8081）、用户/通知服务与静态上传（8083）。
- 突出个性化体验：个人工作台、类实时通知轮询、Markdown 驱动的文章内容展示与丰富的主题皮肤。

### 构建与工具链

- `package.json` 提供 `dev`、`build`、`preview` 脚本；尚未配置 lint/测试工具。
- `vite.config.js` 将 `@` 映射到 `src/`，并为三大后端服务及上传接口设置 HTTP 代理。
- 全局样式位于 `src/assets/main.css`，用于定制 Element Plus 主题变量并提供通用样式。

### 应用外壳

- `src/main.js`：初始化 Vue 应用，挂载 Pinia、路由及 Element Plus。
- `App.vue`：在顶层包裹 `GlobalNav`、`<router-view>` 与 `LoginModal`，确保认证相关流程的响应式体验。

### 状态管理（Pinia）

#### `authStore`

- 持久化管理 `accessToken`、`refreshToken` 与 `user`（存入 `localStorage`）。
- 登录时写入令牌，设置 Axios 默认 Authorization 头，解析 JWT 载荷获取用户 ID，并通过 `fetchMe`（用户服务查询）回填资料。
- 包含 `register`（委派用户服务并自动登录）、`logout`（可选调用黑名单 API）、`updateAvatar`（向 `/api/users/me/avatar` 上传 FormData）、`tryRefreshToken`（对接刷新接口）等方法。
- 暴露 `openLogin`/`closeLogin`、`showLoginModal` 等模态框控制，提供会话过期处理以清理状态并提示重新登录。

#### `articleStore`

- 集中维护分页/筛选状态：文章状态、标签（前端多选过滤）、关键词、作者过滤等。
- `fetchArticles` 请求 `/articles`（后端页码从 0 开始），统一处理计数字段，并在作者过滤导致缺失交互数据时，通过详情接口补齐点赞/评论数。
- 附带 `fetchTags`、`filterByStatus`、`toggleTag`、`searchArticles`、`filterByAuthor`、`clearFilters` 等辅助方法。

#### `notificationsStore`

- 对通知相关 REST 接口做缓存与轮询封装。
- 维护未读计数（computed）、加载状态、错误信息，并提供 `startPolling`（默认 30 秒，在个人主页延长至 45 秒），登出时优雅停止并清理。
- 暴露 `markAsRead`、`markAllAsRead` 与状态更新工具函数。

### API 层

- `src/services/api.js`：共享 Axios 实例，baseURL 为 `/api`，请求前附加 Authorization 头，遇到 401 清理令牌；封装文章相关 CRUD、标签、热榜、点赞及评论分页接口。
- `src/services/auth.js`：为认证（`/api/auth`）与用户（`/api/users`）分别创建 Axios 客户端，手动注入令牌，支持 401 时基于刷新令牌的重试，并暴露 `login`、`register`、`logout`、`refreshToken`、`getUserById`、`uploadAvatar`、`validateToken`。
- `src/services/notifications.js`：面向 `/api/notifications` 的客户端，附带认证头，提供通知查询、单条已读、全部已读等操作。

### 路由与导航

- `router/index.js`：定义五个路由（首页 Feed、文章详情、个人主页、创建、编辑）。受保护路由在未认证时会跳转到 `/` 并弹出登录框，同时通过 `redirect` 查询参数保留原始目标。
- 顶部导航组件实时展示认证状态、未读徽章（来自 Pinia）与快速操作入口。

### 视图与功能流程

#### `BlogView.vue`

- 作为社区首页，挂载时重置作者过滤并拉取已发布文章。
- 侧边栏包含热榜卡片（调用 `articleApi.getHotArticles`，统计 7 天、最多 6 条）、标签筛选与基础统计。
- 搜索输入做 500ms 防抖；清空时复位筛选。分页调用 store 获取数据，从而共享过滤条件。
- CTA 按钮引导用户查看更多精选或触发特定身份动作。

#### `HomeView.vue`

- 个人控制台（需登录）；挂载/监听认证变化时进行用户信息初始化、设置 `currentAuthorId`、加载文章与标签、拉取通知并启动轮询。
- 侧栏提供头像管理：文件选择校验、`AvatarCropperDialog`（CropperJS）裁剪后再通过 store 上传。
- 集成 `NotificationsPanel`，支持刷新、单条/全部已读等操作；搜索与筛选复用 store 逻辑且保持作者范围。

#### `ArticleDetailView.vue`

- 展示文章元信息、封面、经 DOMPurify 清洗的 Markdown 内容、标签、点赞与评论。
- 支持点赞/取消点赞的乐观更新，失败时回退，并在认证状态变化时刷新点赞状态。
- 评论区基于后台返回结构（`content`、`totalElements` 等）分页加载，提交表单校验有效，删除仅限评论作者；含时间格式化工具。
- 监听路由参数变化以重新获取文章与评论，确保认证切换后点赞信息同步。

#### `CreateArticleView.vue`

- 表单采用向导式布局，结合 Element Plus 展示摘要/正文字数与卡片预览（为新增标签生成配色）。
- 校验标题、摘要、正文长度后，调用 `articleApi.createArticle` 并显式指定状态（`DRAFT` 或 `PUBLISHED`）。

#### `EditArticleView.vue`

- 挂载时加载文章详情与标签列表，填充表单并沿用创建页的校验规则（摘要长度上限 200）。
- 封面提供错误兜底占位图，提交时可在发布/草稿间切换，成功后跳回首页。

### 可复用组件

- **GlobalNav.vue**：吸顶导航，展示品牌、路由链接、未读徽章（来自通知 store），登录状态下会主动拉取通知。
- **ArticleCard.vue**：通用文章卡片，支持可选的编辑/删除操作（携带 Element 确认提示）并在删除后向父组件抛出事件。
- **LoginModal.vue**：带 Tab 的登录/注册弹窗，直接绑定 auth store 的模态状态；打开时重置表单，引用了可选的 `auth.clearError()`（未实现但做了类型保护）。
- **NotificationsPanel.vue**：可折叠面板，提供图标/内容/操作插槽，内置刷新与已读控制，并展示未读数与时间格式化。
- **AvatarCropperDialog.vue**：封装 CropperJS，输出 512×512 的圆形 PNG 头像，提供缩放/旋转控制与画布预览，向调用方返回清洗后的 `File` 对象。

### 样式与体验

- 各视图自定义大量 CSS，实现渐变背景、卡片阴影、自适应栅格等效果，丰富默认 Element Plus 风格。
- `assets/main.css` 统一主题变量并重写按钮、标签、头像等组件样式以匹配品牌调性。
- 在列表、通知、详情等场景提供加载/错误态（Skeleton、`el-alert`、`el-empty`）。

### 安全与韧性考量

- Markdown 内容通过 DOMPurify 清洗，降低富文本 XSS 风险。
- Axios 响应拦截对 401 进行令牌清理，防止未授权请求反复发送。
- Auth store 在拉取用户资料失败时尝试刷新令牌，刷新失败则强制登出以清除过期凭证。
- 评论操作在未登录情况下会明确提示并唤起登录框，避免越权。

### 观察与改进空间

- `LoginModal` 引用的 `authStore.clearError` 尚未实现，可补充以便统一清理错误提示。
- 作者过滤下为补齐互动数据会额外发送详情请求，可考虑后端增强或提供批量统计接口。
- 缺乏统一的错误提示封装，可抽象 `ElMessage` 调用以提升一致性。
- 尚未配置自动化测试与 lint，可引入 vitest/eslint 以提升可维护性。
- 通知轮询完全由前端负责，如后端支持可尝试 SSE/WebSocket 以提升扩展性。

---

## blog-user-service 技术报告

### 服务概览

- **定位**：博客平台的用户域微服务，负责用户注册、认证凭证存取、基础资料维护，以及文章互动通知的产生与提供。
- **技术栈**：Spring Boot 3.2.5、Spring Cloud 2023 (Eureka、OpenFeign)、MyBatis-Plus、Spring Security (仅提供 `PasswordEncoder`)、Spring AMQP、JJWT、PostgreSQL、RabbitMQ。默认 Java 17。
- **工程结构**：核心入口 `BlogUserServiceApplication`，通过 `@MapperScan` 注册 MyBatis-Plus Mapper 并启用服务注册发现。配置类覆盖安全策略、RabbitMQ 队列绑定以及静态资源映射。

### 配置与基础设施

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

### 数据模型与持久化

- **表划分**：
  - `t_user`（`User` 实体）：存储公共档案（邮箱、头像、简介、状态、时间戳）。
  - `t_user_auth`（`UserAuth` 实体）：按 `identityType` 存储认证信息（用户名/邮箱 -> BCrypt 哈希），支持多种登录方式。
  - `t_user_notification`（`Notification` 实体）：记录通知（收件人、触发者、关联文章、内容、已读标记、时间）。
- **访问层**：`UserMapper`、`UserAuthMapper`、`NotificationMapper` 均继承 MyBatis-Plus `BaseMapper`，使用 `QueryWrapper`/`UpdateWrapper` 构建查询与更新。

### 核心业务逻辑

#### 用户服务 (`UserServiceImpl`)

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

#### 通知服务 (`NotificationServiceImpl`)

- **通知创建 (`createNotification`)**：
  1. 将 `CreateNotificationRequest` 映射为 `Notification`，若未提供自定义内容，则用 `resolveContent` 根据类型生成默认文本（含触发者昵称、文章标题）。
  2. 插入记录后调用 `enrich`：批量加载触发者 `User` 与用户名 `UserAuth`，补充头像与昵称，返回 `NotificationDTO`。
- **查询 (`getNotificationsForUser`)**：
  - 支持 `unreadOnly` 过滤和 `limit`（缺省 20，上限 100）；按创建时间倒序查询并调用 `enrich`。
- **标记已读 (`markAsRead`/`markAllAsRead`)**：使用 `UpdateWrapper` 修改 `is_read` 字段；若单条更新返回 0，抛 `IllegalArgumentException` 表示不存在或无权限。

### 控制器与外部接口

- `UserController`
  - `POST /api/users/register`：公开注册，JSR-303 校验，冲突由 `IllegalStateException` 驱动统一错误响应。
  - `GET /api/users/{id}`：返回公共资料，供前端及 `blog-article-service` 获取作者信息。
  - `GET /api/users/internal/auth-details`：内部凭证接口，返回 `UserAuthDetailsDTO`；需部署时通过网络隔离或网关保护。
  - `POST /api/users/me/avatar`、`PUT /api/users/me/username`：需 `Authorization` 头，利用 `JwtUtil` 从 token 读取 `userId`。
- `NotificationController`
  - `POST /api/notifications/internal`：供其他服务（或 RabbitMQ 消费器备用）直接写通知。
  - `GET /api/notifications/me`、`POST /api/notifications/{id}/read`、`POST /api/notifications/read-all`：面向登录用户，需 JWT 解码用户身份。

### 消息集成与跨服务协同

- `ArticleNotificationListener`
  - `@RabbitListener` 监听 `ARTICLE_NOTIFICATION_QUEUE`，收到 `CreateNotificationRequest` 后调用 `NotificationService.createNotification`，失败记录错误不重试。
- 与 `blog-article-service` 协作：文章点赞/评论时在对方服务中使用 `NotificationClient` 调用 `createNotification` 或发 MQ 消息，本服务负责持久化与查询。
- 与 `blog-auth-service` 协作：登录流程通过 Feign 调用 `getUserAuthDetails`；本服务提供 BCrypt hash 与账号状态。

### JWT 工具

- `JwtUtil` 使用配置中的 Base64 秘钥解析 token，提取 `userId` 供控制器认证；解析失败返回 `null` 并触发 `401`。

### 异常处理与校验

- `GlobalExceptionHandler` 对 `MethodArgumentNotValidException`、`IllegalStateException`、`IllegalArgumentException`、`ResponseStatusException`、其他异常分别生成统一 JSON（含 message、status、可选 error）。
- 控制层往往捕获业务异常后转为 `ResponseStatusException`（如头像上传失败、通知未找到）。

### 测试现状

- `NotificationServiceImplTest` 使用 Mockito 验证通知创建/查询的丰富化逻辑、已读操作异常分支。尚未覆盖用户注册、头像上传、RabbitMQ 监听等功能，可按优先级扩展。

### 代码关系总结

- 控制器 → 服务 → Mapper：形成典型三层架构，服务层封装业务事务，Mapper 层使用 MyBatis-Plus 操作 Postgres。
- 服务间调用：`blog-auth-service` 通过 Feign 拉取凭证；`blog-article-service` 通过 RabbitMQ/REST 写入通知；JWT 秘钥在多个服务中保持一致。
- 文件系统：头像上传目录由 `UserServiceImpl` 与 `WebConfig` 协同管理，对外通过 `/uploads/**` 暴露。

### 可扩展与注意事项

- 注册时邮箱凭证与用户名共用同一密码 hash，未来若支持重置需同步两条 `UserAuth` 记录。
- 内部接口（auth-details、notifications/internal）需在部署侧加防护，避免被公网访问。
- 当前使用 JJWT 0.9.1（旧版 API），若与其他服务统一升级 0.12.x 需调整代码。
- 头像文件删除失败被忽略，如需审计可加日志或异步清理。
- 通知内容生成支持中文模板；若需国际化，可引入 i18n 服务或模板引擎。
