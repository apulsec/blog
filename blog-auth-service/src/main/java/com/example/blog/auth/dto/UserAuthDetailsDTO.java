package com.example.blog.auth.dto;

/**
 * DTO for user authentication details received from blog-user-service.
 * Contains the necessary information for Spring Security authentication.
 */
public class UserAuthDetailsDTO {
    private Long userId;
    private String identifier;
    private String credential;
    private String username;
    private Integer status;

    public UserAuthDetailsDTO() {
    }

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
