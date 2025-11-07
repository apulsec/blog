param(
    [string]$MetricDate = (Get-Date -Format 'yyyy-MM-dd'),
    [string]$JdbcUrl = 'jdbc:postgresql://localhost:15432/blog_article_db',
    [string]$DbUser = 'postgres',
    [string]$DbPassword = 'password',
    [string]$SparkHome = $env:SPARK_HOME
)

$ErrorActionPreference = 'Stop'

function Resolve-SparkSubmitPath {
    param([string]$SparkHomePath)

    if (-not $SparkHomePath) {
        throw 'SPARK_HOME is not set. Please install Apache Spark locally and export SPARK_HOME.'
    }

    $candidate = Join-Path $SparkHomePath 'bin\spark-submit.cmd'
    if (Test-Path $candidate) {
        return $candidate
    }

    $fallback = Join-Path $SparkHomePath 'bin\spark-submit'
    if (Test-Path $fallback) {
        return $fallback
    }

    throw "Unable to locate spark-submit under $SparkHomePath"
}

function Ensure-JavaHome {
    if ($env:JAVA_HOME) {
        return
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($null -eq $javaCommand) {
        throw 'JAVA_HOME is not set and no java executable was found in PATH. Please install JDK 17+ and set JAVA_HOME.'
    }

    $javaPath = Split-Path -Parent $javaCommand.Source
    $jdkRoot = Split-Path -Parent $javaPath
    $env:JAVA_HOME = $jdkRoot
    Write-Host "Detected JAVA_HOME at $env:JAVA_HOME" -ForegroundColor Yellow
}

function Invoke-MavenPackage {
    param([string[]]$Arguments)

    $mvn = 'mvn'
    & $mvn @Arguments | Write-Output
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }
}

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$currentLocation = Get-Location

try {
    Set-Location $scriptRoot

    Ensure-JavaHome

    $jarPath = Join-Path $scriptRoot 'analytics-spark-job\target\analytics-spark-job.jar'

    Invoke-MavenPackage -Arguments @('-f', 'analytics-spark-job\pom.xml', '-DskipTests', 'package')

    if (-not (Test-Path $jarPath)) {
        throw "Expected job artifact not found at $jarPath"
    }

    $sparkSubmit = Resolve-SparkSubmitPath -SparkHomePath $SparkHome

    $sparkArgs = @(
        '--class', 'com.example.blog.analytics.ArticleMetricsJob',
        '--master', 'local[*]',
        '--conf', 'spark.sql.shuffle.partitions=1',
        '--conf', 'spark.driver.extraJavaOptions=-Duser.timezone=UTC',
        $jarPath,
        '--jdbc-url', $JdbcUrl,
        '--db-user', $DbUser,
        '--db-password', $DbPassword,
        '--metric-date', $MetricDate
    )

    & $sparkSubmit @sparkArgs
    exit $LASTEXITCODE
}
finally {
    Set-Location $currentLocation
}
