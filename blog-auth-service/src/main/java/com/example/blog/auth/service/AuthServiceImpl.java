package com.example.blog.auth.service;

import com.example.blog.auth.client.UserServiceClient;
import com.example.blog.auth.dto.AuthResponse;
import com.example.blog.auth.dto.LoginRequest;
import com.example.blog.auth.dto.TokenRefreshRequest;
import com.example.blog.auth.dto.UserAuthDetailsDTO;
import com.example.blog.auth.security.JwtTokenProvider;
import com.example.blog.auth.service.UserDetailsServiceImpl.CustomUserDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * Implementation of AuthService interface.
 * Contains the core business logic for authentication and authorization operations,
 * including login, logout, token refresh, and token validation.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final JwtBlacklistService blacklistService;
    private final UserServiceClient userServiceClient;

    public AuthServiceImpl(AuthenticationManager authenticationManager, 
                          JwtTokenProvider tokenProvider, 
                          JwtBlacklistService blacklistService,
                          UserServiceClient userServiceClient) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.blacklistService = blacklistService;
        this.userServiceClient = userServiceClient;
    }

    /**
     * Authenticates a user and generates JWT tokens.
     * Uses Spring Security's AuthenticationManager to verify credentials.
     *
     * @param loginRequest The login request containing username and password
     * @return AuthResponse containing access and refresh tokens
     */
    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        // Authenticate the user credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Set the authentication in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Extract username and userId from the authenticated principal
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        Long userId = null;
        
        // Extract userId if available (CustomUserDetails)
        if (userDetails instanceof CustomUserDetails) {
            userId = ((CustomUserDetails) userDetails).getUserId();
        }
        
        // Generate JWT tokens with userId
        String accessToken = tokenProvider.generateAccessToken(username, userId);
        String refreshToken = tokenProvider.generateRefreshToken(username);

        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * Logs out a user by adding their access token to the blacklist.
     * The token's JTI is stored in Redis with a TTL matching the token's remaining validity.
     *
     * @param authHeader The Authorization header containing the Bearer token
     */
    @Override
    public void logout(String authHeader) {
        // Extract token from Authorization header
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            // Validate token before blacklisting
            if (tokenProvider.validateToken(token)) {
                String jti = tokenProvider.getJtiFromToken(token);
                Date expiration = tokenProvider.getExpirationDateFromToken(token);
                
                // Add token to blacklist
                blacklistService.blacklistToken(jti, expiration);
            }
        }
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * Generates both a new access token and a new refresh token.
     *
     * @param refreshRequest The refresh request containing the refresh token
     * @return AuthResponse containing new access and refresh tokens
     * @throws RuntimeException if the refresh token is invalid
     */
    @Override
    public AuthResponse refreshToken(TokenRefreshRequest refreshRequest) {
        String refreshToken = refreshRequest.getRefreshToken();
        
        // Validate the refresh token
        if (tokenProvider.validateToken(refreshToken)) {
            // Extract username from refresh token
            String username = tokenProvider.getUsernameFromToken(refreshToken);
            
            // Determine identity type (email or username)
            String identityType = username.contains("@") ? "email" : "username";
            
            // Fetch user details from user service to get userId
            UserAuthDetailsDTO userAuthDetails = userServiceClient.getUserAuthDetails(identityType, username);
            Long userId = null;
            if (userAuthDetails != null) {
                userId = userAuthDetails.getUserId();
            }
            
            // Generate new tokens with userId
            String newAccessToken = tokenProvider.generateAccessToken(username, userId);
            String newRefreshToken = tokenProvider.generateRefreshToken(username);
            
            return new AuthResponse(newAccessToken, newRefreshToken);
        }
        
        throw new RuntimeException("Invalid refresh token");
    }

    /**
     * Validates a JWT token and returns the username if valid.
     * This method is intended to be called by the API Gateway.
     * It performs both signature validation and blacklist checking.
     *
     * @param token The JWT token to validate
     * @return The username if the token is valid
     * @throws RuntimeException if the token is invalid or blacklisted
     */
    @Override
    public String validateToken(String token) {
        // Validate token signature and expiration
        if (!tokenProvider.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }
        
        // Check if token is blacklisted
        String jti = tokenProvider.getJtiFromToken(token);
        if (blacklistService.isTokenBlacklisted(jti)) {
            throw new RuntimeException("Token is blacklisted");
        }
        
        // Return username from valid token
        return tokenProvider.getUsernameFromToken(token);
    }
}
