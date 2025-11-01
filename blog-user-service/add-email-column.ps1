# 添加email和username字段到t_user表

Write-Host "添加email和username字段到t_user表..." -ForegroundColor Cyan

$sql = @"
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS email VARCHAR(100);
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS username VARCHAR(50);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email ON t_user(email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_username ON t_user(username);
UPDATE t_user SET email = 'user' || user_id || '@example.com' WHERE email IS NULL;
UPDATE t_user SET username = nickname WHERE username IS NULL;
"@

# 写入临时SQL文件
$sql | Out-File -FilePath "add-email-column.sql" -Encoding UTF8

# 执行SQL - 使用postgres超级用户
Get-Content "add-email-column.sql" | docker exec -i blog-postgres psql -U postgres -d blog_user_db

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Email和Username字段添加成功" -ForegroundColor Green
} else {
    Write-Host "✗ 添加失败" -ForegroundColor Red
}

# 清理临时文件
Remove-Item "add-email-column.sql"
