# PowerShell script to start the blog-user-service

Write-Host "Starting blog-user-service..." -ForegroundColor Green

# Check if Maven is installed
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "Error: Maven is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Check if Java is installed
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "Error: Java is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

Write-Host "Checking Java version..." -ForegroundColor Cyan
java -version

Write-Host "`nBuilding the service with Maven..." -ForegroundColor Cyan
mvn clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild successful! Starting the service..." -ForegroundColor Green
    Write-Host "Service will be available at http://localhost:8083" -ForegroundColor Yellow
    Write-Host "Press Ctrl+C to stop the service`n" -ForegroundColor Yellow
    
    java -jar target/blog-user-service-0.0.1-SNAPSHOT.jar
} else {
    Write-Host "`nBuild failed! Please check the error messages above." -ForegroundColor Red
    exit 1
}
