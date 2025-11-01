package com.example.blog.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * Notification entity mapping to t_user_notification table that stores
 * user-facing alerts generated when other users interact with their articles.
 */
@TableName("t_user_notification")
public class Notification {

    /** Primary key. */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Recipient user ID. */
    private Long userId;

    /** Actor who triggered the notification (liker or commenter). */
    private Long actorId;

    /** Related article ID (nullable for system notifications). */
    private Long articleId;

    /** Cached article title for quick display. */
    private String articleTitle;

    /** Notification type string, e.g. ARTICLE_LIKE. */
    private String type;

    /** Optional custom content. */
    private String content;

    /** Flag indicating if the notification has been read. */
    @TableField("is_read")
    private Boolean read;

    /** Creation timestamp. */
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
