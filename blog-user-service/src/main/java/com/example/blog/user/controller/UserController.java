package com.example.blog.user.controller;

import com.example.blog.user.dto.RegistrationRequest;
import com.example.blog.user.dto.UpdateUsernameRequest;
import com.example.blog.user.dto.UserAuthDetailsDTO;
import com.example.blog.user.dto.UserDTO;
import com.example.blog.user.service.UserService;
import com.example.blog.user.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST Controller for user management endpoints.
 * 
 * Provides three types of endpoints:
 * 1. Public registration endpoint for new users
 * 2. Public user profile retrieval endpoint
 * 3. Internal endpoint for authentication credential validation (used by blog-auth-service)
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * Constructor-based dependency injection.
     * 
     * @param userService Service layer for user business logic
     */
    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers a new user account.
     * 
     * Endpoint: POST /api/users/register
     * 
     * Request body validation ensures:
     * - Password is at least 6 characters
     * - All required fields are present
     * 
    * @param registrationRequest Contains username, password, and optional email
    * @return ResponseEntity with UserDTO and HTTP 201 (Created) status
    * @throws IllegalStateException if identifier already exists (returns HTTP 400)
     */
    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@Valid @RequestBody RegistrationRequest registrationRequest) {
        UserDTO newUser = userService.registerUser(registrationRequest);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    /**
     * Retrieves a user's public profile information by user ID.
     * 
     * Endpoint: GET /api/users/{userId}
     * 
     * This endpoint is used by:
     * - Frontend to display user profiles
     * - blog-article-service to get author information for articles
     * 
     * @param userId The unique identifier of the user
     * @return ResponseEntity with UserDTO and HTTP 200 (OK) status
     * @throws RuntimeException if user not found (returns HTTP 404)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        UserDTO user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Internal endpoint for authentication credential validation.
     * 
     * Endpoint: GET /api/users/internal/auth-details
     * 
     * SECURITY CRITICAL: This endpoint returns sensitive information (hashed password)
     * and should ONLY be accessible to internal services (blog-auth-service).
     * 
     * In production, this endpoint should be protected by:
     * - Network isolation (service mesh, internal VPC)
     * - Service-to-service authentication
     * - API Gateway restrictions
     * 
     * Used by blog-auth-service during login to:
     * 1. Retrieve the hashed password for verification
     * 2. Check account status (active, disabled, etc.)
     * 3. Get user metadata for JWT token generation
     * 
     * @param identityType Type of identifier (e.g., "email", "username")
     * @param identifier The unique identifier value (e.g., "user@example.com")
     * @return ResponseEntity with UserAuthDetailsDTO if found, HTTP 404 if not found
     */
    @GetMapping("/internal/auth-details")
    public ResponseEntity<UserAuthDetailsDTO> getUserAuthDetails(
            @RequestParam String identityType,
            @RequestParam String identifier) {
        UserAuthDetailsDTO authDetails = userService.findUserAuthDetailsByIdentifier(identityType, identifier);
        if (authDetails == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(authDetails);
    }

    /**
     * Updates the authenticated user's avatar.
     *
     * Endpoint: POST /api/users/me/avatar
     *
     * @param file                 uploaded image file
     * @param authorizationHeader  JWT bearer token header
     * @return updated user profile information
     */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDTO> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        Long userId = resolveUserId(authorizationHeader);
        try {
            UserDTO updatedUser = userService.updateAvatar(userId, file);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Updates the authenticated user's username.
     *
     * Endpoint: PUT /api/users/me/username
     *
     * @param request update payload containing the new username
     * @param authorizationHeader JWT bearer token header
     * @return updated user profile information
     */
    @PutMapping("/me/username")
    public ResponseEntity<UserDTO> updateUsername(
            @Valid @RequestBody UpdateUsernameRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        Long userId = resolveUserId(authorizationHeader);
        UserDTO updatedUser = userService.updateUsername(userId, request.getUsername());
        return ResponseEntity.ok(updatedUser);
    }

    private Long resolveUserId(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return userId;
    }
}
