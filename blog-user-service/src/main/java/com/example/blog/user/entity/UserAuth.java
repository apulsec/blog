package com.example.blog.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * UserAuth entity representing authentication credentials stored in PostgreSQL.
 * Maps to the t_user_auth table.
 * 
 * This table stores sensitive authentication information separately from the main User table.
 * It supports multiple authentication methods (email, username, phone, OAuth, etc.) for the same user.
 * Passwords are stored using BCrypt hashing for security.
 */
@TableName("t_user_auth")
public class UserAuth {
    
    /**
     * Unique identifier for this authentication record (primary key, auto-increment).
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * Foreign key reference to the User entity.
     */
    private Long userId;
    
    /**
     * Type of authentication identity.
     * Examples: "email", "username", "phone", "wechat", "google"
     */
    private String identityType;
    
    /**
     * The unique identifier for this authentication method.
     * Examples: user's email address, username, phone number, or OAuth ID
     */
    private String identifier;
    
    /**
     * The credential (typically a hashed password for local authentication).
     * For password-based auth, this field stores the BCrypt-hashed password.
     * For OAuth, this might store tokens or be left null.
     */
    private String credential;
    
    /**
     * Timestamp when this authentication record was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when this authentication record was last updated.
     */
    private LocalDateTime updatedAt;

    // Getters and Setters
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
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
