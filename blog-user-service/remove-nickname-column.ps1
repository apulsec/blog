# 迁移脚本：删除 t_user 表中的 nickname 字段，并确保 email 列存在

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  博客用户服务 - 表结构升级工具" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "正在升级 t_user 表结构 (移除 nickname 字段)..." -ForegroundColor Yellow

$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    Write-Host "✗ Docker 命令不可用!" -ForegroundColor Red
    Write-Host "  请确保 Docker Desktop 已安装并运行" -ForegroundColor Yellow
    exit 1
}

$pgContainers = docker ps --format "{{.Names}}" 2>&1 | Select-String -Pattern "postgres"
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($pgContainers)) {
    Write-Host "✗ 未找到运行中的 PostgreSQL 容器" -ForegroundColor Red
    exit 1
}

$containerName = $pgContainers.ToString().Trim()
Write-Host "✓ 找到容器: $containerName" -ForegroundColor Green

$sql = @'
ALTER TABLE t_user
    DROP COLUMN IF EXISTS nickname;

ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS email VARCHAR(100);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 't_user' AND column_name = 'username'
    ) THEN
        EXECUTE 'ALTER TABLE t_user ALTER COLUMN username DROP NOT NULL';
    END IF;
END;
$$;
'@

$tempSqlFile = [System.IO.Path]::GetTempFileName()
$sql | Out-File -FilePath $tempSqlFile -Encoding UTF8

try {
    Get-Content -Raw $tempSqlFile | docker exec -i $containerName psql -U postgres -d blog_user_db

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 表结构升级完成" -ForegroundColor Green
    } else {
        Write-Host "✗ 表结构升级失败 (退出码: $LASTEXITCODE)" -ForegroundColor Red
    }
} finally {
    Remove-Item -Path $tempSqlFile -ErrorAction SilentlyContinue
}

Write-Host "升级完成后, 建议重新启动用户服务以使用最新结构。" -ForegroundColor Cyan
