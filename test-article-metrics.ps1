param(
    [int]$ArticleId = 1,
    [switch]$RunJob,
    [string]$MetricDate = (Get-Date -Format 'yyyy-MM-dd'),
    [string]$ApiBaseUrl = 'http://localhost:8082'
)

$ErrorActionPreference = 'Stop'

function Invoke-RunArticleMetrics {
    param([string]$Date)

    Write-Host "Triggering Spark aggregation for $Date ..." -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot 'run-article-metrics.ps1') -MetricDate $Date
    if ($LASTEXITCODE -ne 0) {
        throw "Spark aggregation job failed with exit code $LASTEXITCODE"
    }
}

try {
    if ($RunJob.IsPresent) {
        Invoke-RunArticleMetrics -Date $MetricDate
        Start-Sleep -Seconds 2
    } else {
        Write-Host 'Skipping Spark job execution (use -RunJob to enable).' -ForegroundColor Yellow
    }

    $metricsEndpoint = "$ApiBaseUrl/api/articles/$ArticleId/metrics"
    Write-Host "Fetching metrics from $metricsEndpoint" -ForegroundColor Cyan

    $response = Invoke-RestMethod -Uri $metricsEndpoint -Method GET

    if (-not $response) {
        throw 'Metrics endpoint returned an empty response.'
    }

    $requiredFields = @('metricDate', 'likesCount', 'commentsCount', 'hotScore')
    foreach ($field in $requiredFields) {
        if (-not $response.PSObject.Properties.Name.Contains($field)) {
            throw "Missing expected field '$field' in response."
        }
    }

    Write-Host "`n✓ Metrics retrieved successfully" -ForegroundColor Green
    Write-Host "  Article ID   : $($response.articleId)" -ForegroundColor Gray
    Write-Host "  Metric Date  : $($response.metricDate)" -ForegroundColor Gray
    Write-Host "  Likes Count  : $($response.likesCount)" -ForegroundColor Gray
    Write-Host "  Comments Count: $($response.commentsCount)" -ForegroundColor Gray
    Write-Host "  Hot Score    : $($response.hotScore)" -ForegroundColor Gray

    Write-Host "" 
    Write-Host "✓ Article metrics test completed." -ForegroundColor Green
    exit 0
} catch {
    Write-Host "✗ Article metrics test failed" -ForegroundColor Red
    Write-Host ("Error: {0}" -f $_) -ForegroundColor Yellow
    exit 1
}
