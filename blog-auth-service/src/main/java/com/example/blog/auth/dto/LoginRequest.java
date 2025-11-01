package com.example.blog.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for user login requests.
 * Encapsulates user credentials for authentication.
 */
public class LoginRequest {

    /**
     * The username provided by the user.
     */
    @NotBlank(message = "Username cannot be blank")
    private String username;

    /**
     * The password provided by the user.
     */
    @NotBlank(message = "Password cannot be blank")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
