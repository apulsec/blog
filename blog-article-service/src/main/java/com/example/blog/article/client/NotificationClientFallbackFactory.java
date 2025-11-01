package com.example.blog.article.client;

import com.example.blog.article.dto.CreateNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback factory for NotificationClient to log failures without impacting
 * the article interaction flow.
 */
@Component
public class NotificationClientFallbackFactory implements FallbackFactory<NotificationClient> {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallbackFactory.class);

    @Override
    public NotificationClient create(Throwable cause) {
        return request -> log.warn("Failed to send notification to user-service for article {}: {}",
                request != null ? request.getArticleId() : null,
                cause != null ? cause.getMessage() : "unknown error");
    }
}
