# PowerShell script to test user registration

Write-Host "Testing User Registration API..." -ForegroundColor Green

$baseUrl = "http://localhost:8083"

# Test 1: Register a new user
Write-Host "`n=== Test 1: Register New User ===" -ForegroundColor Cyan

$registrationData = @{
    username = "testuser"
    password = "password123"
    email    = "testuser@example.com"
} | ConvertTo-Json

Write-Host "Request:" -ForegroundColor Yellow
Write-Host $registrationData

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/users/register" `
        -Method Post `
        -ContentType "application/json" `
        -Body $registrationData
    
    Write-Host "`nResponse:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3
    
    $userId = $response.id
    Write-Host "`nUser registered successfully! User ID: $userId" -ForegroundColor Green
    
    # Test 2: Get user by ID
    Write-Host "`n=== Test 2: Get User by ID ===" -ForegroundColor Cyan
    $userResponse = Invoke-RestMethod -Uri "$baseUrl/api/users/$userId" -Method Get
    
    Write-Host "Response:" -ForegroundColor Green
    $userResponse | ConvertTo-Json -Depth 3
    
    # Test 3: Try to register with duplicate identifier (should fail)
    Write-Host "`n=== Test 3: Duplicate Registration (Expected to Fail) ===" -ForegroundColor Cyan
    try {
        $duplicateResponse = Invoke-RestMethod -Uri "$baseUrl/api/users/register" `
            -Method Post `
            -ContentType "application/json" `
            -Body $registrationData
        Write-Host "Unexpected: Duplicate registration succeeded!" -ForegroundColor Red
    } catch {
        Write-Host "Expected error: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    # Test 4: Internal endpoint - Get auth details
    Write-Host "`n=== Test 4: Get Auth Details (Internal Endpoint) ===" -ForegroundColor Cyan
    $authDetailsUrl = "$baseUrl/api/users/internal/auth-details?identityType=username&identifier=testuser"
    $authDetails = Invoke-RestMethod -Uri $authDetailsUrl -Method Get
    
    Write-Host "Response:" -ForegroundColor Green
    $authDetails | ConvertTo-Json -Depth 3
    Write-Host "`nNote: Credential is BCrypt hashed password" -ForegroundColor Yellow
    
    Write-Host "`n=== All Tests Completed ===" -ForegroundColor Green
    
} catch {
    Write-Host "`nError occurred:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
}

Write-Host "`nTest script finished." -ForegroundColor Cyan
