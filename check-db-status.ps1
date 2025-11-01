# 检查运行在 Docker 中的 PostgreSQL 和 MongoDB 状态的脚本
# check-db-status.ps1

Write-Host "=== 检查 Docker 中的数据库状态 ===" -ForegroundColor Cyan

# 检查 PostgreSQL 容器
Write-Host "\n[1/4] 检查 PostgreSQL 容器..." -ForegroundColor Yellow
$pgContainer = docker ps --filter "name=blog-postgres" --format "{{.Names}}"
if ($pgContainer) {
    Write-Host "✓ blog-postgres 容器正在运行" -ForegroundColor Green
    Write-Host "- 容器名: $pgContainer"
    # 检查 PostgreSQL 服务
    Write-Host "- 检查数据库连接..."
    $pgStatus = docker exec blog-postgres pg_isready -U postgres
    Write-Host $pgStatus
    # 显示数据库列表
    Write-Host "- 数据库列表:"
    docker exec blog-postgres psql -U postgres -c "\l"
    # 显示表结构和行数（blog_article_db）
    Write-Host "- 表结构与行数 (blog_article_db):"
    docker exec blog-postgres psql -U postgres -d blog_article_db -c "\dt"
    $tables = docker exec blog-postgres psql -U postgres -d blog_article_db -t -c "SELECT tablename FROM pg_tables WHERE schemaname='public';"
    foreach ($t in $tables -split "\n") {
        if ($t.Trim()) {
            Write-Host "  表: $t"
            docker exec blog-postgres psql -U postgres -d blog_article_db -c "SELECT COUNT(*) FROM \"$t\";"
            docker exec blog-postgres psql -U postgres -d blog_article_db -c "\d+ \"$t\""
        }
    }
    # 显示连接、锁、活动会话
    Write-Host "- 活动连接:"
    docker exec blog-postgres psql -U postgres -d blog_article_db -c "SELECT * FROM pg_stat_activity;"
    Write-Host "- 锁信息:"
    docker exec blog-postgres psql -U postgres -d blog_article_db -c "SELECT * FROM pg_locks LIMIT 10;"
    # 显示索引
    Write-Host "- 索引信息:"
    docker exec blog-postgres psql -U postgres -d blog_article_db -c "\di"
    # 显示部分数据
    Write-Host "- t_article 表部分数据:"
    docker exec blog-postgres psql -U postgres -d blog_article_db -c "SELECT * FROM t_article LIMIT 5;"
} else {
    Write-Host "✗ blog-postgres 容器未运行" -ForegroundColor Red
}

# 检查 MongoDB 容器
Write-Host "\n[2/4] 检查 MongoDB 容器..." -ForegroundColor Yellow
$mongoContainer = docker ps --filter "name=blog-mongodb" --format "{{.Names}}"
if ($mongoContainer) {
    Write-Host "✓ blog-mongodb 容器正在运行" -ForegroundColor Green
    Write-Host "- 容器名: $mongoContainer"
    # 检查 MongoDB 服务
    Write-Host "- 检查数据库连接..."
    $mongoStatus = docker exec blog-mongodb mongosh --quiet --eval "db.adminCommand('ping')" -u root -p password
    Write-Host $mongoStatus
    # 显示数据库列表
    Write-Host "- 数据库列表:"
    docker exec blog-mongodb mongosh --quiet --eval "db.adminCommand('listDatabases')" -u root -p password
    # 显示集合和文档数（blog_content_db）
    Write-Host "- 集合与文档数 (blog_content_db):"
    $collections = docker exec blog-mongodb mongosh blog_content_db --quiet --eval "db.getCollectionNames().join(',')" -u root -p password
    foreach ($c in $collections -split ",") {
        if ($c.Trim()) {
            Write-Host "  集合: $c"
            docker exec blog-mongodb mongosh blog_content_db --quiet --eval "db.getCollection('$c').countDocuments()" -u root -p password
            docker exec blog-mongodb mongosh blog_content_db --quiet --eval "db.getCollection('$c').stats()" -u root -p password
            Write-Host "  部分数据:"
            docker exec blog-mongodb mongosh blog_content_db --quiet --eval "db.getCollection('$c').find().limit(3).toArray()" -u root -p password
        }
    }
    # 显示连接数、状态
    Write-Host "- 连接数与状态:"
    docker exec blog-mongodb mongosh admin --quiet --eval "db.serverStatus()" -u root -p password
} else {
    Write-Host "✗ blog-mongodb 容器未运行" -ForegroundColor Red
}

Write-Host "\n[3/4] 检查 Docker 容器状态..." -ForegroundColor Yellow
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

Write-Host "\n[4/4] 检查主机端口占用..." -ForegroundColor Yellow
netstat -ano | findstr ":5432\|:27017"

Write-Host "\n=== 数据库状态检查完成 ===" -ForegroundColor Cyan
