package com.example.blog.auth.dto;

/**
 * Data Transfer Object for authentication responses.
 * Returns JWT tokens after successful authentication.
 */
public class AuthResponse {

    /**
     * The access token (JWT) with short expiration time.
     */
    private String accessToken;

    /**
     * The refresh token (JWT) with longer expiration time.
     */
    private String refreshToken;

    /**
     * The type of token, always "Bearer" for JWT.
     */
    private String tokenType = "Bearer";

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String refreshToken, String tokenType) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
    }

    /**
     * Constructor that accepts only tokens and sets the default token type.
     *
     * @param accessToken  The access token
     * @param refreshToken The refresh token
     */
    public AuthResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
