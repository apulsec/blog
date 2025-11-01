package com.example.blog.auth.client;

import com.example.blog.auth.dto.UserAuthDetailsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for UserServiceClient.
 * Provides graceful degradation when blog-user-service is unavailable.
 */
@Component
public class UserServiceClientFallback implements UserServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceClientFallback.class);
    
    @Override
    public UserAuthDetailsDTO getUserAuthDetails(String identityType, String identifier) {
        log.error("Failed to get user auth details for identityType: {}, identifier: {}. User service may be down.", 
                  identityType, identifier);
        return null;
    }
}
