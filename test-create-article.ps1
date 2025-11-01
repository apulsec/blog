# Test creating an article with JWT token

# Step 1: Register a test user
Write-Host "Step 1: Registering test user..." -ForegroundColor Cyan
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$registerBody = @{
    username = "testuser$timestamp"
    password = "Test123456"
    email = "test$timestamp@test.com"
    nickname = "TestUser$timestamp"
} | ConvertTo-Json

try {
    $user = Invoke-RestMethod -Uri "http://localhost:8083/api/users/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody
    Write-Host "✓ User registered: $($user.username) (ID: $($user.id))" -ForegroundColor Green
} catch {
    Write-Host "✗ Registration failed: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Login to get JWT token
Write-Host "`nStep 2: Logging in..." -ForegroundColor Cyan
$loginBody = @{
    username = "testuser$timestamp"
    password = "Test123456"
} | ConvertTo-Json

try {
    $authResult = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody
    $token = $authResult.accessToken
    Write-Host "✓ Login successful" -ForegroundColor Green
    Write-Host "  Token: $($token.Substring(0,50))..." -ForegroundColor Gray
} catch {
    Write-Host "✗ Login failed: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Verify token contains userId
Write-Host "`nStep 3: Verifying token..." -ForegroundColor Cyan
$parts = $token -split '\.'
$payload = $parts[1]
$padding = 4 - ($payload.Length % 4)
if ($padding -ne 4) { $payload += "=" * $padding }
$payload = $payload.Replace('-', '+').Replace('_', '/')
$bytes = [System.Convert]::FromBase64String($payload)
$json = [System.Text.Encoding]::UTF8.GetString($bytes)
$claims = $json | ConvertFrom-Json

if ($claims.userId) {
    Write-Host "✓ Token contains userId: $($claims.userId)" -ForegroundColor Green
} else {
    Write-Host "✗ Token does NOT contain userId!" -ForegroundColor Red
    Write-Host "Token claims: $json" -ForegroundColor Yellow
    exit 1
}

# Step 4: Create an article
Write-Host "`nStep 4: Creating article..." -ForegroundColor Cyan
$articleBody = @{
    title = "Test Article $timestamp"
    summary = "This is a test article"
    content = "## Test Content`n`nThis is the test article content."
    status = "PUBLISHED"
    tags = @("test", "demo")
} | ConvertTo-Json

try {
    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }
    
    $article = Invoke-RestMethod -Uri "http://localhost:8082/api/articles" `
        -Method POST `
        -Headers $headers `
        -Body $articleBody
    
    Write-Host "✓ Article created successfully!" -ForegroundColor Green
    Write-Host "  Article ID: $($article.id)" -ForegroundColor Gray
    Write-Host "  Title: $($article.title)" -ForegroundColor Gray
    Write-Host "  Author ID: $($article.authorId)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Article creation failed!" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
    Write-Host "Error: $_" -ForegroundColor Yellow
    
    if ($_.ErrorDetails.Message) {
        Write-Host "Details: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
    }
    exit 1
}

Write-Host "`n✓ All tests passed!" -ForegroundColor Green
