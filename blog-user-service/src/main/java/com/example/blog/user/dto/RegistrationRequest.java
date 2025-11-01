package com.example.blog.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user registration requests.
 * Contains all necessary information to create a new user account.
 * Validation annotations ensure data integrity before processing.
 */
public class RegistrationRequest {
    
    /**
     * Username for login.
     */
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * Email address (optional but recommended).
     */
    @Email(message = "Email must be valid")
    private String email;

    /**
     * User's password (plain text, will be hashed before storage).
     * Must be at least 6 characters long.
     */
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    // Getters and Setters
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
