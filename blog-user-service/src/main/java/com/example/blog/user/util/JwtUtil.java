package com.example.blog.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Utility for extracting user information from JWT tokens.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret-key}")
    private String secretKey;

    /**
     * Parses the JWT token and returns the user ID claim.
     *
     * @param token JWT token without the Bearer prefix
     * @return user ID if present; otherwise null
     */
    public Long getUserIdFromToken(String token) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            Claims claims = Jwts.parser()
                    .setSigningKey(keyBytes)
                    .parseClaimsJws(token)
                    .getBody();

            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            if (userId instanceof Long) {
                return (Long) userId;
            }
            if (userId instanceof String) {
                return Long.parseLong((String) userId);
            }
            return null;
        } catch (Exception ex) {
            log.warn("Failed to parse JWT token: {}", ex.getMessage());
            return null;
        }
    }
}
