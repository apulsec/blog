# 状态检查脚本 - Blog Article Service
# 用途: 快速检查服务和依赖的运行状态

Write-Host "=== Blog Article Service 状态检查 ===" -ForegroundColor Cyan
Write-Host ""

# 检查服务状态
Write-Host "[1/5] 服务状态" -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health" -TimeoutSec 2 -ErrorAction Stop
    if ($health.status -eq "UP") {
        Write-Host "  ✓ 服务运行中 (端口 8082)" -ForegroundColor Green
        
        # 获取更多信息
        try {
            $info = Invoke-RestMethod -Uri "http://localhost:8082/actuator" -ErrorAction SilentlyContinue
            if ($info) {
                Write-Host "  可用端点:" -ForegroundColor Gray
                $info._links.PSObject.Properties | ForEach-Object {
                    if ($_.Name -ne "self") {
                        Write-Host "    • $($_.Name)" -ForegroundColor White
                    }
                }
            }
        } catch {
            # 忽略错误
        }
    } elseif ($health.status -eq "DOWN") {
        Write-Host "  ⚠ 服务已启动但状态异常: $($health.status)" -ForegroundColor Yellow
        Write-Host "  说明: 应用运行中,但健康检查失败(可能是数据库连接问题)" -ForegroundColor Gray
        Write-Host "  排查: .\check-error.ps1 查看详细错误" -ForegroundColor Gray
        
        # 显示详细健康信息
        if ($health.components) {
            Write-Host "  组件状态:" -ForegroundColor Gray
            $health.components.PSObject.Properties | ForEach-Object {
                $status = $_.Value.status
                $color = if($status -eq "UP"){"Green"}else{"Red"}
                Write-Host "    • $($_.Name): $status" -ForegroundColor $color
            }
        }
    } else {
        Write-Host "  ⚠ 服务状态: $($health.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ 服务未运行" -ForegroundColor Red
    Write-Host "  启动命令: .\start.ps1" -ForegroundColor Gray
}

# 测试 API 功能 (验证数据库真实连接状态)
Write-Host ""
Write-Host "[2/5] API 功能测试" -ForegroundColor Yellow
try {
    $apiTest = Invoke-RestMethod -Uri "http://localhost:8082/api/articles?page=0&size=1" -TimeoutSec 3 -ErrorAction Stop
    Write-Host "  ✓ API 正常工作" -ForegroundColor Green
    Write-Host "  数据库文章数: $($apiTest.total)" -ForegroundColor Gray
    if ($apiTest.total -gt 0) {
        Write-Host "  最新文章: $($apiTest.records[0].title)" -ForegroundColor Gray
    }
} catch {
    Write-Host "  ✗ API 调用失败" -ForegroundColor Red
    if ($_.Exception.Message -match "500") {
        Write-Host "  说明: 服务运行但数据库可能有问题" -ForegroundColor Gray
    } else {
        Write-Host "  说明: $($_.Exception.Message)" -ForegroundColor Gray
    }
}

# 检查 PostgreSQL
Write-Host ""
Write-Host "[3/5] PostgreSQL 数据库" -ForegroundColor Yellow

# 首先检查端口是否监听
$pgPort = Get-NetTCPConnection -LocalPort 15432 -State Listen -ErrorAction SilentlyContinue
if ($pgPort) {
    Write-Host "  ✓ PostgreSQL 端口 15432 已监听" -ForegroundColor Green
    Write-Host "  进程: PID $($pgPort.OwningProcess)" -ForegroundColor Gray
    
    # 如果 psql 可用，尝试查询数据
    $psqlAvailable = Get-Command psql -ErrorAction SilentlyContinue
    if ($psqlAvailable) {
        try {
            $env:PGPASSWORD = "password"
            $articleCount = psql -h localhost -p 15432 -U postgres -d blog_article_db -c "SELECT COUNT(*) FROM t_article;" -t 2>&1
            Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
            
            if ($LASTEXITCODE -eq 0) {
                $count = $articleCount.Trim()
                Write-Host "  数据库: blog_article_db" -ForegroundColor Gray
                Write-Host "  文章数: $count" -ForegroundColor Gray
            }
        } catch {
            # 忽略 psql 错误
        }
    } else {
        Write-Host "  说明: 应用将通过 JDBC 连接 (psql 工具不是必需的)" -ForegroundColor Gray
    }
} else {
    Write-Host "  ✗ PostgreSQL 端口 15432 未监听" -ForegroundColor Red
    Write-Host "  请确保 PostgreSQL Docker 容器已启动" -ForegroundColor Gray
}

# 检查 MongoDB
Write-Host ""
Write-Host "[4/5] MongoDB 数据库" -ForegroundColor Yellow

# 检查端口是否监听
$mongoPort = Get-NetTCPConnection -LocalPort 27017 -State Listen -ErrorAction SilentlyContinue
if ($mongoPort) {
    Write-Host "  ✓ MongoDB 端口 27017 已监听" -ForegroundColor Green
    Write-Host "  进程: PID $($mongoPort.OwningProcess)" -ForegroundColor Gray
    
    # 如果 mongosh 可用，尝试查询版本
    $mongoshAvailable = Get-Command mongosh -ErrorAction SilentlyContinue
    if ($mongoshAvailable) {
        try {
            $mongoVersion = mongosh --quiet --host localhost --port 27017 -u root -p password --authenticationDatabase admin --eval "db.version()" 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  版本: $mongoVersion" -ForegroundColor Gray
            }
        } catch {
            # 忽略 mongosh 错误
        }
    } else {
        Write-Host "  说明: 应用将通过 MongoDB Driver 连接 (mongosh 工具不是必需的)" -ForegroundColor Gray
    }
} else {
    Write-Host "  ✗ MongoDB 端口 27017 未监听" -ForegroundColor Red
    Write-Host "  请确保 MongoDB Docker 容器已启动" -ForegroundColor Gray
}

# 检查端口占用
Write-Host ""
Write-Host "[5/5] 端口占用情况" -ForegroundColor Yellow
$ports = @(8082, 15432, 27017, 8081)
$portNames = @{
    8082 = "Article Service"
    15432 = "PostgreSQL"
    27017 = "MongoDB"
    8081 = "User Service"
}

foreach ($port in $ports) {
    $connection = netstat -ano | Select-String ":$port " | Select-Object -First 1
    if ($connection) {
        Write-Host "  ✓ 端口 $port ($($portNames[$port]))" -ForegroundColor Green
    } else {
        if ($port -eq 8081) {
            Write-Host "  • 端口 $port ($($portNames[$port])) - 未使用 (正常)" -ForegroundColor Gray
        } else {
            Write-Host "  ✗ 端口 $port ($($portNames[$port])) - 未监听" -ForegroundColor Red
        }
    }
}

# 总结
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 快速操作" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "启动服务:" -ForegroundColor Yellow
Write-Host "  .\start.ps1" -ForegroundColor White
Write-Host ""
Write-Host "测试 API:" -ForegroundColor Yellow
Write-Host "  .\test-api.ps1" -ForegroundColor White
Write-Host ""
Write-Host "重启服务:" -ForegroundColor Yellow
Write-Host "  .\restart.ps1" -ForegroundColor White
Write-Host ""
Write-Host "初始化数据库:" -ForegroundColor Yellow
Write-Host "  .\init-db.ps1" -ForegroundColor White
Write-Host ""
