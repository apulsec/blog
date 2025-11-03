package com.example.blog.user.messaging;

import com.example.blog.user.config.RabbitMQConfig;
import com.example.blog.user.dto.CreateNotificationRequest;
import com.example.blog.user.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes article interaction notifications and persists them as user notifications.
 */
@Component
public class ArticleNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ArticleNotificationListener.class);

    private final NotificationService notificationService;

    public ArticleNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.ARTICLE_NOTIFICATION_QUEUE)
    public void handleArticleNotification(@Payload CreateNotificationRequest request) {
        if (request == null) {
            log.warn("Received null article interaction notification message, skipping");
            return;
        }

        try {
            notificationService.createNotification(request);
        } catch (Exception ex) {
            log.error("Failed to process article interaction notification for articleId={} and userId={}",
                    request.getArticleId(), request.getUserId(), ex);
        }
    }
}
