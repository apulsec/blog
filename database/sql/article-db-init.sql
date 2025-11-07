-- ------------------------------------------------------------
-- Blog Article Service - PostgreSQL bootstrap script
-- Purpose: recreate blog_article_db schema with demo data
-- ------------------------------------------------------------

-- Create database if missing
SELECT 'CREATE DATABASE blog_article_db'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'blog_article_db'
)\gexec

-- Switch connection
\c blog_article_db

-- Reduce NOTICE verbosity so初始化日志不出现红色提醒
SET client_min_messages TO warning;

-- Drop existing tables (ensures idempotency)
DO $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
        EXECUTE format('DROP TABLE IF EXISTS %I CASCADE;', rec.tablename);
    END LOOP;
END $$;

-- Drop sequences to avoid leftovers
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

-- Explicitly drop legacy tables
DROP TABLE IF EXISTS t_article_tag CASCADE;
DROP TABLE IF EXISTS t_tag CASCADE;
DROP TABLE IF EXISTS t_article CASCADE;
DROP TABLE IF EXISTS article_likes CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS article_metrics CASCADE;

-- Create t_article table
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
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ensure new columns exist for legacy environments
ALTER TABLE t_article ADD COLUMN IF NOT EXISTS likes_count INT DEFAULT 0;
ALTER TABLE t_article ADD COLUMN IF NOT EXISTS comments_count INT DEFAULT 0;

-- Create article_likes table
CREATE TABLE IF NOT EXISTS article_likes (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_article_likes_article FOREIGN KEY (article_id) REFERENCES t_article(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_article_like UNIQUE (user_id, article_id)
);

-- Create comments table
CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_article FOREIGN KEY (article_id) REFERENCES t_article(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- Create article_metrics table for Spark job outputs
CREATE TABLE IF NOT EXISTS article_metrics (
    article_id BIGINT NOT NULL REFERENCES t_article(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    likes_count INT NOT NULL DEFAULT 0,
    comments_count INT NOT NULL DEFAULT 0,
    hot_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id, metric_date)
);

-- Create tag tables
CREATE TABLE IF NOT EXISTS t_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_article_tag (
    article_id BIGINT NOT NULL REFERENCES t_article(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES t_tag(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (article_id, tag_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_author_id ON t_article(author_id);
CREATE INDEX IF NOT EXISTS idx_publish_time ON t_article(publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_status ON t_article(status);
CREATE INDEX IF NOT EXISTS idx_article_likes_article_id ON article_likes(article_id);
CREATE INDEX IF NOT EXISTS idx_comments_article_id ON comments(article_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON comments(parent_id);
CREATE INDEX IF NOT EXISTS idx_article_metrics_date ON article_metrics(metric_date DESC);

COMMENT ON COLUMN comments.parent_id IS 'ID of the parent comment if this is a reply';

-- Seed tag data
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

-- Seed articles
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

-- Link tags to articles
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

-- Ensure sequences are aligned
SELECT setval(pg_get_serial_sequence('t_article', 'id'), GREATEST(15, COALESCE((SELECT MAX(id) FROM t_article), 0)), true);
SELECT setval(pg_get_serial_sequence('t_tag', 'id'), GREATEST(8, COALESCE((SELECT MAX(id) FROM t_tag), 0)), true);
SELECT setval(
    pg_get_serial_sequence('article_likes', 'id'),
    COALESCE((SELECT MAX(id) + 1 FROM article_likes), 1),
    false
);
SELECT setval(
    pg_get_serial_sequence('comments', 'id'),
    COALESCE((SELECT MAX(id) + 1 FROM comments), 1),
    false
);

-- Summary
SELECT COUNT(*) AS total_articles FROM t_article;
