package com.example.blog.auth.client;

import com.example.blog.auth.dto.UserAuthDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for calling blog-user-service.
 * Provides methods to retrieve user authentication details.
 * 
 * The URL can be configured via application.yml using:
 * - Eureka discovery by default (blank URL)
 * - Direct URL override via user-service.url when service discovery is disabled
 */
@FeignClient(
    name = "blog-user-service",
    url = "${user-service.url:}",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {
    
    /**
     * Retrieves user authentication details by identifier (email/username/phone).
     * This endpoint is used during login to verify user credentials.
     * 
     * @param identityType The type of identity (email, username, phone)
     * @param identifier The user's identifier (email, username, or phone)
     * @return UserAuthDetailsDTO containing authentication information
     */
    @GetMapping("/api/users/internal/auth-details")
    UserAuthDetailsDTO getUserAuthDetails(
            @RequestParam("identityType") String identityType,
            @RequestParam("identifier") String identifier
    );
}
