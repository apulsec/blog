# 启动所有后端服务进行集成测试
# 依次启动: user-service -> auth-service

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  启动博客平台后端服务" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$rootPath = $PSScriptRoot

# ============================================================
# 检查先决条件
# ============================================================
Write-Host "检查先决条件..." -ForegroundColor Yellow

# 检查 Redis
Write-Host "  检查 Redis (端口 6379)..." -ForegroundColor Gray
try {
    $redisTest = Test-NetConnection -ComputerName localhost -Port 6379 -WarningAction SilentlyContinue
    if ($redisTest.TcpTestSucceeded) {
        Write-Host "  ✓ Redis 运行中" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Redis 未运行! 请启动 Redis" -ForegroundColor Red
        Write-Host "    docker run -d --name redis -p 6379:6379 redis:7-alpine" -ForegroundColor Gray
        exit 1
    }
} catch {
    Write-Host "  ✗ 无法连接 Redis" -ForegroundColor Red
    exit 1
}

# 检查 PostgreSQL
Write-Host "  检查 PostgreSQL (端口 15432)..." -ForegroundColor Gray
try {
    $pgTest = Test-NetConnection -ComputerName localhost -Port 15432 -WarningAction SilentlyContinue
    if ($pgTest.TcpTestSucceeded) {
        Write-Host "  ✓ PostgreSQL 运行中" -ForegroundColor Green
    } else {
        Write-Host "  ✗ PostgreSQL 未运行! 请启动 PostgreSQL" -ForegroundColor Red
        Write-Host "    cd blog-user-service" -ForegroundColor Gray
        Write-Host "    .\init-db.ps1" -ForegroundColor Gray
        exit 1
    }
} catch {
    Write-Host "  ✗ 无法连接 PostgreSQL" -ForegroundColor Red
    exit 1
}

Write-Host ""

# ============================================================
# 启动 blog-user-service
# ============================================================
Write-Host "[1/2] 启动 blog-user-service (端口 8083)..." -ForegroundColor Yellow

Set-Location "$rootPath\blog-user-service"

# 检查端口是否占用
$port8083 = Get-NetTCPConnection -LocalPort 8083 -ErrorAction SilentlyContinue
if ($port8083) {
    Write-Host "  端口 8083 已被占用，跳过启动" -ForegroundColor Yellow
} else {
    Write-Host "  编译 blog-user-service..." -ForegroundColor Gray
    mvn clean compile -q
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  启动服务..." -ForegroundColor Gray
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$rootPath\blog-user-service'; mvn spring-boot:run"
        
        # 等待服务启动
        Write-Host "  等待服务启动..." -ForegroundColor Gray
        Start-Sleep -Seconds 10
        
        # 验证服务
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:8083/api/users/1" -Method Get -ErrorAction Stop
            Write-Host "  ✓ blog-user-service 启动成功!" -ForegroundColor Green
        } catch {
            Write-Host "  ⚠ blog-user-service 可能需要更多时间启动" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ✗ 编译失败" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# ============================================================
# 启动 blog-auth-service
# ============================================================
Write-Host "[2/2] 启动 blog-auth-service (端口 8081)..." -ForegroundColor Yellow

Set-Location "$rootPath\blog-auth-service"

# 检查端口是否占用
$port8081 = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue
if ($port8081) {
    Write-Host "  端口 8081 已被占用，跳过启动" -ForegroundColor Yellow
} else {
    Write-Host "  编译 blog-auth-service..." -ForegroundColor Gray
    mvn clean compile -q
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  启动服务..." -ForegroundColor Gray
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$rootPath\blog-auth-service'; mvn spring-boot:run"
        
        # 等待服务启动
        Write-Host "  等待服务启动..." -ForegroundColor Gray
        Start-Sleep -Seconds 15
        
        # 验证服务
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -Method Get -ErrorAction Stop
            Write-Host "  ✓ blog-auth-service 启动成功!" -ForegroundColor Green
        } catch {
            Write-Host "  ⚠ blog-auth-service 可能需要更多时间启动" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ✗ 编译失败" -ForegroundColor Red
        exit 1
    }
}

Set-Location $rootPath

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  所有服务已启动!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "服务地址:" -ForegroundColor Yellow
Write-Host "  blog-user-service: http://localhost:8083" -ForegroundColor White
Write-Host "  blog-auth-service: http://localhost:8081" -ForegroundColor White
Write-Host ""

Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "  运行集成测试: .\test-integration.ps1" -ForegroundColor White
Write-Host ""
