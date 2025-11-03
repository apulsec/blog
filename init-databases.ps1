# 博客平台 - 全量数据库初始化脚本
# 用途: 一次性初始化文章服务、用户服务所需的 PostgreSQL 数据库及 MongoDB 内容

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  博客平台 - 数据库初始化工具" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$rootPath = $PSScriptRoot

function Ensure-DockerAvailable {
    Write-Host "[1/6] 检查 Docker 环境..." -ForegroundColor Yellow
    $dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $dockerCmd) {
        Write-Host "✗ Docker 命令不可用!" -ForegroundColor Red
        Write-Host "  请确保 Docker Desktop 已安装并运行" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "  ✓ Docker 已就绪" -ForegroundColor Green
    Write-Host ""
}

function Resolve-PostgresContainer {
    Write-Host "[2/6] 查找 PostgreSQL 容器..." -ForegroundColor Yellow
    $candidates = docker ps --filter "publish=15432" --format "{{.Names}}" 2>&1

    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($candidates)) {
        # 兜底: 查找名称包含 postgres 的容器
        $candidates = docker ps --format "{{.Names}}" 2>&1 | Select-String -Pattern "postgres"
    }

    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($candidates)) {
        Write-Host "✗ 未找到运行中的 PostgreSQL 容器" -ForegroundColor Red
        Write-Host ""
        Write-Host "请启动 PostgreSQL Docker 容器,例如:" -ForegroundColor Yellow
        Write-Host "  docker run -d --name postgres-blog -p 15432:5432 -e POSTGRES_PASSWORD=password postgres:14" -ForegroundColor Gray
        Write-Host "或使用 docker-compose: docker-compose up -d postgres" -ForegroundColor Gray
        exit 1
    }

    $containerName = $candidates.ToString().Split([Environment]::NewLine)[0].Trim()
    Write-Host "  ✓ 找到容器: $containerName" -ForegroundColor Green
    Write-Host ""
    return $containerName
}

function Invoke-PostgresScriptFromFile {
    param(
        [string]$Container,
        [string]$ScriptPath,
        [string]$Description
    )

    if (-not (Test-Path $ScriptPath)) {
        Write-Host "✗ 未找到 SQL 文件: $ScriptPath" -ForegroundColor Red
        exit 1
    }

    $remoteFile = "/tmp/{0}-{1}" -f (Split-Path $ScriptPath -Leaf), ([Guid]::NewGuid().ToString("N"))

    try {
        Write-Host $Description -ForegroundColor Yellow
        docker cp $ScriptPath ("{0}:{1}" -f $Container, $remoteFile) | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "✗ 无法复制 SQL 脚本到容器 $Container" -ForegroundColor Red
            exit 1
        }

        docker exec $Container psql -v ON_ERROR_STOP=1 -U postgres -f $remoteFile 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "✗ 初始化失败 (退出码: $LASTEXITCODE)" -ForegroundColor Red
            exit 1
        }
        Write-Host "  ✓ 完成" -ForegroundColor Green
        Write-Host ""
    }
    finally {
        docker exec $Container rm -f $remoteFile | Out-Null
    }
}

function Reset-MongoContent {
    Write-Host "[5/6] 清空 MongoDB 文章内容..." -ForegroundColor Yellow

    $mongoContainers = docker ps --filter "publish=27017" --format "{{.Names}}" 2>&1
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($mongoContainers)) {
        Write-Host "✗ 未找到运行在 27017 端口的 MongoDB 容器" -ForegroundColor Red
        Write-Host ""
        Write-Host "请确保 MongoDB Docker 容器正在运行,例如:" -ForegroundColor Yellow
        Write-Host "  docker run -d -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=root -e MONGO_INITDB_ROOT_PASSWORD=password mongo:7" -ForegroundColor Gray
        exit 1
    }

    $mongoContainerName = $mongoContainers.Split([Environment]::NewLine)[0].Trim()
    Write-Host "  ✓ 找到容器: $mongoContainerName" -ForegroundColor Green

    $mongoUser = if ([string]::IsNullOrWhiteSpace($env:MONGO_INITDB_ROOT_USERNAME)) { $env:MONGO_USER } else { $env:MONGO_INITDB_ROOT_USERNAME }
    if ([string]::IsNullOrWhiteSpace($mongoUser)) { $mongoUser = "root" }

    $mongoPassword = if ([string]::IsNullOrWhiteSpace($env:MONGO_INITDB_ROOT_PASSWORD)) { $env:MONGO_PASSWORD } else { $env:MONGO_INITDB_ROOT_PASSWORD }
    if ([string]::IsNullOrWhiteSpace($mongoPassword)) { $mongoPassword = "password" }

    $mongoDatabase = if ([string]::IsNullOrWhiteSpace($env:MONGO_DB)) { "blog_content_db" } else { $env:MONGO_DB }

    $mongoResult = docker exec $mongoContainerName mongosh --quiet --username $mongoUser --password $mongoPassword --authenticationDatabase admin --eval "db.getSiblingDB('$mongoDatabase').dropDatabase();" 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ 清空 MongoDB 数据库失败" -ForegroundColor Red
        Write-Host $mongoResult -ForegroundColor Gray
        exit 1
    }

    Write-Host "  ✓ MongoDB 数据库 $mongoDatabase 已清空" -ForegroundColor Green
    Write-Host ""
    return @{ Name = $mongoDatabase; Container = $mongoContainerName }
}

Ensure-DockerAvailable
$postgresContainer = Resolve-PostgresContainer

$sqlDirectory = Join-Path $rootPath "database"
$sqlDirectory = Join-Path $sqlDirectory "sql"
$articleSqlPath = Join-Path $sqlDirectory "article-db-init.sql"
$userSqlPath = Join-Path $sqlDirectory "user-db-init.sql"

Invoke-PostgresScriptFromFile -Container $postgresContainer -ScriptPath $articleSqlPath -Description "[3/6] 初始化 PostgreSQL (blog_article_db)..."
Invoke-PostgresScriptFromFile -Container $postgresContainer -ScriptPath $userSqlPath -Description "[4/6] 初始化 PostgreSQL (blog_user_db)..."

$mongoInfo = Reset-MongoContent

Write-Host "[6/6] 验证数据库结构 (用户库)..." -ForegroundColor Yellow
$verifySql = @"
\c blog_user_db
\dt
"@
$tempVerifyFile = [System.IO.Path]::GetTempFileName()
$verifySql | Out-File -FilePath $tempVerifyFile -Encoding UTF8
try {
    Get-Content $tempVerifyFile | docker exec -i $postgresContainer psql -U postgres -f -
} finally {
    Remove-Item $tempVerifyFile -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  ✓ 所有数据库已初始化" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "数据库信息:" -ForegroundColor Yellow
Write-Host "  - PostgreSQL 容器: $postgresContainer" -ForegroundColor White
Write-Host "  - blog_article_db: 文章 / 点赞 / 评论表已重建并写入测试数据" -ForegroundColor White
Write-Host "  - blog_user_db: 用户 / 认证 / 通知表已重建并写入示例作者" -ForegroundColor White
Write-Host "  - MongoDB ($($mongoInfo.Name)): 已在容器 $($mongoInfo.Container) 内清空" -ForegroundColor White
Write-Host ""
Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "  .\\start-all-services.ps1" -ForegroundColor Gray
Write-Host "  或分别启动各服务验证运行状态" -ForegroundColor Gray
