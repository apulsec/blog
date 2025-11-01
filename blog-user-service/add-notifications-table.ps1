# 博客用户服务 - 通知表新增脚本
# 用途: 为 blog_user_db 添加用户通知表，用于存储文章互动提醒

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  博客用户服务 - 添加通知表" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1/3] 检查 Docker 环境..." -ForegroundColor Yellow
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "✗ 未检测到 Docker，请先安装并启动 Docker Desktop" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Docker 已就绪" -ForegroundColor Green
Write-Host ""

Write-Host "[2/3] 查找 PostgreSQL 容器..." -ForegroundColor Yellow
$containerName = docker ps --format "{{.Names}}" 2>&1 | Select-String -Pattern "postgres" | Select-Object -First 1 | ForEach-Object { $_.ToString().Trim() }
if (-not $containerName) {
    Write-Host "✗ 未找到正在运行的 PostgreSQL 容器" -ForegroundColor Red
    Write-Host "  请先启动数据库容器，例如: docker run -d --name postgres-blog -p 15432:5432 -e POSTGRES_PASSWORD=123456 postgres:14" -ForegroundColor Yellow
    exit 1
}
Write-Host "✓ 找到容器: $containerName" -ForegroundColor Green
Write-Host ""

Write-Host "[3/3] 创建通知表..." -ForegroundColor Yellow
$sql = @"
\c blog_user_db

CREATE TABLE IF NOT EXISTS t_user_notification (
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

CREATE INDEX IF NOT EXISTS idx_notification_user ON t_user_notification(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notification_article ON t_user_notification(article_id);
"@

$tempFile = [System.IO.Path]::GetTempFileName()
$sql | Out-File -FilePath $tempFile -Encoding UTF8

try {
    Get-Content $tempFile | docker exec -i $containerName psql -U postgres -f -
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ 执行 SQL 失败 (退出码: $LASTEXITCODE)" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ 通知表创建完成" -ForegroundColor Green
} finally {
    Remove-Item $tempFile -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "任务完成，可使用 '\\dt t_user_notification' 验证表结构" -ForegroundColor Cyan
