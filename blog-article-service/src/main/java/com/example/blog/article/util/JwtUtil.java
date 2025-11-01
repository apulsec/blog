package com.example.blog.article.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Utility class for parsing JWT tokens.
 * Used to extract user information from incoming requests.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret-key}")
    private String secretKey;

    /**
     * Extracts user ID from JWT token.
     *
     * @param token JWT token string (without "Bearer " prefix)
     * @return User ID from token claims
     */
    public Long getUserIdFromToken(String token) {
        try {
            log.debug("Parsing JWT token to extract userId");
            // Decode Base64 secret key
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            
            Claims claims = Jwts.parser()
                    .setSigningKey(keyBytes)
                    .parseClaimsJws(token)
                    .getBody();
            
            // The userId is stored in the token claims
            Object userIdObj = claims.get("userId");
            log.debug("Extracted userId from token: {}", userIdObj);
            
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            } else if (userIdObj instanceof String) {
                return Long.parseLong((String) userIdObj);
            }
            
            return null;
        } catch (Exception e) {
            // Token parsing failed
            log.error("Failed to parse JWT token: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts username from JWT token.
     *
     * @param token JWT token string
     * @return Username from token subject
     */
    public String getUsernameFromToken(String token) {
        try {
            // Decode Base64 secret key
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            
            Claims claims = Jwts.parser()
                    .setSigningKey(keyBytes)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
