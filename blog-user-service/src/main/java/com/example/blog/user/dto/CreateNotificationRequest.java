package com.example.blog.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a notification for a user.
 * This endpoint is designed for internal service-to-service communication.
 */
public class CreateNotificationRequest {

    /** Recipient user ID (article author). */
    @NotNull(message = "userId is required")
    private Long userId;

    /** Actor user ID who triggered the notification (liker or commenter). */
    @NotNull(message = "actorId is required")
    private Long actorId;

    /** Related article ID. */
    @NotNull(message = "articleId is required")
    private Long articleId;

    /** Article title for quick reference in notifications. */
    @Size(max = 255, message = "articleTitle length must be <= 255 characters")
    private String articleTitle;

    /** Notification type, e.g. ARTICLE_LIKE or ARTICLE_COMMENT. */
    @NotBlank(message = "type is required")
    @Size(max = 50, message = "type length must be <= 50 characters")
    private String type;

    /**
     * Optional content message. If omitted, the service will generate
     * a localized default message based on the notification type.
     */
    @Size(max = 2000, message = "content length must be <= 2000 characters")
    private String content;

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
}
