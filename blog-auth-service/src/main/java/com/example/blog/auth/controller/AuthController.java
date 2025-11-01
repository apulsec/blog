package com.example.blog.auth.controller;

import com.example.blog.auth.dto.AuthResponse;
import com.example.blog.auth.dto.LoginRequest;
import com.example.blog.auth.dto.TokenRefreshRequest;
import com.example.blog.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication and authorization endpoints.
 * Exposes API endpoints for login, logout, token refresh, and token validation.
 * All endpoints are under the /api/auth base path.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user and returns JWTs.
     * This endpoint validates user credentials and generates access and refresh tokens.
     *
     * @param loginRequest The login request containing username and password.
     * @return A response entity with the authentication response containing tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Logs out a user by blacklisting the current access token.
     * The token is extracted from the Authorization header and added to the blacklist.
     *
     * @param authHeader The Authorization header containing the Bearer token.
     * @return A response entity indicating successful logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok("User logged out successfully");
    }

    /**
     * Refreshes an access token using a refresh token.
     * Generates a new set of access and refresh tokens if the provided refresh token is valid.
     *
     * @param refreshRequest The request containing the refresh token.
     * @return A response entity with the new set of tokens.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest refreshRequest) {
        AuthResponse authResponse = authService.refreshToken(refreshRequest);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Validates a JWT. This is an internal endpoint intended to be called by the API Gateway.
     * Performs signature validation and blacklist checking.
     *
     * @param token The JWT to validate.
     * @return A response entity with the username if the token is valid, or an error message with 401 status.
     */
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestParam("token") String token) {
        try {
            String username = authService.validateToken(token);
            return ResponseEntity.ok(username);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
