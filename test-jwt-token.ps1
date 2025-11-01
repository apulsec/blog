# Test login and get JWT token with userId
$loginBody = @{
    username = "eeeeeee"
    password = "123456"
} | ConvertTo-Json

Write-Host "Testing login to get JWT token with userId..." -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
    
    Write-Host "`nLogin successful!" -ForegroundColor Green
    Write-Host "Access Token: $($response.accessToken.Substring(0, 50))..." -ForegroundColor Yellow
    
    # Decode JWT token to check if userId exists
    $tokenParts = $response.accessToken.Split('.')
    if ($tokenParts.Length -eq 3) {
        $payload = $tokenParts[1]
        # Add padding if needed
        while ($payload.Length % 4 -ne 0) {
            $payload += "="
        }
        $decodedBytes = [System.Convert]::FromBase64String($payload)
        $decodedJson = [System.Text.Encoding]::UTF8.GetString($decodedBytes)
        $claims = $decodedJson | ConvertFrom-Json
        
        Write-Host "`nJWT Claims:" -ForegroundColor Cyan
        Write-Host $decodedJson -ForegroundColor White
        
        if ($claims.userId) {
            Write-Host "`n✓ userId found in token: $($claims.userId)" -ForegroundColor Green
        } else {
            Write-Host "`n✗ userId NOT found in token!" -ForegroundColor Red
        }
    }
    
} catch {
    Write-Host "`nLogin failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "Details: $($_.ErrorDetails)" -ForegroundColor Red
    }
}
