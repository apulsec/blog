# 博客文章服务 - Docker 数据库初始化脚本
# 用途: 通过 Docker exec 方式在容器内执行 SQL 初始化

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  博客文章服务 - Docker 数据库初始化工具" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Docker 是否可用
Write-Host "[1/3] 检查 Docker 环境..." -ForegroundColor Yellow
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    Write-Host "✗ Docker 命令不可用!" -ForegroundColor Red
    Write-Host "  请确保 Docker Desktop 已安装并运行" -ForegroundColor Yellow
    exit 1
}
Write-Host "✓ Docker 已就绪" -ForegroundColor Green
Write-Host ""

# 查找 PostgreSQL 容器
Write-Host "[2/3] 查找 PostgreSQL 容器..." -ForegroundColor Yellow
$pgContainers = docker ps --filter "publish=15432" --format "{{.Names}}" 2>&1

if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($pgContainers)) {
    Write-Host "✗ 未找到运行在 15432 端口的 PostgreSQL 容器" -ForegroundColor Red
    Write-Host ""
    Write-Host "请确保 PostgreSQL Docker 容器正在运行,例如:" -ForegroundColor Yellow
    Write-Host "  docker run -d -p 15432:5432 -e POSTGRES_PASSWORD=password postgres" -ForegroundColor Gray
    exit 1
}

$containerName = $pgContainers.Split([Environment]::NewLine)[0].Trim()
Write-Host "✓ 找到容器: $containerName" -ForegroundColor Green
Write-Host ""

# 执行初始化
Write-Host "[3/3] 在容器中执行数据库初始化..." -ForegroundColor Yellow
Write-Host ""

# 创建初始化 SQL (内嵌在脚本中)
$initSQL = @"
-- 创建数据库 (仅在不存在时创建)
SELECT 'CREATE DATABASE blog_article_db'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'blog_article_db'
)\gexec

-- 连接到数据库
\c blog_article_db

-- 清空数据库（删除所有表以保证初始化幂等性）
DO $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
        EXECUTE format('DROP TABLE IF EXISTS %I CASCADE;', rec.tablename);
    END LOOP;
END $$;

-- 清除序列以防遗留 ID
DO $$
DECLARE
    seq RECORD;
BEGIN
    FOR seq IN (
        SELECT sequence_schema, sequence_name
        FROM information_schema.sequences
        WHERE sequence_schema = 'public'
    ) LOOP
        EXECUTE format('DROP SEQUENCE IF EXISTS %I.%I CASCADE;', seq.sequence_schema, seq.sequence_name);
    END LOOP;
END $$;

-- 双保险：显式删除关键表，防止旧版本中存在不同 schema 时残留数据
DROP TABLE IF EXISTS t_article_tag CASCADE;
DROP TABLE IF EXISTS t_tag CASCADE;
DROP TABLE IF EXISTS t_article CASCADE;

