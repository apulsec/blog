package com.example.blog.article.messaging;

import com.example.blog.article.dto.CreateNotificationRequest;

/**
 * Publishes article interaction notifications.
 */
public interface ArticleNotificationPublisher {

    void publish(CreateNotificationRequest request);
}
