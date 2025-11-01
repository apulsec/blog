package com.example.blog.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for the User Service.
 * 
 * Primary responsibilities:
 * 1. Provide a PasswordEncoder bean for secure password hashing (BCrypt)
 * 2. Configure HTTP security to allow internal service access
 * 
 * NOTE: This service does not perform authentication itself.
 * Authentication is handled by the API Gateway and blog-auth-service.
 * This configuration assumes the service is deployed in a trusted internal network
 * or behind an API gateway that handles authentication/authorization.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Provides a BCryptPasswordEncoder for secure password hashing.
     * BCrypt is a one-way hashing algorithm specifically designed for passwords.
     * It includes a salt to prevent rainbow table attacks and has an adjustable work factor.
     * 
     * @return PasswordEncoder instance for password encryption
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the HTTP security filter chain.
     * Since this is an internal microservice, we permit all requests to user endpoints.
     * 
     * IMPORTANT: In production, this service should be protected by:
     * - Network isolation (only accessible within the service mesh)
     * - API Gateway authentication
     * - Service-to-service authentication (e.g., mutual TLS)
     * 
     * @param http HttpSecurity configuration object
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for REST APIs
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless REST API
            .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/users/**").permitAll()
            .requestMatchers("/api/notifications/**").permitAll()
            .requestMatchers("/uploads/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().authenticated()
            );
        return http.build();
    }

    /**
     * Configure CORS to allow requests from the frontend.
     * 
     * @return CorsConfigurationSource with allowed origins, methods, and headers
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
