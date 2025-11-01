-- Create article_likes table to store user likes for articles
CREATE TABLE IF NOT EXISTS article_likes (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_article_likes_article FOREIGN KEY (article_id) REFERENCES t_article(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_article_like UNIQUE (user_id, article_id)
);

-- Create comments table for articles
CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT, -- For nested comments (replies)
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_article FOREIGN KEY (article_id) REFERENCES t_article(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_article_likes_article_id ON article_likes(article_id);
CREATE INDEX IF NOT EXISTS idx_comments_article_id ON comments(article_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON comments(parent_id);

COMMENT ON COLUMN comments.parent_id IS 'ID of the parent comment if this is a reply';
