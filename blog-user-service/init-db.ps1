# 博客用户服务 - PostgreSQL 数据库初始化脚本
# 用途: 初始化 blog_user_db 数据库和表结构

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  博客用户服务 - 数据库初始化工具" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Docker 是否可用
Write-Host "[1/4] 检查 Docker 环境..." -ForegroundColor Yellow
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    Write-Host "✗ Docker 命令不可用!" -ForegroundColor Red
    Write-Host "  请确保 Docker Desktop 已安装并运行" -ForegroundColor Yellow
    exit 1
}
Write-Host "✓ Docker 已就绪" -ForegroundColor Green
Write-Host ""

# 查找 PostgreSQL 容器
Write-Host "[2/4] 查找 PostgreSQL 容器..." -ForegroundColor Yellow
$pgContainers = docker ps --format "{{.Names}}" 2>&1 | Select-String -Pattern "postgres"

if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($pgContainers)) {
    Write-Host "✗ 未找到运行中的 PostgreSQL 容器" -ForegroundColor Red
    Write-Host ""
    Write-Host "请启动 PostgreSQL Docker 容器,例如:" -ForegroundColor Yellow
    Write-Host "  docker run -d --name postgres-blog -p 15432:5432 -e POSTGRES_PASSWORD=123456 postgres:14" -ForegroundColor Gray
    Write-Host ""
    Write-Host "或使用 Docker Compose:" -ForegroundColor Yellow
    Write-Host "  docker-compose up -d postgres" -ForegroundColor Gray
    exit 1
}

$containerName = $pgContainers.ToString().Trim()
Write-Host "✓ 找到容器: $containerName" -ForegroundColor Green
Write-Host ""

# 创建初始化 SQL
Write-Host "[3/4] 创建数据库和表结构..." -ForegroundColor Yellow
$initSQL = @"
-- 创建数据库
SELECT 'CREATE DATABASE blog_user_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'blog_user_db')\gexec

-- 连接到数据库
\c blog_user_db

-- 清空已有数据，确保初始化幂等
DROP TABLE IF EXISTS t_user_notification CASCADE;
DROP TABLE IF EXISTS t_user_auth CASCADE;
DROP TABLE IF EXISTS t_user CASCADE;

-- 创建用户信息表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    bio VARCHAR(500),
    status INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户认证表
CREATE TABLE IF NOT EXISTS t_user_auth (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES t_user(id) ON DELETE CASCADE,
    identity_type VARCHAR(20) NOT NULL,
    identifier VARCHAR(100) NOT NULL,
    credential VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (identity_type, identifier)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_user_auth_user_id ON t_user_auth(user_id);
CREATE INDEX IF NOT EXISTS idx_user_auth_identifier ON t_user_auth(identifier);

-- 预置示例作者，确保文章服务中的初始文章作者信息稳定
INSERT INTO t_user (id, email, avatar_url, bio, status)
SELECT 1, 'author1@example.com', 'https://picsum.photos/seed/author1/200/200', '云原生领域的技术布道者', 0
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 1);

INSERT INTO t_user (id, email, avatar_url, bio, status)
SELECT 2, 'author2@example.com', 'https://picsum.photos/seed/author2/200/200', '前端框架爱好者与开源贡献者', 0
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 2);

INSERT INTO t_user (id, email, avatar_url, bio, status)
SELECT 3, 'author3@example.com', 'https://picsum.photos/seed/author3/200/200', '后端架构与数据库调优实践者', 0
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 3);

-- 为示例作者配置用户名身份（credential 采用占位哈希，避免明文）
INSERT INTO t_user_auth (id, user_id, identity_type, identifier, credential)
SELECT 1, 1, 'username', 'cloud_architect', '$2a$10$Y2JxcnlwdGhhc2hIYXNocGFkLkhlbGxvUHJvamVjdA9O1EfnKvHRtS'
WHERE NOT EXISTS (
    SELECT 1 FROM t_user_auth WHERE identity_type = 'username' AND identifier = 'cloud_architect'
);

