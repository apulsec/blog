[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [int]
    $StartId = 1,

    [Parameter(Mandatory = $false)]
    [int]
    $EndId = 15,

    [Parameter(Mandatory = $false)]
    [string]
    $ContainerName,

    [Parameter(Mandatory = $false)]
    [string]
    $Database = "blog_article_db",

    [Parameter(Mandatory = $false)]
    [string]
    $PostgresUser = "postgres",

    [Parameter(Mandatory = $false)]
    [int]
    $PublishedPort = 15432
)

$ErrorActionPreference = "Stop"

Write-Host "=== Delete Demo Articles ===" -ForegroundColor Cyan
Write-Host "Start ID: $StartId" -ForegroundColor Gray
Write-Host "End ID  : $EndId" -ForegroundColor Gray

if ($StartId -le 0 -or $EndId -le 0) {
    throw "Article IDs must be positive integers."
}

if ($StartId -gt $EndId) {
    throw "StartId cannot be greater than EndId."
}

$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    throw "Docker command not found. Please install and start Docker Desktop."
}

if (-not $ContainerName) {
    Write-Host "Looking for PostgreSQL container on port $PublishedPort..." -ForegroundColor Yellow
    $candidates = docker ps --filter "publish=$PublishedPort" --format "{{.Names}}" 2>$null

    if (-not [string]::IsNullOrWhiteSpace($candidates)) {
        $ContainerName = $candidates.Split([Environment]::NewLine, [StringSplitOptions]::RemoveEmptyEntries)[0]
        Write-Host "Using container: $ContainerName" -ForegroundColor Green
    }
    else {
        Write-Host "No container found on port $PublishedPort. Searching for postgres ancestor..." -ForegroundColor Yellow
        $candidates = docker ps --filter "ancestor=postgres" --format "{{.Names}}" 2>$null

        if (-not [string]::IsNullOrWhiteSpace($candidates)) {
            $ContainerName = $candidates.Split([Environment]::NewLine, [StringSplitOptions]::RemoveEmptyEntries)[0]
            Write-Host "Using container: $ContainerName" -ForegroundColor Green
        }
        else {
            throw "No running PostgreSQL container detected. Pass -ContainerName explicitly."
        }
    }
}
else {
    Write-Host "Using provided container: $ContainerName" -ForegroundColor Green
}

$tempSql = [System.IO.Path]::GetTempFileName() + ".sql"
$outPathInContainer = "/tmp/delete_articles.sql"

$sql = @"
BEGIN;

DELETE FROM article_metrics
WHERE article_id BETWEEN $StartId AND $EndId;

DELETE FROM comments
WHERE article_id BETWEEN $StartId AND $EndId;

DELETE FROM article_likes
WHERE article_id BETWEEN $StartId AND $EndId;

DELETE FROM t_article_tag
WHERE article_id BETWEEN $StartId AND $EndId;

DELETE FROM t_article
WHERE id BETWEEN $StartId AND $EndId;

COMMIT;
"@

$sql | Out-File -FilePath $tempSql -Encoding UTF8 -Force

try {
    Write-Host "Uploading SQL script to container..." -ForegroundColor Yellow
    docker cp $tempSql "${ContainerName}:$outPathInContainer" | Out-Null

    Write-Host "Executing delete script..." -ForegroundColor Yellow
    $execResult = docker exec -i $ContainerName psql -U $PostgresUser -d $Database -v ON_ERROR_STOP=1 -f $outPathInContainer 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Host $execResult -ForegroundColor Red
        throw "Failed to execute delete script inside container."
    }

    Write-Host "Delete operation completed successfully." -ForegroundColor Green
}
finally {
    if (Test-Path $tempSql) {
        Remove-Item $tempSql -ErrorAction SilentlyContinue
    }

    try {
        docker exec $ContainerName rm -f $outPathInContainer 2>$null | Out-Null
    }
    catch {
        # Clean-up failures inside the container are non-fatal
    }
}
