package com.example.blog.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for token refresh requests.
 * Encapsulates the refresh token for obtaining a new access token.
 */
public class TokenRefreshRequest {

    /**
     * The refresh token to be used for generating a new access token.
     */
    @NotBlank(message = "Refresh token cannot be blank")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
