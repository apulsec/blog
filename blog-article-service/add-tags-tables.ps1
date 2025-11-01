# PowerShell script to add tags tables to existing PostgreSQL database in Docker
# This script updates the blog_article_db database with tag-related tables

Write-Host "=== Adding Tags Tables to Blog Article Database ===" -ForegroundColor Cyan
Write-Host ""

# Find the PostgreSQL container by port
$container = docker ps --format "{{.ID}} {{.Ports}}" | Select-String "15432" | ForEach-Object {
    $_.ToString().Split()[0]
}

if (-not $container) {
    Write-Host "ERROR: No PostgreSQL container found on port 15432" -ForegroundColor Red
    Write-Host "Please start the Docker container first using docker-compose up -d" -ForegroundColor Yellow
    exit 1
}

Write-Host "Found PostgreSQL container: $container" -ForegroundColor Green
Write-Host ""

# SQL script to create tables and insert sample data
$sql = @"
-- Create tags table
CREATE TABLE IF NOT EXISTS t_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    color VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create article-tag relationship table
CREATE TABLE IF NOT EXISTS t_article_tag (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES t_article(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES t_tag(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_article_tag_article ON t_article_tag(article_id);
CREATE INDEX IF NOT EXISTS idx_article_tag_tag ON t_article_tag(tag_id);
CREATE INDEX IF NOT EXISTS idx_tag_name ON t_tag(name);

-- Insert sample tags
INSERT INTO t_tag (name, color) VALUES
    ('Vue.js', '#3B82F6'),
    ('Spring Boot', '#10B981'),
    ('微服务', '#F59E0B'),
    ('数据库', '#EF4444'),
    ('前端开发', '#8B5CF6'),
    ('后端开发', '#EC4899'),
    ('Docker', '#3B82F6'),
    ('云原生', '#10B981')
ON CONFLICT (name) DO NOTHING;

-- Add tags to some existing articles
INSERT INTO t_article_tag (article_id, tag_id)
SELECT a.id, t.id
FROM t_article a
CROSS JOIN t_tag t
WHERE a.id <= 10 AND t.name IN ('Spring Boot', '微服务', '后端开发')
ON CONFLICT DO NOTHING;

INSERT INTO t_article_tag (article_id, tag_id)
SELECT a.id, t.id
FROM t_article a
CROSS JOIN t_tag t
WHERE a.id > 10 AND a.id <= 20 AND t.name IN ('Vue.js', '前端开发', 'Docker')
ON CONFLICT DO NOTHING;

SELECT 'Tags tables created and initialized successfully!' as result;
"@

# Save SQL to temp file
$tempFile = [System.IO.Path]::GetTempFileName() + ".sql"
$sql | Out-File -FilePath $tempFile -Encoding UTF8

try {
    Write-Host "Executing SQL script..." -ForegroundColor Yellow
    Write-Host ""
    
    # Execute SQL in Docker container directly
    $result = docker exec $container psql -U postgres -d blog_article_db -c "$sql"
    
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to execute SQL"
    }
    
    Write-Host $result -ForegroundColor Green
    Write-Host ""
    Write-Host "=== Tables Created Successfully ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "New tables:" -ForegroundColor Cyan
    Write-Host "  - t_tag: Stores tag information" -ForegroundColor White
    Write-Host "  - t_article_tag: Many-to-many relationship between articles and tags" -ForegroundColor White
    Write-Host ""
    Write-Host "Sample tags added:" -ForegroundColor Cyan
    Write-Host "  Vue.js, Spring Boot, 微服务, 数据库, 前端开发, 后端开发, Docker, 云原生" -ForegroundColor White
    Write-Host ""
    
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    exit 1
}

Write-Host "Done!" -ForegroundColor Green
