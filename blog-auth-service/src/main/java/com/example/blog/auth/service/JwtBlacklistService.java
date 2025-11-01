package com.example.blog.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing JWT blacklist using Redis.
 * This service encapsulates all interactions with Redis for blacklist operations,
 * following the single responsibility principle.
 */
@Service
public class JwtBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";
    private final StringRedisTemplate redisTemplate;

    public JwtBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Adds a token's JTI to the blacklist with a TTL equal to the token's remaining validity.
     * This ensures that blacklisted tokens are automatically removed from Redis after expiration.
     *
     * @param jti        The JTI (JWT ID) of the token to blacklist.
     * @param expiration The expiration date of the token.
     */
    public void blacklistToken(String jti, Date expiration) {
        long now = System.currentTimeMillis();
        long ttl = expiration.getTime() - now;
        if (ttl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "blacklisted", ttl, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Checks if a token's JTI is in the blacklist.
     * This method is used during token validation to ensure revoked tokens are rejected.
     *
     * @param jti The JTI (JWT ID) to check.
     * @return true if the token is blacklisted, false otherwise.
     */
    public boolean isTokenBlacklisted(String jti) {
        Boolean exists = redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti);
        return exists != null && exists;
    }
}
