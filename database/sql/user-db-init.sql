-- ------------------------------------------------------------
-- Blog User Service - PostgreSQL bootstrap script
-- Purpose: recreate blog_user_db schema with seed accounts
-- ------------------------------------------------------------

-- Create database if missing
SELECT 'CREATE DATABASE blog_user_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'blog_user_db')\gexec

-- Switch connection
\c blog_user_db

SET client_min_messages TO warning;

-- Drop existing tables to keep initialization idempotent
DROP TABLE IF EXISTS t_user_notification CASCADE;
DROP TABLE IF EXISTS t_user_auth CASCADE;
DROP TABLE IF EXISTS t_user CASCADE;

-- Create user profile table
CREATE TABLE IF NOT EXISTS t_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    bio VARCHAR(500),
    status INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user auth table
CREATE TABLE IF NOT EXISTS t_user_auth (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES t_user(id) ON DELETE CASCADE,
    identity_type VARCHAR(20) NOT NULL,
    identifier VARCHAR(100) NOT NULL,
    credential VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (identity_type, identifier)
);

-- Create notification table
CREATE TABLE IF NOT EXISTS t_user_notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    actor_id BIGINT,
    article_id BIGINT,
    article_title VARCHAR(255),
    type VARCHAR(50),
    content TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_user_auth_user_id ON t_user_auth(user_id);
CREATE INDEX IF NOT EXISTS idx_user_auth_identifier ON t_user_auth(identifier);
CREATE INDEX IF NOT EXISTS idx_user_notification_user_id ON t_user_notification(user_id);

-- Seed primary authors
INSERT INTO t_user (id, email, avatar_url, bio, status)
SELECT 1, 'author1@example.com', 'https://picsum.photos/seed/author1/200/200', '云原生领域的技术布道者', 0
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 1);

INSERT INTO t_user (id, email, avatar_url, bio, status)
SELECT 2, 'author2@example.com', 'https://picsum.photos/seed/author2/200/200', '前端框架爱好者与开源贡献者', 0
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 2);

INSERT INTO t_user (id, email, avatar_url, bio, status)
SELECT 3, 'author3@example.com', 'https://picsum.photos/seed/author3/200/200', '后端架构与数据库调优实践者', 0
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 3);

-- Seed username credentials (bcrypt placeholder hash)
INSERT INTO t_user_auth (id, user_id, identity_type, identifier, credential)
SELECT 1, 1, 'username', 'cloud_architect', '$2a$10$Y2JxcnlwdGhhc2hIYXNocGFkLkhlbGxvUHJvamVjdA9O1EfnKvHRtS'
WHERE NOT EXISTS (
    SELECT 1 FROM t_user_auth WHERE identity_type = 'username' AND identifier = 'cloud_architect'
);

INSERT INTO t_user_auth (id, user_id, identity_type, identifier, credential)
SELECT 2, 2, 'username', 'frontend_guru', '$2a$10$Y2JxcnlwdGhhc2hIYXNocGFkLkhlbGxvUHJvamVjdA9O1EfnKvHRtS'
WHERE NOT EXISTS (
    SELECT 1 FROM t_user_auth WHERE identity_type = 'username' AND identifier = 'frontend_guru'
);

INSERT INTO t_user_auth (id, user_id, identity_type, identifier, credential)
SELECT 3, 3, 'username', 'data_maven', '$2a$10$Y2JxcnlwdGhhc2hIYXNocGFkLkhlbGxvUHJvamVjdA9O1EfnKvHRtS'
WHERE NOT EXISTS (
    SELECT 1 FROM t_user_auth WHERE identity_type = 'username' AND identifier = 'data_maven'
);

-- Align sequences to prevent collisions
SELECT setval(
    pg_get_serial_sequence('t_user', 'id'),
    COALESCE((SELECT MAX(id) + 1 FROM t_user), 1),
    false
);
SELECT setval(
    pg_get_serial_sequence('t_user_auth', 'id'),
    COALESCE((SELECT MAX(id) + 1 FROM t_user_auth), 1),
    false
);
SELECT setval(
    pg_get_serial_sequence('t_user_notification', 'id'),
    COALESCE((SELECT MAX(id) + 1 FROM t_user_notification), 1),
    false
);

-- Verification helper
SELECT COUNT(*) AS total_users FROM t_user;
