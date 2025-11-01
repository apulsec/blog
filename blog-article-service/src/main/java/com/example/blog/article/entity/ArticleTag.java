package com.example.blog.article.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * ArticleTag entity representing the many-to-many relationship
 * between articles and tags stored in PostgreSQL.
 * Maps to the t_article_tag table.
 */
@TableName("t_article_tag")
public class ArticleTag {
    
    private Long articleId;
    private Long tagId;
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
