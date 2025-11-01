package com.example.blog.article.client;

import com.example.blog.article.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for calling blog-user-service.
 * Enables declarative REST API calls to fetch user information.
 * 
 * The fallback mechanism ensures resilience when the user service is unavailable.
 * 
 * Note: When Eureka is disabled, this client uses a static URL. 
 * The fallback will be triggered if the service is not available.
 */
@FeignClient(
    name = "blog-user-service", 
    url = "${user-service.url:http://localhost:8081}",
    fallbackFactory = UserServiceClientFallbackFactory.class
)
public interface UserServiceClient {

    /**
     * Fetches user information by user ID.
     *
     * @param userId The ID of the user to fetch
     * @return UserDTO containing user information
     */
    @GetMapping("/api/users/{userId}")
    UserDTO getUserById(@PathVariable("userId") Long userId);
}
