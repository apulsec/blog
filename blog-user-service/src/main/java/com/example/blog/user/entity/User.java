package com.example.blog.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * User entity representing the core user profile information stored in PostgreSQL.
 * Maps to the t_user table.
 * 
 * This table stores user metadata such as email, avatar, bio, and account status.
 * Authentication credentials are stored separately in the UserAuth entity for security.
 */
@TableName("t_user")
public class User {
    
    /**
     * Unique identifier for the user (primary key, auto-increment).
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * User's email address (optional, for contact purposes).
     */
    private String email;
    
    /**
     * URL to the user's avatar image.
     */
    private String avatarUrl;
    
    /**
     * User's biography or description.
     */
    private String bio;
    
    /**
     * Account status:
     * 0: Normal (active account)
     * 1: Disabled (account temporarily disabled by admin)
     * 2: Pending activation (awaiting email confirmation)
     * 3: Deactivated (user requested account deletion)
     */
    private Integer status;
    
    /**
     * Timestamp when the user account was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the user account was last updated.
     */
    private LocalDateTime updatedAt;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
