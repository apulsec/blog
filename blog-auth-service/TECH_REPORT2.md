# blog-auth-service 技术报告

## 组件概览

- **运行时栈**：Spring Boot 3.2.5 + Spring Security 6，配合 Spring Cloud 2023 (OpenFeign、Eureka) 与 Spring Data Redis；Java 17 目标。
- **系统职责**：集中处理登录、注销、令牌刷新、令牌校验，产出 JWT 并维护 Redis 黑名单；所有业务入口集中在 `AuthController` → `AuthService` → `AuthServiceImpl` 调度。
- **构建依赖**：`pom.xml` 引入 JJWT 0.12.5（拆分 api/impl/jackson）、Spring Boot Web/Validation/Actuator、Spring Security、Redis Starter、OpenFeign、Netflix Eureka；测试层仅保留 `spring-boot-starter-test`（暂无落地用例）。

## 配置层设计

- `SecurityConfig`
  - 公开 `/api/auth/**` 与 `/actuator/**`，其余路径默认拒绝（本服务内部 API）；关闭 CSRF，启用 `SessionCreationPolicy.STATELESS`，明确 JWT 模式。
  - 配置全局 `PasswordEncoder = BCryptPasswordEncoder` 及 `AuthenticationManager`（注入 Spring Security 默认实现）。
  - CORS 策略允许本地前端域名（`5173`/`3000`），同源信任可按环境调整。
- `application.yml`
  - 通过变量注入 Redis、Eureka、Feign 超时、JWT 秘钥与过期时间（1h access + 7d refresh），默认 profile `local`。
  - `spring.cloud.openfeign.circuitbreaker.enabled=true` 保证 Feign 整合 Resilience4j。
- `RedisHealthIndicator` 实现 Actuator `HealthIndicator`，运行时 ping Redis，若失败在 `/actuator/health` 展示错误并提示 JWT 黑名单功能受阻。

## 安全/认证管线

- `UserDetailsServiceImpl`
  - 使用 Feign `UserServiceClient` 查询 `blog-user-service` 内部鉴权信息 `/api/users/internal/auth-details`。
  - 根据用户名包含 `@` 判定 `identityType`（email vs username）。
  - 若状态非 0（启用），抛出 `UsernameNotFoundException` 终止登录。
  - 自定义 `CustomUserDetails` 扩展 `User`，附带 `userId` 供后续 JWT claim 使用。
- `AuthenticationManager` 结合 `CustomUserDetails` 与密码编码器完成登录校验，失败统一由 `GlobalExceptionHandler` 捕获转换为中文提示。

## JWT 管理 (`JwtTokenProvider`)

- 使用 Base64 编码秘钥初始化 `SecretKey`（`@PostConstruct`）。
- **Access Token**：payload 包含 `sub=username`、`jti` 随机 UUID、`userId`、签发/过期时间；`jti` 为黑名单索引。
- **Refresh Token**：payload 仅含 `sub` 与时效，过期时间更长。
- 公开工具方法：`getUsernameFromToken`、`getJtiFromToken`、`getExpirationDateFromToken`、`validateToken`；验证时捕获并记录具体异常类型。

## 认证业务流程 (`AuthServiceImpl`)

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

## Redis 黑名单 (`JwtBlacklistService`)

- 键前缀 `jwt:blacklist:`，value 固定 `blacklisted`。
- 使用 `StringRedisTemplate` 的 `set` + TTL 防止无界膨胀；校验通过 `hasKey`。
- 与 `RedisHealthIndicator` 联动，Redis 不可用时功能降级：黑名单写/读失败但流程仍能进行（未显式捕获，依赖 Redis 可用性）。

## 对外接口 (`AuthController`)

- `POST /api/auth/login`：入参 `LoginRequest`（JSR 303 校验），回返 `AuthResponse`；异常走全局处理。
- `POST /api/auth/logout`：需 `Authorization` 头。
- `POST /api/auth/refresh`：刷新请求体 `TokenRefreshRequest`。
- `GET /api/auth/validate`：网关内部调用，返回用户名字符串或 401。
- 控制器仅承担参数解析与响应包装，所有业务分支封装在 `AuthService`。

## 异常与降级策略

- `GlobalExceptionHandler` 提供统一 JSON：校验错误聚合字段消息，登录失败/用户不存在/非法状态等映射成明确中文提示，其余异常返回 500，并携带 `error` 字段方便排查。
- Feign Fallback (`UserServiceClientFallback`) 记录错误并返回 `null`；`UserDetailsServiceImpl` 根据空值抛 `UsernameNotFoundException`，确保认证流程一致终止。

## 健康监控与运维

- Actuator 暴露 `health`、`info`，Redis 检查合并展示。
- `SecurityConfig` 允许匿名访问 `/actuator/**`，便于容器探针；若需限制可后续加 IP 白名单或 key。

## 代码协作关系

- `AuthController` -> `AuthServiceImpl` -> `AuthenticationManager` / `JwtTokenProvider` / `JwtBlacklistService` / `UserServiceClient`。
- `UserDetailsServiceImpl` 与 `UserServiceClient` 构成 Spring Security 内部认证桥梁。
- `JwtBlacklistService` 与 Redis 紧耦合，配合 `validateToken` 完成注销后的即时失效。
- `RedisHealthIndicator` 与 Actuator 集成，辅助定位 Redis 故障对黑名单能力的影响。

## 未覆盖与可扩展点

- 当前缺少集成/单元测试，可针对 `AuthServiceImpl`、`JwtBlacklistService` 编写基于 Mock 的校验用例。
- 登录接口未实现多因子/验证码；若需可在 `AuthServiceImpl.login` 前插入额外校验。
- 刷新令牌流程每次调用都会重新下发 refresh token，需保证客户端替换；若需单次刷新，可在 Redis 存储 refresh token 状态。
- 黑名单机制依赖 Redis，可通过熔断或 fallback 增强不可用时的降级策略（例如拒绝注销或标记只读模式）。
