param(
    [string]$AuthBase = "http://localhost:8081",
    [string]$UserBase = "http://localhost:8083",
    [string]$ArticleBase = "http://localhost:8082",
    [switch]$SkipHealthCheck
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

function Invoke-ApiRequest {
    param(
        [Parameter(Mandatory)][ValidateSet("GET", "POST", "DELETE")][string]$Method,
        [Parameter(Mandatory)][string]$Uri,
        $Body = $null,
        [hashtable]$Headers = $null
    )

    $invokeParams = @{ Uri = $Uri; Method = $Method }
    if ($Headers) {
        $invokeParams.Headers = $Headers
    }
    if ($Body -ne $null) {
        $invokeParams.ContentType = "application/json"
        if ($Body -is [string]) {
            $invokeParams.Body = $Body
        } else {
            $invokeParams.Body = $Body | ConvertTo-Json -Depth 8
        }
    }

    return Invoke-RestMethod @invokeParams
}

function Assert-ServiceHealthy {
    param(
        [string]$Name,
        [string]$HealthUrl
    )

    Write-Host " - Checking $Name..." -ForegroundColor DarkGray
    try {
        $response = Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 5 -ErrorAction Stop
        if ($response.status -ne "UP") {
            throw "$Name health endpoint returned status '$($response.status)'"
        }
        Write-Host "   OK" -ForegroundColor Green
    } catch {
        Write-Host "   FAILED: $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

Write-Host "===========================================" -ForegroundColor Cyan
Write-Host " Blog Notification Flow E2E Test Runner" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "Auth:    $AuthBase" -ForegroundColor Gray
Write-Host "User:    $UserBase" -ForegroundColor Gray
Write-Host "Article: $ArticleBase" -ForegroundColor Gray
Write-Host ""

if (-not $SkipHealthCheck) {
    Write-Host "[1/8] Health checks" -ForegroundColor Yellow
    Assert-ServiceHealthy -Name "auth-service" -HealthUrl "$AuthBase/actuator/health"
    Assert-ServiceHealthy -Name "user-service" -HealthUrl "$UserBase/actuator/health"
    Assert-ServiceHealthy -Name "article-service" -HealthUrl "$ArticleBase/actuator/health"
    Write-Host ""
}

$randomSuffix = (Get-Date -Format "yyyyMMddHHmmss") + (Get-Random -Minimum 100 -Maximum 999)
$authorUsername = "author_$randomSuffix"
$actorUsername = "actor_$randomSuffix"
$authorPassword = "Author!$randomSuffix"
$actorPassword = "Actor!$randomSuffix"
$authorEmail = "$authorUsername@example.com"
$actorEmail = "$actorUsername@example.com"

Write-Host "[2/8] Register author account" -ForegroundColor Yellow
$authorRegisterBody = @{
    username = $authorUsername
    email    = $authorEmail
    password = $authorPassword
}
$authorRegisterResponse = Invoke-ApiRequest -Method POST -Uri "$UserBase/api/users/register" -Body $authorRegisterBody
$authorId = $authorRegisterResponse.id
Write-Host "   Created author user $authorUsername (ID=$authorId)" -ForegroundColor Green

Write-Host "[3/8] Register actor account" -ForegroundColor Yellow
$actorRegisterBody = @{
    username = $actorUsername
    email    = $actorEmail
    password = $actorPassword
}
$actorRegisterResponse = Invoke-ApiRequest -Method POST -Uri "$UserBase/api/users/register" -Body $actorRegisterBody
$actorId = $actorRegisterResponse.id
Write-Host "   Created actor user $actorUsername (ID=$actorId)" -ForegroundColor Green

Write-Host "[4/8] Login both accounts to obtain JWT" -ForegroundColor Yellow
$loginEndpoint = "$AuthBase/api/auth/login"
$authorLoginBody = @{ username = $authorUsername; password = $authorPassword }
$actorLoginBody = @{ username = $actorUsername; password = $actorPassword }
$authorLoginResponse = Invoke-ApiRequest -Method POST -Uri $loginEndpoint -Body $authorLoginBody
$actorLoginResponse = Invoke-ApiRequest -Method POST -Uri $loginEndpoint -Body $actorLoginBody
$authorAccessToken = $authorLoginResponse.accessToken
$actorAccessToken = $actorLoginResponse.accessToken
Write-Host "   Author token acquired (length $($authorAccessToken.Length))" -ForegroundColor Green
Write-Host "   Actor token acquired (length $($actorAccessToken.Length))" -ForegroundColor Green
$authorHeaders = @{ Authorization = "Bearer $authorAccessToken" }
$actorHeaders = @{ Authorization = "Bearer $actorAccessToken" }

Write-Host "[5/8] Create published article as author" -ForegroundColor Yellow
$articleTitle = "Notification Demo $randomSuffix"
$articleBody = @{
    title        = $articleTitle
    summary      = "Auto generated summary $randomSuffix"
    coverImageUrl = "https://placehold.co/600x400?text=Blog"
    content      = "<p>End-to-end notification flow verification $randomSuffix</p>"
    status       = "PUBLISHED"
    tags         = @("notifications", "demo")
}
$articleResponse = Invoke-ApiRequest -Method POST -Uri "$ArticleBase/api/articles" -Body $articleBody -Headers $authorHeaders
$articleId = $articleResponse.id
Write-Host "   Article $articleId created" -ForegroundColor Green

Write-Host "[6/8] Actor likes the article" -ForegroundColor Yellow
Invoke-ApiRequest -Method POST -Uri "$ArticleBase/api/articles/$articleId/likes" -Headers $actorHeaders | Out-Null
Write-Host "   Like action completed" -ForegroundColor Green

Write-Host "[7/8] Actor comments on the article" -ForegroundColor Yellow
$commentBody = @{ content = "Great article! Notification flow test $randomSuffix" }
$commentResponse = Invoke-ApiRequest -Method POST -Uri "$ArticleBase/api/articles/$articleId/comments" -Body $commentBody -Headers $actorHeaders
$commentId = $commentResponse.id
Write-Host "   Comment $commentId created" -ForegroundColor Green

Start-Sleep -Seconds 1

Write-Host "[8/8] Fetch notifications for author" -ForegroundColor Yellow
$notifications = Invoke-ApiRequest -Method GET -Uri "$UserBase/api/notifications/me?limit=20" -Headers $authorHeaders
$recentNotifications = @()
if ($notifications) {
    if ($notifications -is [Array]) {
        $recentNotifications = $notifications | Where-Object { $_.articleId -eq $articleId }
    } else {
        if ($notifications.articleId -eq $articleId) {
            $recentNotifications = @($notifications)
        }
    }
}

if ($recentNotifications.Count -eq 0) {
    Write-Host "   No notifications found for the new article." -ForegroundColor Red
    Write-Host "   Raw response:" -ForegroundColor Red
    $notifications | ConvertTo-Json -Depth 5
    exit 1
}

Write-Host "   Retrieved $($recentNotifications.Count) notification(s) for article $articleId" -ForegroundColor Green
$recentNotifications | ForEach-Object {
    Write-Host "     - [$($_.type)] ID=$($_.id) Read=$($_.read) -> $($_.content)" -ForegroundColor Gray
}

$likeCount = ($recentNotifications | Where-Object { $_.type -eq "ARTICLE_LIKE" }).Count
$commentCount = ($recentNotifications | Where-Object { $_.type -eq "ARTICLE_COMMENT" }).Count

if ($likeCount -ge 1 -and $commentCount -ge 1) {
    Write-Host "\nSUCCESS: Like and comment notifications are present." -ForegroundColor Green
} else {
    Write-Host "\nWARNING: Expected both like and comment notifications." -ForegroundColor Yellow
}

$firstNotification = $recentNotifications | Select-Object -First 1
if ($null -ne $firstNotification) {
    Write-Host "\nMarking notification $($firstNotification.id) as read..." -ForegroundColor Yellow
    Invoke-ApiRequest -Method POST -Uri "$UserBase/api/notifications/$($firstNotification.id)/read" -Headers $authorHeaders | Out-Null
    Write-Host "   Done." -ForegroundColor Green
}

Write-Host "\nNext steps:" -ForegroundColor Cyan
Write-Host " - Run this script again if you want fresh data." -ForegroundColor Gray
Write-Host " - Use Invoke-RestMethod '$UserBase/api/notifications/me?unreadOnly=true' with the author token to verify unread filtering." -ForegroundColor Gray
