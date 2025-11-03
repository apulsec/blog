package com.example.blog.article.messaging;

import com.example.blog.article.config.RabbitMQConfig;
import com.example.blog.article.dto.CreateNotificationRequest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ backed implementation of {@link ArticleNotificationPublisher}.
 */
@Component
public class RabbitArticleNotificationPublisher implements ArticleNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitArticleNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitArticleNotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate);
    }

    @Override
    public void publish(CreateNotificationRequest request) {
        Objects.requireNonNull(request, "notification request must not be null");

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ARTICLE_NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.ARTICLE_NOTIFICATION_ROUTING_KEY,
                    request
            );
        } catch (AmqpException ex) {
            log.warn("Failed to publish article interaction notification message", ex);
        }
    }
}
