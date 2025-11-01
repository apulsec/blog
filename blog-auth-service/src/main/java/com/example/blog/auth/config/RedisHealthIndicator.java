package com.example.blog.auth.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Health Indicator
 * Checks if Redis is available and can be accessed.
 * This helps identify Redis connection issues early.
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redisTemplate;

    public RedisHealthIndicator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            // Try to ping Redis
            String response = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            
            if ("PONG".equalsIgnoreCase(response)) {
                return Health.up()
                        .withDetail("redis", "Available")
                        .withDetail("message", "Redis connection is healthy")
                        .build();
            } else {
                return Health.down()
                        .withDetail("redis", "Unavailable")
                        .withDetail("message", "Redis ping returned unexpected response: " + response)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "Unavailable")
                    .withDetail("error", e.getMessage())
                    .withDetail("message", "Cannot connect to Redis. Logout and token validation will fail!")
                    .build();
        }
    }
}
