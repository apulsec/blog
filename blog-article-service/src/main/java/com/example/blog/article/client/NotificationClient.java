package com.example.blog.article.client;

import com.example.blog.article.dto.CreateNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client used to create notifications via blog-user-service.
 *
 * Eureka discovery is used by default. When running without Eureka, set user-service.url to the
 * target REST endpoint so notifications can still be delivered.
 */
@FeignClient(
    name = "blog-user-service",
    contextId = "notificationClient",
    url = "${user-service.url:}",
    fallbackFactory = NotificationClientFallbackFactory.class
)
public interface NotificationClient {

    /**
     * Sends a notification creation request to blog-user-service.
     */
    @PostMapping("/api/notifications/internal")
    void createNotification(@RequestBody CreateNotificationRequest request);
}
