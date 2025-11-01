# Check article service logs for errors
Write-Host "Checking article service for recent errors..." -ForegroundColor Cyan

# Try to get logs from the running Maven process
$logFile = "C:\Users\lenovo\Desktop\blog\blog-article-service\article-service.log"

# Check if there's a log file
if (Test-Path $logFile) {
    Write-Host "`nRecent errors from log file:" -ForegroundColor Yellow
    Get-Content $logFile | Select-String -Pattern "ERROR|Exception|Caused by" | Select-Object -Last 20
} else {
    Write-Host "No log file found. Please check the terminal where article service is running." -ForegroundColor Yellow
}

# Also try to curl the actuator health endpoint
Write-Host "`nChecking article service health..." -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health"
    Write-Host "Service Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "Cannot reach article service health endpoint: $_" -ForegroundColor Red
}
