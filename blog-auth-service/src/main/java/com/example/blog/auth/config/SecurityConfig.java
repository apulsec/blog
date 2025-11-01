package com.example.blog.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
 * Spring Security configuration class.
 * Defines the security policy, authentication manager, and password encoder.
 * This service is the JWT producer, so authentication endpoints must be public.
 * JWT validation will be performed by the API Gateway or other microservices.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Defines the password encoder bean.
     * Uses BCrypt hashing algorithm for secure password storage.
     *
     * @return The password encoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines the authentication manager bean.
     * This is the core component for processing authentication requests.
     *
     * @param authenticationConfiguration The authentication configuration
     * @return The authentication manager instance
     * @throws Exception if unable to get the authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Defines the security filter chain.
     * Configures CSRF protection, session management, and request authorization rules.
     *
     * @param http The HttpSecurity to configure
     * @return The configured SecurityFilterChain
     * @throws Exception if unable to build the filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF since the API is stateless and uses JWT authentication
                .csrf(AbstractHttpConfigurer::disable)
                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Set session management to stateless (no server-side sessions)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Allow anonymous access to all authentication endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        // Allow anonymous access to actuator endpoints for health checks
                        .requestMatchers("/actuator/**").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) to allow frontend access.
     *
     * @return The CORS configuration source
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
