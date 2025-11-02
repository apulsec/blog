# 启动所有服务进行集成测试
# 依次启动: eureka-server -> user-service -> article-service -> auth-service -> frontend

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  启动博客平台服务" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$rootPath = $PSScriptRoot

# ============================================================
# 检查先决条件
# ============================================================
Write-Host "检查先决条件..." -ForegroundColor Yellow

# 清除可能残留的直连配置以确保使用服务发现
Remove-Item Env:USER_SERVICE_URL -ErrorAction SilentlyContinue

$dockerEurekaPort = 18761
$localEurekaPort = 8761
$eurekaHostPort = $null
$eurekaUri = $null

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
# 启动或连接 blog-eureka-server
# ============================================================
Write-Host "[1/5] 检查 blog-eureka-server ..." -ForegroundColor Yellow

$dockerEurekaRunning = $false
try {
    $dockerTest = Test-NetConnection -ComputerName localhost -Port $dockerEurekaPort -WarningAction SilentlyContinue
    if ($dockerTest.TcpTestSucceeded) {
        $dockerEurekaRunning = $true
        $eurekaHostPort = $dockerEurekaPort
        $eurekaUri = "http://localhost:$dockerEurekaPort/eureka/"
        Write-Host "  ✓ 检测到 Docker 版 Eureka (端口 $dockerEurekaPort)" -ForegroundColor Green
    }
} catch {
    $dockerEurekaRunning = $false
}

if (-not $dockerEurekaRunning) {
    $localEurekaRunning = $false
    try {
        $localTest = Test-NetConnection -ComputerName localhost -Port $localEurekaPort -WarningAction SilentlyContinue
        if ($localTest.TcpTestSucceeded) {
            $localEurekaRunning = $true
            Write-Host "  ✓ 本地 Eureka Server 已在运行 (端口 $localEurekaPort)" -ForegroundColor Green
        }
    } catch {
        $localEurekaRunning = $false
    }

    if (-not $localEurekaRunning) {
        Set-Location "$rootPath\blog-eureka-server"

        Write-Host "  编译 blog-eureka-server..." -ForegroundColor Gray
        mvn clean package -DskipTests -q

        if ($LASTEXITCODE -eq 0) {
            Write-Host "  启动 Eureka Server..." -ForegroundColor Gray
            $command = "& { `$env:JAVA_OPTS='-Xms256m -Xmx512m'; cd '$rootPath\blog-eureka-server'; mvn spring-boot:run }"
            Start-Process powershell -ArgumentList "-NoExit", "-Command", $command

            Write-Host "  等待服务启动..." -ForegroundColor Gray
            Start-Sleep -Seconds 10

            try {
                $verify = Test-NetConnection -ComputerName localhost -Port $localEurekaPort -WarningAction SilentlyContinue
                if ($verify.TcpTestSucceeded) {
                    Write-Host "  ✓ blog-eureka-server 启动成功!" -ForegroundColor Green
                } else {
                    Write-Host "  ⚠ 无法验证 Eureka Server 状态，请手动检查" -ForegroundColor Yellow
                }
            } catch {
                Write-Host "  ⚠ 无法验证 Eureka Server 状态，请手动检查" -ForegroundColor Yellow
            }
        } else {
            Write-Host "  ✗ 编译失败" -ForegroundColor Red
            exit 1
        }

        Set-Location $rootPath
    } else {
        Set-Location $rootPath
    }

    $eurekaHostPort = $localEurekaPort
    $eurekaUri = "http://localhost:$localEurekaPort/eureka/"
}

# 在当前会话中开启 Eureka 客户端能力，后续子进程也会继承
$env:EUREKA_ENABLED = "true"
$env:EUREKA_URI = $eurekaUri
$env:EUREKA_REGISTER = "true"
$env:EUREKA_FETCH = "true"

# ============================================================
# 启动 blog-user-service
# ============================================================
Write-Host "[2/5] 启动 blog-user-service (端口 8083)..." -ForegroundColor Yellow

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
    $userServiceCommand = "& { `$env:EUREKA_ENABLED='true'; `$env:EUREKA_URI='$eurekaUri'; `$env:EUREKA_REGISTER='true'; `$env:EUREKA_FETCH='true'; cd '$rootPath\blog-user-service'; mvn spring-boot:run }"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $userServiceCommand
        
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
# 启动 blog-article-service
# ============================================================
Write-Host "[3/5] 启动 blog-article-service (端口 8082)..." -ForegroundColor Yellow

Set-Location "$rootPath\blog-article-service"

