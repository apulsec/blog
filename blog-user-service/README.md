# Blog User Service

用户管理与通知服务，为博客平台提供用户账户管理、认证凭证存储、用户资料维护以及文章互动通知等功能。

## 服务概述

`blog-user-service` 是博客平台的用户领域核心服务，负责：

- **用户注册**: 处理新用户账户创建，使用 BCrypt 安全加密密码
- **用户资料管理**: 存储和检索用户的公开资料信息（用户名、邮箱、头像、简介等）
- **认证凭证提供**: 为 `blog-auth-service` 提供内部 API，用于验证用户登录凭证
- **作者信息服务**: 为 `blog-article-service` 提供文章作者的详细信息
- **通知派发**: 在文章被点赞或评论时为作者生成通知，并提供查询与已读操作接口

## 技术栈

- **框架**: Spring Boot 3.2.5
- **Java 版本**: Java 17
- **数据库**: PostgreSQL (用户元数据和认证凭证)
- **数据访问**: MyBatis-Plus 3.5.7
- **安全**: Spring Security (BCrypt 密码加密)
- **服务发现**: Spring Cloud Netflix Eureka Client
- **构建工具**: Maven

## 数据库设计

### PostgreSQL 表结构

#### 1. `t_user` (用户表)

存储用户基础资料信息。

| 字段       | 类型        | 说明                                          |
| ---------- | ----------- | --------------------------------------------- |
| id         | BIGINT (PK) | 用户唯一标识符                                |
| email      | VARCHAR     | 用户邮箱（可为空，用于联系或找回密码）        |
| avatar_url | VARCHAR     | 头像 URL                                      |
| bio        | TEXT        | 用户简介                                      |
| status     | INTEGER     | 账户状态 (0:正常, 1:禁用, 2:待激活, 3:已注销) |
| created_at | TIMESTAMP   | 创建时间                                      |
| updated_at | TIMESTAMP   | 更新时间                                      |

#### 2. `t_user_auth` (用户认证表)

存储用户认证凭证，支持多种认证方式。

| 字段          | 类型        | 说明                                 |
| ------------- | ----------- | ------------------------------------ |
| id            | BIGINT (PK) | 认证记录唯一标识符                   |
| user_id       | BIGINT (FK) | 关联用户 ID                          |
| identity_type | VARCHAR     | 认证类型 (username, email, phone 等) |
| identifier    | VARCHAR     | 唯一标识符 (用户名、邮箱地址等)      |
| credential    | VARCHAR     | 凭证 (BCrypt 加密的密码或第三方令牌) |
| created_at    | TIMESTAMP   | 创建时间                             |
| updated_at    | TIMESTAMP   | 更新时间                             |

**唯一约束**: (identity_type, identifier) 组合唯一

#### 3. `t_user_notification` (用户通知表)

保存文章互动产生的通知记录。

| 字段          | 类型        | 说明                                        |
| ------------- | ----------- | ------------------------------------------- |
| id            | BIGINT (PK) | 通知唯一标识符                              |
| user_id       | BIGINT (FK) | 通知接收者（文章作者）                      |
| actor_id      | BIGINT      | 触发通知的用户（点赞或评论者）              |
| article_id    | BIGINT      | 关联文章 ID                                 |
| article_title | VARCHAR     | 文章标题快照，用于前端展示                  |
| type          | VARCHAR     | 通知类型 (ARTICLE_LIKE、ARTICLE_COMMENT 等) |
| content       | TEXT        | 通知内容，未提供时由服务按类型生成默认文案  |
| is_read       | BOOLEAN     | 是否已读，默认 `false`                      |
| created_at    | TIMESTAMP   | 创建时间                                    |

常用索引： `(user_id, is_read)`、`article_id`

## API 端点

### 公开端点

#### 1. 用户注册

```http
POST /api/users/register
Content-Type: application/json

{
  "username": "zhangsan",
  "email": "zhangsan@example.com",
  "password": "password123"
}
```

**响应**:

```json
{
  "id": 1,
  "username": "zhangsan",
  "avatarUrl": null,
  "bio": null,
  "email": "zhangsan@example.com"
}
```

#### 2. 获取用户信息

```http
GET /api/users/{userId}
```

**响应**:

```json
{
  "id": 1,
  "avatarUrl": "https://example.com/avatar.jpg",
  "bio": "热爱技术的开发者",
  "username": "zhangsan",
  "email": "zhangsan@example.com"
}
```

### 内部端点 (仅供其他微服务调用)

#### 3. 获取认证详情

```http
GET /api/users/internal/auth-details?identityType=email&identifier=zhangsan@example.com
```

**响应**:

```json
{
  "userId": 1,
  "identifier": "zhangsan",
  "credential": "$2a$10$...", // BCrypt 哈希值
  "username": "zhangsan",
  "status": 0
}
```

**⚠️ 安全警告**: 此端点返回敏感信息（密码哈希），应仅在内部网络中访问。

#### 4. 创建通知（内部）

