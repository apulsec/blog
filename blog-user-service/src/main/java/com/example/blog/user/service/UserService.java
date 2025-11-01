package com.example.blog.user.service;

import com.example.blog.user.dto.RegistrationRequest;
import com.example.blog.user.dto.UserAuthDetailsDTO;
import com.example.blog.user.dto.UserDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface defining the core business logic for user management.
 * 
 * This service is responsible for:
 * - User registration with secure password storage
 * - Retrieving user profile information
 * - Providing authentication details for internal services
 */
public interface UserService {
    
    /**
    * Registers a new user account.
    * 
    * This method performs the following operations:
    * 1. Validates that the identifier (email/username) is unique for the given identity type
    * 2. Creates a new User record with basic profile information
    * 3. Creates a UserAuth record with the encrypted password
    * 
    * @param registrationRequest Contains username, password, and optional email information
    * @return UserDTO containing the newly created user's public information
    * @throws IllegalStateException if the identifier already exists
     */
    UserDTO registerUser(RegistrationRequest registrationRequest);
    
    /**
     * Retrieves a user's public profile information by their user ID.
     * 
     * @param userId The unique identifier of the user
     * @return UserDTO containing the user's public profile data
     * @throws RuntimeException if user is not found
     */
    UserDTO getUserById(Long userId);
    
    /**
     * Retrieves authentication details for credential validation.
     * 
     * This method is used by the blog-auth-service to validate user credentials during login.
     * It returns the hashed password and account status, which the auth service uses to:
     * 1. Verify the provided password against the stored hash
     * 2. Check if the account is active and not disabled
     * 
     * SECURITY: This method should only be called by internal services (not exposed publicly).
     * 
     * @param identityType The type of identifier (e.g., "email", "username")
     * @param identifier The unique identifier value (e.g., "user@example.com")
     * @return UserAuthDetailsDTO containing user ID, credential hash, and account status, or null if not found
     */
    UserAuthDetailsDTO findUserAuthDetailsByIdentifier(String identityType, String identifier);

    /**
     * Updates the avatar for the specified user.
     *
     * @param userId ID of the user performing the update
     * @param file   avatar image file
     * @return updated user DTO
     */
    UserDTO updateAvatar(Long userId, MultipartFile file);

    /**
     * Updates the username for the specified user.
     *
     * @param userId      ID of the user performing the update
     * @param newUsername desired new username
     * @return updated user DTO
     */
    UserDTO updateUsername(Long userId, String newUsername);
}
