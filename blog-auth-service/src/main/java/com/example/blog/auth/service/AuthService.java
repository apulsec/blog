package com.example.blog.auth.service;

import com.example.blog.auth.dto.AuthResponse;
import com.example.blog.auth.dto.LoginRequest;
import com.example.blog.auth.dto.TokenRefreshRequest;

/**
 * Interface defining the core authentication and authorization business logic.
 * This interface provides methods for login, logout, token refresh, and token validation.
 */
public interface AuthService {
    
    /**
     * Authenticates a user and returns JWT tokens.
     *
     * @param loginRequest The login request containing username and password
     * @return AuthResponse containing access and refresh tokens
     */
    AuthResponse login(LoginRequest loginRequest);
    
    /**
     * Logs out a user by blacklisting their current access token.
     *
     * @param authHeader The Authorization header containing the Bearer token
     */
    void logout(String authHeader);
    
    /**
     * Refreshes an access token using a valid refresh token.
     *
     * @param refreshRequest The refresh request containing the refresh token
     * @return AuthResponse containing new access and refresh tokens
     */
    AuthResponse refreshToken(TokenRefreshRequest refreshRequest);
    
    /**
     * Validates a JWT token and returns the username if valid.
     * This is an internal endpoint intended for the API Gateway.
     *
     * @param token The JWT token to validate
     * @return The username if the token is valid
     * @throws RuntimeException if the token is invalid or blacklisted
     */
    String validateToken(String token);
}
