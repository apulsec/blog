# 测试前端服务连通性
Write-Host "测试前端到后端服务的连接..." -ForegroundColor Cyan
Write-Host ""

# 测试用户服务 - 直接访问
Write-Host "1. 测试用户服务 (直接访问 8083)..." -ForegroundColor Yellow
try {
    $body = @{
        username = "directtest"
        password = "Test123456"
        email = "directtest@test.com"
        nickname = "Direct Test"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "http://localhost:8083/api/users/register" `
        -Method POST -ContentType "application/json" -Body $body
    
    Write-Host "✓ 用户服务直接访问成功" -ForegroundColor Green
    Write-Host "  返回: $($response | ConvertTo-Json -Compress)" -ForegroundColor Gray
} catch {
    Write-Host "✗ 用户服务直接访问失败: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 测试前端代理 - 通过Vite
Write-Host "2. 测试前端代理 (Vite 5173 -> 用户服务 8083)..." -ForegroundColor Yellow
try {
    $body = @{
        username = "proxytest"
        password = "Test123456"
        email = "proxytest@test.com"
        nickname = "Proxy Test"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "http://localhost:5173/api/users/register" `
        -Method POST -ContentType "application/json" -Body $body
    
    Write-Host "✓ 前端代理访问成功" -ForegroundColor Green
    Write-Host "  返回: $($response | ConvertTo-Json -Compress)" -ForegroundColor Gray
} catch {
    Write-Host "✗ 前端代理访问失败: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  确保前端开发服务器正在运行: npm run dev" -ForegroundColor Yellow
}

Write-Host ""

# 测试认证服务登录
Write-Host "3. 测试认证服务登录..." -ForegroundColor Yellow
try {
    $loginBody = @{
        username = "proxytest"
        password = "Test123456"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
        -Method POST -ContentType "application/json" -Body $loginBody
    
    Write-Host "✓ 登录成功" -ForegroundColor Green
    Write-Host "  AccessToken: $($response.accessToken.Substring(0,20))..." -ForegroundColor Gray
} catch {
    Write-Host "✗ 登录失败: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "测试完成！" -ForegroundColor Cyan
