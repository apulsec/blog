package com.example.blog.auth.service;

import com.example.blog.auth.client.UserServiceClient;
import com.example.blog.auth.dto.UserAuthDetailsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of UserDetailsService that integrates with blog-user-service.
 * Retrieves user authentication details via Feign client for Spring Security authentication.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    
    private final UserServiceClient userServiceClient;

    public UserDetailsServiceImpl(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    /**
     * Custom UserDetails implementation that includes userId.
     */
    public static class CustomUserDetails extends User {
        private final Long userId;

        public CustomUserDetails(String username, String password, 
                                Collection<? extends GrantedAuthority> authorities, Long userId) {
            super(username, password, authorities);
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }
    }

    /**
     * Loads user details by username (identifier).
     * Calls blog-user-service to retrieve user authentication information.
     *
     * @param username The username (identifier) to look up
     * @return UserDetails object containing user information
     * @throws UsernameNotFoundException if the user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);
        
        try {
            // Determine identity type based on username format
            // Simple heuristic: if contains @, it's email; otherwise username
            String identityType = username.contains("@") ? "email" : "username";
            
            // Call blog-user-service to get user auth details
            UserAuthDetailsDTO authDetails = userServiceClient.getUserAuthDetails(identityType, username);
            
            if (authDetails == null) {
                log.warn("User not found or user service unavailable: {}", username);
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
            
            // Check user status (0=active/normal, 1=disabled, 2=pending, 3=deleted)
            if (authDetails.getStatus() == null || authDetails.getStatus() != 0) {
                log.warn("User account is not active: {} (status: {})", username, authDetails.getStatus());
                throw new UsernameNotFoundException("User account is not active: " + username);
            }
            
            log.debug("Successfully loaded user: {} (userId: {})", authDetails.getUsername(), authDetails.getUserId());
            
            // Build Spring Security UserDetails with userId
            return new CustomUserDetails(
                    authDetails.getIdentifier(),
                    authDetails.getCredential(),
                    new ArrayList<>(),
                    authDetails.getUserId()
            );
                    
        } catch (Exception e) {
            log.error("Error loading user details for username: {}", username, e);
            throw new UsernameNotFoundException("Error loading user: " + username, e);
        }
    }
}