-- 创建文章表
CREATE TABLE IF NOT EXISTS t_article (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500),
    cover_image_url VARCHAR(500),
    author_id BIGINT NOT NULL,
    publish_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PUBLISHED',
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    -- 以下列为前端/服务层查询使用的字段别名（历史遗留/命名不一致问题），增加以避免查询报错
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 确保历史环境补齐新增字段
ALTER TABLE t_article ADD COLUMN IF NOT EXISTS likes_count INT DEFAULT 0;
ALTER TABLE t_article ADD COLUMN IF NOT EXISTS comments_count INT DEFAULT 0;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_author_id ON t_article(author_id);
CREATE INDEX IF NOT EXISTS idx_publish_time ON t_article(publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_status ON t_article(status);

-- 创建标签表（t_tag）: 某些查询依赖此表，服务启动时会读取标签列表
CREATE TABLE IF NOT EXISTS t_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建文章-标签关联表
CREATE TABLE IF NOT EXISTS t_article_tag (
    article_id BIGINT NOT NULL REFERENCES t_article(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES t_tag(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id, tag_id)
);

-- 插入默认标签（若已存在则跳过）
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

-- 插入测试数据
INSERT INTO t_article (title, summary, cover_image_url, author_id, publish_time) VALUES
('Spring Cloud 微服务架构实战', '本文详细介绍了 Spring Cloud 在微服务架构中的应用...', 'https://picsum.photos/400/300?random=1', 1, CURRENT_TIMESTAMP - INTERVAL '0 days'),
('Vue.js 3 组合式 API 入门', '探索 Vue.js 3 全新的组合式 API 设计理念...', 'https://picsum.photos/400/300?random=2', 2, CURRENT_TIMESTAMP - INTERVAL '1 days'),
('Docker 容器化部署指南', 'Docker 是现代应用部署的利器...', 'https://picsum.photos/400/300?random=3', 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
('PostgreSQL 性能优化技巧', '掌握这些技巧，让你的数据库性能提升10倍！...', 'https://picsum.photos/400/300?random=4', 3, CURRENT_TIMESTAMP - INTERVAL '3 days'),
('Kubernetes 集群管理实践', 'Kubernetes 已成为容器编排的事实标准...', 'https://picsum.photos/400/300?random=5', 2, CURRENT_TIMESTAMP - INTERVAL '4 days'),
('Redis 缓存设计模式', '深入探讨 Redis 在高并发场景下的缓存设计模式...', 'https://picsum.photos/400/300?random=6', 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),
('RabbitMQ 消息队列入门', '消息队列是分布式系统的重要组件...', 'https://picsum.photos/400/300?random=7', 3, CURRENT_TIMESTAMP - INTERVAL '6 days'),
('MyBatis-Plus 高级用法', 'MyBatis-Plus 是 MyBatis 的增强工具...', 'https://picsum.photos/400/300?random=8', 2, CURRENT_TIMESTAMP - INTERVAL '7 days'),
('Elasticsearch 全文搜索实战', '学习如何使用 Elasticsearch 构建强大的全文搜索系统...', 'https://picsum.photos/400/300?random=9', 1, CURRENT_TIMESTAMP - INTERVAL '8 days'),
('MongoDB 文档数据库应用', 'MongoDB 是流行的 NoSQL 数据库...', 'https://picsum.photos/400/300?random=10', 3, CURRENT_TIMESTAMP - INTERVAL '9 days'),
('Nginx 反向代理配置详解', '深入理解 Nginx 反向代理的工作原理...', 'https://picsum.photos/400/300?random=11', 2, CURRENT_TIMESTAMP - INTERVAL '10 days'),
('Git 版本控制最佳实践', 'Git 是必备的版本控制工具...', 'https://picsum.photos/400/300?random=12', 1, CURRENT_TIMESTAMP - INTERVAL '11 days'),
('JWT 认证授权机制', '深入剖析 JWT (JSON Web Token) 的工作原理...', 'https://picsum.photos/400/300?random=13', 3, CURRENT_TIMESTAMP - INTERVAL '12 days'),
('Linux 服务器运维指南', 'Linux 服务器运维必备知识...', 'https://picsum.photos/400/300?random=14', 2, CURRENT_TIMESTAMP - INTERVAL '13 days'),
('CI/CD 持续集成实践', '使用 Jenkins、GitLab CI 等工具实现自动化构建...', 'https://picsum.photos/400/300?random=15', 1, CURRENT_TIMESTAMP - INTERVAL '14 days');

-- 为示例文章绑定默认标签
INSERT INTO t_article_tag (article_id, tag_id)
SELECT a.id, t.id
FROM (
    VALUES
        ('Spring Cloud 微服务架构实战', 'Spring Boot'),
        ('Spring Cloud 微服务架构实战', '微服务'),
        ('Spring Cloud 微服务架构实战', '云原生'),
        ('Vue.js 3 组合式 API 入门', 'Vue.js'),
        ('Vue.js 3 组合式 API 入门', '前端开发'),
        ('Docker 容器化部署指南', 'Docker'),
        ('Docker 容器化部署指南', '云原生'),
        ('PostgreSQL 性能优化技巧', '数据库'),
        ('PostgreSQL 性能优化技巧', '后端开发'),
        ('Kubernetes 集群管理实践', '云原生'),
        ('Kubernetes 集群管理实践', '微服务'),
        ('Redis 缓存设计模式', '后端开发'),
        ('Redis 缓存设计模式', '数据库'),
        ('RabbitMQ 消息队列入门', '微服务'),
        ('RabbitMQ 消息队列入门', '后端开发'),
        ('MyBatis-Plus 高级用法', '后端开发'),
        ('MyBatis-Plus 高级用法', 'Spring Boot'),
        ('Elasticsearch 全文搜索实战', '后端开发'),
        ('Elasticsearch 全文搜索实战', '数据库'),
        ('MongoDB 文档数据库应用', '数据库'),
        ('MongoDB 文档数据库应用', '后端开发'),
        ('Nginx 反向代理配置详解', '后端开发'),
        ('Nginx 反向代理配置详解', '微服务'),
        ('Git 版本控制最佳实践', '前端开发'),
        ('Git 版本控制最佳实践', '后端开发'),
        ('JWT 认证授权机制', 'Spring Boot'),
        ('JWT 认证授权机制', '后端开发'),
        ('Linux 服务器运维指南', '后端开发'),
        ('CI/CD 持续集成实践', '微服务'),
        ('CI/CD 持续集成实践', '后端开发')
) AS mapping(article_title, tag_name)
JOIN t_article a ON a.title = mapping.article_title
JOIN t_tag t ON t.name = mapping.tag_name
ON CONFLICT (article_id, tag_id) DO NOTHING;

-- 验证数据
SELECT COUNT(*) as total_articles FROM t_article;
"@

# 将 SQL 写入临时文件
$tempSQL = [System.IO.Path]::GetTempFileName() + ".sql"
$initSQL | Out-File -FilePath $tempSQL -Encoding UTF8

try {
    # 复制 SQL 文件到容器
    Write-Host "正在复制 SQL 脚本到容器..." -ForegroundColor Cyan
    docker cp $tempSQL "${containerName}:/tmp/init.sql" 2>&1 | Out-Null
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ 无法复制文件到容器" -ForegroundColor Red
        exit 1
    }
    
    # 在容器中执行 SQL
    Write-Host "正在执行初始化脚本..." -ForegroundColor Cyan
    $result = docker exec -i $containerName psql -U postgres -f /tmp/init.sql 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "================================================" -ForegroundColor Green
        Write-Host "  ✓ 数据库初始化成功!" -ForegroundColor Green
        Write-Host "================================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "数据库信息:" -ForegroundColor Cyan
        Write-Host "  - 容器: $containerName" -ForegroundColor White
        Write-Host "  - 数据库: blog_article_db" -ForegroundColor White
        Write-Host "  - 表: t_article" -ForegroundColor White
        Write-Host "  - 测试数据: 15 篇文章" -ForegroundColor White
        Write-Host ""

        Write-Host "=== 初始化标签数据 ===" -ForegroundColor Cyan
        $tagSeedData = @(
            @{ Name = 'Vue.js'; Color = '#3B82F6' },
            @{ Name = 'Spring Boot'; Color = '#10B981' },
            @{ Name = '微服务'; Color = '#F59E0B' },
            @{ Name = '数据库'; Color = '#EF4444' },
            @{ Name = '前端开发'; Color = '#8B5CF6' },
            @{ Name = '后端开发'; Color = '#EC4899' },
            @{ Name = 'Docker'; Color = '#3B82F6' },
            @{ Name = '云原生'; Color = '#10B981' }
        )

        foreach ($tag in $tagSeedData) {
            $escapedName = $tag.Name.Replace("'", "''")
            $insertSql = "INSERT INTO t_tag (name, color) VALUES ('$escapedName', '$($tag.Color)') ON CONFLICT (name) DO NOTHING;"
            docker exec $containerName psql -U postgres -d blog_article_db -c "$insertSql" 2>&1 | Out-Null
        }

        Write-Host "" 
        Write-Host "✓ 标签数据初始化完成" -ForegroundColor Green

        Write-Host ""
        Write-Host "当前标签列表:" -ForegroundColor Cyan
        try {
            $tagList = Invoke-RestMethod -Uri "http://localhost:8082/api/articles/tags" -Method Get -TimeoutSec 5
            if ($null -ne $tagList) {
                $tagList | Select-Object id, name, color | Format-Table
            } else {
                Write-Host "未从文章服务获取到标签数据 (返回为空)。" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "无法通过文章服务 API 获取标签列表，可能服务尚未启动。" -ForegroundColor Yellow
        }
    } else {
        Write-Host ""
        Write-Host "✗ 初始化失败!" -ForegroundColor Red
        Write-Host $result -ForegroundColor Gray
        exit 1
    }
} finally {
    # 清理临时文件
    Remove-Item $tempSQL -ErrorAction SilentlyContinue
}