# 检查端口是否占用
$port8082 = Get-NetTCPConnection -LocalPort 8082 -ErrorAction SilentlyContinue
if ($port8082) {
    Write-Host "  端口 8082 已被占用，跳过启动" -ForegroundColor Yellow
} else {
    Write-Host "  编译 blog-article-service..." -ForegroundColor Gray
    mvn clean compile -q
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  启动服务..." -ForegroundColor Gray
    $articleServiceCommand = "& { `$env:EUREKA_ENABLED='true'; `$env:EUREKA_URI='$eurekaUri'; `$env:EUREKA_REGISTER='true'; `$env:EUREKA_FETCH='true'; cd '$rootPath\blog-article-service'; mvn spring-boot:run }"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $articleServiceCommand
        
        # 等待服务启动
        Write-Host "  等待服务启动..." -ForegroundColor Gray
        Start-Sleep -Seconds 12
        
        # 验证服务
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health" -Method Get -ErrorAction Stop
            Write-Host "  ✓ blog-article-service 启动成功!" -ForegroundColor Green
        } catch {
            Write-Host "  ⚠ blog-article-service 可能需要更多时间启动" -ForegroundColor Yellow
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
Write-Host "[4/5] 启动 blog-auth-service (端口 8081)..." -ForegroundColor Yellow
Set-Location $rootPath

# ============================================================
# 启动 blog-frontend
# ============================================================
Write-Host "[5/5] 启动 blog-frontend (端口 5173)..." -ForegroundColor Yellow

Set-Location "$rootPath\blog-frontend"

$frontendPort = 5173

# 检查 npm 是否可用
$npmCommand = Get-Command npm -ErrorAction SilentlyContinue
if (-not $npmCommand) {
    Write-Host "  ✗ 未检测到 npm，请安装 Node.js (包含 npm)" -ForegroundColor Red
    exit 1
}

# 安装依赖（如有必要）
if (-not (Test-Path "$rootPath\blog-frontend\node_modules")) {
    Write-Host "  安装依赖 (npm install)..." -ForegroundColor Gray
    npm install | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ✗ npm install 失败" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "  已检测到 node_modules，跳过安装" -ForegroundColor Gray
}

# 检查端口是否占用
$frontendPortUsed = Get-NetTCPConnection -LocalPort $frontendPort -ErrorAction SilentlyContinue
if ($frontendPortUsed) {
    Write-Host "  端口 $frontendPort 已被占用，跳过启动" -ForegroundColor Yellow
} else {
    Write-Host "  启动前端开发服务器..." -ForegroundColor Gray
    $frontendCommand = "& { cd '$rootPath\blog-frontend'; npm run dev -- --host }"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCommand

    Write-Host "  等待前端启动..." -ForegroundColor Gray
    Start-Sleep -Seconds 5

    Write-Host "  ✓ blog-frontend 已启动 (监听 http://localhost:$frontendPort)" -ForegroundColor Green
}

Set-Location $rootPath

Write-Host ""

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  所有服务已启动!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "服务地址:" -ForegroundColor Yellow
if (-not $eurekaHostPort) {
    $eurekaHostPort = $localEurekaPort
}
Write-Host "  blog-eureka-server: http://localhost:$eurekaHostPort" -ForegroundColor White
Write-Host "  blog-article-service: http://localhost:8082" -ForegroundColor White
Write-Host "  blog-user-service: http://localhost:8083" -ForegroundColor White
Write-Host "  blog-auth-service: http://localhost:8081" -ForegroundColor White
Write-Host "  blog-frontend: http://localhost:$frontendPort" -ForegroundColor White
Write-Host ""

Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "  运行集成测试: .\test-integration.ps1" -ForegroundColor White
Write-Host ""

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
    $authServiceCommand = "& { `$env:EUREKA_ENABLED='true'; `$env:EUREKA_URI='$eurekaUri'; `$env:EUREKA_REGISTER='true'; `$env:EUREKA_FETCH='true'; cd '$rootPath\blog-auth-service'; mvn spring-boot:run }"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $authServiceCommand
        
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
if (-not $eurekaHostPort) {
    $eurekaHostPort = $localEurekaPort
}
Write-Host "  blog-eureka-server: http://localhost:$eurekaHostPort" -ForegroundColor White
Write-Host "  blog-article-service: http://localhost:8082" -ForegroundColor White
Write-Host "  blog-user-service: http://localhost:8083" -ForegroundColor White
Write-Host "  blog-auth-service: http://localhost:8081" -ForegroundColor White
Write-Host ""

Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "  运行集成测试: .\test-integration.ps1" -ForegroundColor White
Write-Host ""
