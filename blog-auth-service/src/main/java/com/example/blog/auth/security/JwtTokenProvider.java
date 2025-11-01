package com.example.blog.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token Provider component.
 * Responsible for generating, parsing, and validating JSON Web Tokens (JWT).
 * This class is thread-safe and acts as the single entry point for all JWT operations.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret-key}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey key;

    /**
     * Initializes the secret key after dependency injection.
     * Converts the Base64-encoded secret from configuration into a SecretKey object.
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates an access token for the given username and userId.
     * The token includes a unique JTI (JWT ID) claim for blacklist functionality
     * and a userId claim for authorization in other services.
     *
     * @param username The username to encode in the token
     * @param userId The user ID to encode in the token
     * @return The generated JWT access token
     */
    public String generateAccessToken(String username, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("jti", UUID.randomUUID().toString())
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Generates a refresh token for the given username.
     * Refresh tokens have a longer expiration time than access tokens.
     *
     * @param username The username to encode in the token
     * @return The generated JWT refresh token
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extracts the username (subject) from a valid JWT token.
     *
     * @param token The JWT token to parse
     * @return The username encoded in the token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Extracts the JTI (JWT ID) claim from a valid JWT token.
     * The JTI is used as a unique identifier for blacklist functionality.
     *
     * @param token The JWT token to parse
     * @return The JTI claim value
     */
    public String getJtiFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("jti", String.class);
    }

    /**
     * Extracts the expiration date from a valid JWT token.
     *
     * @param token The JWT token to parse
     * @return The expiration date of the token
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getExpiration();
    }

    /**
     * Validates the given JWT token.
     * Checks the signature, format, and expiration of the token.
     * All validation exceptions are caught and logged appropriately.
     *
     * @param authToken The JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
        }
        return false;
    }
}
