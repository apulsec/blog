package com.example.blog.article.dto;

/**
 * Data Transfer Object for user information.
 * Used to receive user data from blog-user-service via Feign client.
 */
public class UserDTO {
    private Long id;
    private String username;
    private String avatarUrl;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
