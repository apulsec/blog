package com.example.blog.article.client;

import com.example.blog.article.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * FallbackFactory for UserServiceClient.
 * Provides default user information when blog-user-service is unavailable.
 * 
 * Using FallbackFactory instead of Fallback allows access to the exception cause.
 */
@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClientFallbackFactory.class);

    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {
            @Override
            public UserDTO getUserById(Long userId) {
                log.warn("Fallback triggered for getUserById({}). Reason: {}", 
                         userId, cause.getMessage());
                
                UserDTO defaultUser = new UserDTO();
                defaultUser.setId(userId);
                defaultUser.setUsername("Unknown Author");
                defaultUser.setAvatarUrl("default-avatar.png");
                return defaultUser;
            }
        };
    }
}