```http
POST /api/notifications/internal
Content-Type: application/json

{
  "userId": 42,
  "actorId": 7,
  "articleId": 128,
  "articleTitle": "Spring Boot 实战指南",
  "type": "ARTICLE_LIKE"
}
```

**响应**:

```json
{
  "id": 101,
  "userId": 42,
  "actorId": 7,
  "articleId": 128,
  "articleTitle": "Spring Boot 实战指南",
  "type": "ARTICLE_LIKE",
  "content": "someone点赞了《Spring Boot 实战指南》",
  "read": false,
  "createdAt": "2025-11-01T10:18:00"
}
```

### 受保护端点（需携带 JWT Bearer Token）

这些接口由前端或其他受信服务调用，控制器通过 `Authorization: Bearer <token>` 解析用户 ID。

#### 1. 更新头像

```http
POST /api/users/me/avatar
Content-Type: multipart/form-data
Authorization: Bearer <JWT>

file=@avatar.png
```

#### 2. 更新用户名

```http
PUT /api/users/me/username
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "username": "new-name"
}
```

#### 3. 查询个人通知列表

```http
GET /api/notifications/me?unreadOnly=true&limit=10
Authorization: Bearer <JWT>
```

#### 4. 标记通知为已读

```http
POST /api/notifications/{notificationId}/read
Authorization: Bearer <JWT>
```

#### 5. 一键已读全部通知

```http
POST /api/notifications/read-all
Authorization: Bearer <JWT>
```

## 配置说明

### 环境变量

服务支持通过环境变量配置，便于容器化部署：

| 环境变量         | 默认值                        | 说明                 |
| ---------------- | ----------------------------- | -------------------- |
| `DB_HOST`        | localhost                     | PostgreSQL 主机地址  |
| `DB_PORT`        | 5432                          | PostgreSQL 端口      |
| `DB_USER`        | postgres                      | 数据库用户名         |
| `DB_PASSWORD`    | password                      | 数据库密码           |
| `EUREKA_ENABLED` | true                          | 是否启用 Eureka 注册 |
| `EUREKA_URI`     | http://localhost:8761/eureka/ | Eureka 服务器地址（使用 Docker 时请设置为 `http://localhost:18761/eureka/`） |

### application.yml 配置要点

- **服务端口**: 8083
- **数据库**: 自动创建/更新表结构 (`ddl-auto: update`)
- **Eureka**: 默认启用，可通过环境变量控制注册行为

## 快速启动

### 前置条件

1. **Java 17** 或更高版本
2. **Maven 3.6+**
3. **PostgreSQL 14+** (可使用 Docker)

### 启动 PostgreSQL (使用 Docker)

```bash
docker run -d \
  --name blog-postgres \
  -e POSTGRES_DB=blog_user_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:14-alpine
```

### 编译和运行

```bash
# 编译项目
mvn clean package

# 运行服务
java -jar target/blog-user-service-0.0.1-SNAPSHOT.jar
```

或者使用 Maven 插件直接运行：

```bash
mvn spring-boot:run
```

### 验证服务运行

```bash
# 健康检查
curl http://localhost:8083/actuator/health

# 注册测试用户
curl -X POST http://localhost:8083/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

## 安全性考虑

### 密码安全

- 使用 **BCrypt** 算法进行密码哈希
- 密码哈希包含随机盐值，防止彩虹表攻击
- 哈希算法工作因子可调整，随硬件升级提高安全性

### 内部端点保护

`/api/users/internal/auth-details` 端点返回敏感信息，生产环境中应：

- 部署在隔离的内部网络（VPC、Service Mesh）
- 通过 API 网关限制访问
- 实施服务间认证（如 mTLS）

### 数据验证

- 所有输入使用 Jakarta Validation 进行验证
- 用户名长度限制：3-50 字符
- 密码最小长度：6 字符（建议在生产中提高到 8-12 字符）

## 与其他服务的集成

### blog-auth-service

认证服务通过调用 `/internal/auth-details` 端点验证用户登录凭证：

1. 接收用户登录请求（identifier + password）
2. 调用 user-service 获取存储的密码哈希
3. 使用 PasswordEncoder 验证密码
4. 验证账户状态是否允许登录

### blog-article-service

文章服务调用 `/api/users/{userId}` 端点获取作者信息：

- 在文章列表中展示作者用户名和头像
- 在文章详情页显示作者完整资料

## 开发和扩展

### 添加新的认证方式

系统设计支持多种认证方式（OAuth、手机号等），只需：

1. 在注册时指定不同的 `identityType`
2. UserAuth 表自动支持一个用户多个认证方式

### 自定义查询

在 `UserMapper` 或 `UserAuthMapper` 中添加自定义方法：

```java
@Select("SELECT * FROM t_user WHERE username LIKE #{pattern}")
List<User> searchByUsername(@Param("pattern") String pattern);
```

## 监控和运维

### 健康检查

```bash
curl http://localhost:8083/actuator/health
```

### 服务信息

```bash
curl http://localhost:8083/actuator/info
```

## 许可证

本项目为博客平台的一部分，遵循项目整体许可证。
