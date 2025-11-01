package com.example.blog.user.dto;

/**
 * Data Transfer Object for user authentication details.
 * This DTO is used exclusively for internal service-to-service communication,
 * particularly for the blog-auth-service to validate user credentials during login.
 * 
 * SECURITY NOTE: This DTO contains sensitive information (hashed password)
 * and should NEVER be exposed to public APIs or frontend clients.
 */
public class UserAuthDetailsDTO {
    
    /**
     * User's unique identifier.
     */
    private Long userId;
    
    /**
     * The identifier used for authentication (e.g., email or username).
     */
    private String identifier;
    
    /**
     * The hashed password credential (BCrypt hash).
     * This is used by auth-service to verify the user's login attempt.
     */
    private String credential;
    
    /**
     * User's username (for inclusion in JWT claims).
     */
    private String username;
    
    /**
     * Account status (0: Normal, 1: Disabled, 2: Pending, 3: Deactivated).
     * Auth service checks this to prevent login for disabled accounts.
     */
    private Integer status;

    // Constructors
    
    public UserAuthDetailsDTO() {
    }

    public UserAuthDetailsDTO(Long userId, String identifier, String credential, String username, Integer status) {
        this.userId = userId;
        this.identifier = identifier;
        this.credential = credential;
        this.username = username;
        this.status = status;
    }

    // Getters and Setters
    
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