INSERT INTO t_user_auth (id, user_id, identity_type, identifier, credential)
SELECT 2, 2, 'username', 'frontend_guru', '$2a$10$Y2JxcnlwdGhhc2hIYXNocGFkLkhlbGxvUHJvamVjdA9O1EfnKvHRtS'
WHERE NOT EXISTS (
    SELECT 1 FROM t_user_auth WHERE identity_type = 'username' AND identifier = 'frontend_guru'
);

INSERT INTO t_user_auth (id, user_id, identity_type, identifier, credential)
SELECT 3, 3, 'username', 'data_maven', '$2a$10$Y2JxcnlwdGhhc2hIYXNocGFkLkhlbGxvUHJvamVjdA9O1EfnKvHRtS'
WHERE NOT EXISTS (
    SELECT 1 FROM t_user_auth WHERE identity_type = 'username' AND identifier = 'data_maven'
);

-- 调整序列值，确保后续自增不会与示例数据冲突
SELECT setval(pg_get_serial_sequence('t_user', 'id'), GREATEST(3, COALESCE((SELECT MAX(id) FROM t_user), 0)), true);
SELECT setval(pg_get_serial_sequence('t_user_auth', 'id'), GREATEST(3, COALESCE((SELECT MAX(id) FROM t_user_auth), 0)), true);

-- 创建用户通知表 (t_user_notification)
-- 一些服务（或前端）会调用 /api/notifications 相关接口，期望此表存在
CREATE TABLE IF NOT EXISTS t_user_notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    actor_id BIGINT,
    article_id BIGINT,
    article_title VARCHAR(255),
    type VARCHAR(50),
    content TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 验证表创建
SELECT 
    'Tables created successfully:' as message,
    count(*) as table_count 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('t_user', 't_user_auth');
"@

# 将 SQL 写入临时文件
$tempSqlFile = [System.IO.Path]::GetTempFileName()
$initSQL | Out-File -FilePath $tempSqlFile -Encoding UTF8

# 执行初始化
try {
    Get-Content $tempSqlFile | docker exec -i $containerName psql -U postgres -f - 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 数据库初始化成功" -ForegroundColor Green
    } else {
        Write-Host "✗ 数据库初始化失败 (退出码: $LASTEXITCODE)" -ForegroundColor Red
        exit 1
    }
} finally {
    Remove-Item -Path $tempSqlFile -ErrorAction SilentlyContinue
}

Write-Host ""

# 验证数据库
Write-Host "[4/4] 验证数据库结构..." -ForegroundColor Yellow
$verifySQL = @"
\c blog_user_db
\dt
"@

$tempVerifyFile = [System.IO.Path]::GetTempFileName()
$verifySQL | Out-File -FilePath $tempVerifyFile -Encoding UTF8

try {
    Get-Content $tempVerifyFile | docker exec -i $containerName psql -U postgres -f -
} finally {
    Remove-Item -Path $tempVerifyFile -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  数据库初始化完成!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "数据库信息:" -ForegroundColor Yellow
Write-Host "  数据库名: blog_user_db" -ForegroundColor White
Write-Host "  主机: localhost" -ForegroundColor White
Write-Host "  端口: 15432" -ForegroundColor White
Write-Host "  用户: postgres" -ForegroundColor White
Write-Host "  密码: 123456" -ForegroundColor White
Write-Host ""
Write-Host "包含的表:" -ForegroundColor Yellow
Write-Host "  - t_user (用户信息表)" -ForegroundColor White
Write-Host "  - t_user_auth (用户认证表)" -ForegroundColor White
Write-Host ""
Write-Host "现在可以启动用户服务:" -ForegroundColor Yellow
Write-Host "  cd blog-user-service" -ForegroundColor Gray
Write-Host "  .\start.ps1" -ForegroundColor Gray
Write-Host ""
