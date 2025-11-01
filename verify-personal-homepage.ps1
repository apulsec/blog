# 验证个人主页筛选功能
Write-Host "=== 验证个人主页文章筛选 ===" -ForegroundColor Cyan

# 先创建测试用户（如果不存在）
Write-Host "`n0. 准备测试用户..." -ForegroundColor Yellow
$testUsername = "testuser_homepage"
$testPassword = "Test123456"
$testEmail = "homepage@test.com"
$testNickname = "HomepageTestUser"

try {
    $registerBody = @{
        username = $testUsername
        password = $testPassword
        email = $testEmail
        nickname = $testNickname
    } | ConvertTo-Json
    
    $regResp = Invoke-RestMethod -Uri "http://localhost:8083/api/users/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody
    Write-Host "✓ 创建新测试用户成功" -ForegroundColor Green
} catch {
    Write-Host "  用户可能已存在，继续..." -ForegroundColor Gray
}

# 测试登录
Write-Host "`n1. 测试登录..." -ForegroundColor Yellow
try {
    $loginBody = @{
        username = $testUsername
        password = $testPassword
    } | ConvertTo-Json
    
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody
} catch {
    Write-Host "✗ 登录失败: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Response: $($_.ErrorDetails.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "✓ 登录成功" -ForegroundColor Green
Write-Host "登录响应结构: $($loginResponse | ConvertTo-Json -Depth 3)" -ForegroundColor Gray

# 尝试多种可能的token路径
$token = $null
if ($loginResponse.data.accessToken) {
    $token = $loginResponse.data.accessToken
} elseif ($loginResponse.accessToken) {
    $token = $loginResponse.accessToken
} elseif ($loginResponse.data) {
    $token = $loginResponse.data
}

if (-not $token) {
    Write-Host "✗ 无法获取token" -ForegroundColor Red
    Write-Host "响应内容: $($loginResponse | Out-String)" -ForegroundColor Red
    exit 1
}

Write-Host "Token: $($token.Substring(0,20))..." -ForegroundColor Gray

# 解析JWT获取userId (处理URL-safe Base64)
$jwtParts = $token.Split('.')
$payloadBase64 = $jwtParts[1]
# 添加Base64 padding
$padding = (4 - ($payloadBase64.Length % 4)) % 4
$payloadBase64 = $payloadBase64 + ('=' * $padding)
# 替换URL-safe字符
$payloadBase64 = $payloadBase64.Replace('-', '+').Replace('_', '/')

$payload = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($payloadBase64))
$userId = ($payload | ConvertFrom-Json).userId
Write-Host "当前用户ID: $userId" -ForegroundColor Cyan

# 获取所有文章
Write-Host "`n2. 获取所有文章..." -ForegroundColor Yellow
$allArticles = Invoke-RestMethod -Uri "http://localhost:8082/api/articles?pageNum=1&pageSize=20" `
    -Method GET

# 文章服务可能直接返回分页对象，不在data字段中
$allTotal = if ($allArticles.total) { $allArticles.total } else { $allArticles.data.total }
$allRecords = if ($allArticles.records) { $allArticles.records } else { $allArticles.data.records }

Write-Host "✓ 总文章数: $allTotal" -ForegroundColor Green

# 获取当前用户的文章
Write-Host "`n3. 获取当前用户的文章..." -ForegroundColor Yellow
$myArticles = Invoke-RestMethod -Uri "http://localhost:8082/api/articles?pageNum=1&pageSize=20&authorId=$userId" `
    -Method GET

$myTotal = if ($myArticles.total) { $myArticles.total } else { $myArticles.data.total }
$myRecords = if ($myArticles.records) { $myArticles.records } else { $myArticles.data.records }

Write-Host "✓ 我的文章数: $myTotal" -ForegroundColor Green

# 显示文章详情
Write-Host "`n4. 文章列表详情:" -ForegroundColor Yellow
foreach ($article in $myRecords) {
    Write-Host "  - [$($article.id)] $($article.title)" -ForegroundColor White
    Write-Host "    作者: $($article.author.nickname) (ID: $($article.author.id))" -ForegroundColor Gray
}

# 验证结果
Write-Host "`n5. 验证结果:" -ForegroundColor Yellow
if ($myTotal -lt $allTotal) {
    Write-Host "✓ 个人主页筛选正常 (我的文章 < 全部文章)" -ForegroundColor Green
} elseif ($myTotal -eq 0) {
    Write-Host "⚠ 当前用户还没有文章" -ForegroundColor Yellow
} elseif ($myTotal -eq $allTotal) {
    Write-Host "⚠ 可能所有文章都是你的，或者筛选未生效" -ForegroundColor Yellow
} else {
    Write-Host "✗ 筛选异常" -ForegroundColor Red
}

Write-Host "`n=== 前端使用说明 ===" -ForegroundColor Cyan
Write-Host "1. 打开浏览器控制台 (F12)" -ForegroundColor White
Write-Host "2. 在 Console 中执行: localStorage.clear()" -ForegroundColor White
Write-Host "3. 刷新页面并重新登录" -ForegroundColor White
Write-Host "4. 进入个人主页，应该只看到你的文章" -ForegroundColor White
Write-Host "`n当前用户 (ID=$userId) 的文章数: $myTotal" -ForegroundColor Cyan
Write-Host "数据库总文章数: $allTotal" -ForegroundColor Cyan

if ($myTotal -eq 0) {
    Write-Host "`n⚠ 提示: 当前用户还没有文章，请先创建一篇文章再测试筛选功能" -ForegroundColor Yellow
}
