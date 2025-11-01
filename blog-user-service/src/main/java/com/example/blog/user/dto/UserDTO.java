package com.example.blog.user.dto;

/**
 * Data Transfer Object for user information exposed to public APIs.
 * This DTO excludes sensitive information such as passwords and internal IDs.
 * Used for returning user profile information to frontend or other services.
 */
public class UserDTO {
    
    /**
     * User's unique identifier.
     */
    private Long id;
    
    /**
     * User's email address.
     */
    private String email;
    
    /**
     * User's username for login.
     */
    private String username;
    
    /**
     * URL to the user's avatar image.
     */
    private String avatarUrl;
    
    /**
     * User's biography or description.
     */
    private String bio;

    // Getters and Setters
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
