package com.example.blog.article.client;

import com.example.blog.article.dto.UserDTO;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for UserServiceClient.
 * Provides default user information when blog-user-service is unavailable.
 * 
 * This implements the "Design for Failure" principle by gracefully degrading
 * functionality instead of propagating errors to the user.
 */
@Component
public class UserServiceClientFallback implements UserServiceClient {

    /**
     * Returns a default user object when the actual service call fails.
     *
     * @param userId The ID of the user that was being fetched
     * @return A default UserDTO with "Unknown Author" information
     */
    @Override
    public UserDTO getUserById(Long userId) {
        UserDTO defaultUser = new UserDTO();
        defaultUser.setId(userId);
        defaultUser.setUsername("Unknown Author");
        defaultUser.setAvatarUrl("default-avatar.png");
        return defaultUser;
    }
}
