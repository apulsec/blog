package com.example.blog.user.config;

import java.util.Objects;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for consuming article interaction notifications.
 */
@EnableRabbit
@Configuration
public class RabbitMQConfig {

    public static final String ARTICLE_NOTIFICATION_EXCHANGE = "blog.article.notifications";
    public static final String ARTICLE_NOTIFICATION_QUEUE = "blog.notifications.article-interactions";
    public static final String ARTICLE_NOTIFICATION_ROUTING_KEY = "article.interaction";

    @Bean
    public TopicExchange articleNotificationExchange() {
        return new TopicExchange(ARTICLE_NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue articleNotificationQueue() {
        return QueueBuilder.durable(ARTICLE_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding articleNotificationBinding(Queue articleNotificationQueue, TopicExchange articleNotificationExchange) {
        return BindingBuilder.bind(articleNotificationQueue)
                .to(articleNotificationExchange)
                .with(ARTICLE_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(Objects.requireNonNull(connectionFactory));
        template.setMessageConverter(Objects.requireNonNull(jacksonMessageConverter));
        return template;
    }
}
