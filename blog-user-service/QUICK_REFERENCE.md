# Blog User Service - 快速参考指南

## 服务信息

- **端口**: 8083
- **数据库**: PostgreSQL (`blog_user_db`)
- **主要功能**: 用户注册、资料维护、凭证查询、文章互动通知

## 快速命令

### 启动服务

```powershell
.\start.ps1
```

### 测试 API

```powershell
.\test-api.ps1
```

### 检查服务健康

```powershell
curl http://localhost:8083/actuator/health
```

## 常用 API 示例

### 1. 用户注册

```powershell
curl -X POST http://localhost:8083/api/users/register `
  -H "Content-Type: application/json" `
  -d '{
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "password": "password123"
  }'
```

### 2. 获取用户信息

```powershell
curl http://localhost:8083/api/users/1
```

### 3. 查询认证详情 (内部接口)

```powershell
curl "http://localhost:8083/api/users/internal/auth-details?identityType=username&identifier=zhangsan"
```

### 4. 创建通知 (内部接口)

```powershell
curl -X POST http://localhost:8083/api/notifications/internal `
  -H "Content-Type: application/json" `
  -d '{
    "userId": 1,
    "actorId": 2,
    "articleId": 101,
    "articleTitle": "Java 入门",
    "type": "ARTICLE_LIKE"
  }'
```

### 5. 获取当前用户通知

```powershell
curl "http://localhost:8083/api/notifications/me?unreadOnly=true&limit=10" `
  -H "Authorization: Bearer <JWT>"
```

### 6. 标记通知为已读

```powershell
curl -X POST http://localhost:8083/api/notifications/123/read `
  -H "Authorization: Bearer <JWT>"
```

### 7. 更新用户名

```powershell
curl -X PUT http://localhost:8083/api/users/me/username `
  -H "Authorization: Bearer <JWT>" `
  -H "Content-Type: application/json" `
  -d '{
    "username": "new-name"
  }'
```

## 数据库表速览

### t_user

```sql
CREATE TABLE t_user (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255),
  avatar_url VARCHAR(255),
  bio TEXT,
  status INTEGER DEFAULT 0,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

### t_user_auth

```sql
CREATE TABLE t_user_auth (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES t_user(id),
  identity_type VARCHAR(20) NOT NULL,
  identifier VARCHAR(100) NOT NULL,
  credential VARCHAR(255) NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  UNIQUE (identity_type, identifier)
);
```

### t_user_notification

```sql
CREATE TABLE t_user_notification (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES t_user(id) ON DELETE CASCADE,
  actor_id BIGINT NOT NULL,
  article_id BIGINT,
  article_title VARCHAR(255),
  type VARCHAR(50) NOT NULL,
  content TEXT,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_user ON t_user_notification(user_id, is_read);
CREATE INDEX idx_notification_article ON t_user_notification(article_id);
```

## 环境变量

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_USER="postgres"
$env:DB_PASSWORD="password"
```

## Docker 命令

### 启动 PostgreSQL

```powershell
docker run -d `
  --name blog-postgres-user `
  -e POSTGRES_DB=blog_user_db `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=password `
  -p 5432:5432 `
  postgres:14-alpine
```

### 停止并删除容器

```powershell
docker stop blog-postgres-user
docker rm blog-postgres-user
```

## 常见问题

- **服务启动失败，提示数据库连接错误** → 检查 PostgreSQL 是否运行，并确认连接参数正确。
- **注册时提示 "Username is already taken"** → 用户名必须唯一，请更换新的用户名。
- **注册时提示 "Identifier is already registered"** → 该用户名或邮箱已存在，请确认是否已有账户。
- **通知接口返回 401** → 确保请求头携带 `Authorization: Bearer <JWT>` 并确认 token 未过期。

## 安全提示

- `/api/users/internal/**` 与 `/api/notifications/internal` 仅供内部服务调用，生产环境需通过网关或网络隔离保护。
- 所有密码使用 BCrypt 哈希存储，切勿在日志中输出明文密码。
- 建议在生产环境提升密码最小长度限制并强制使用 HTTPS。
