package com.example.blog.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.blog.user.dto.RegistrationRequest;
import com.example.blog.user.dto.UserAuthDetailsDTO;
import com.example.blog.user.dto.UserDTO;
import com.example.blog.user.entity.User;
import com.example.blog.user.entity.UserAuth;
import com.example.blog.user.mapper.UserAuthMapper;
import com.example.blog.user.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

/**
 * Implementation of the UserService interface.
 * Handles all user management business logic including registration, profile retrieval,
 * and authentication credential management.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.avatar.upload-dir:uploads/avatars}")
    private String avatarUploadDir;

    private Path avatarStoragePath;

    private static final String AVATAR_URL_PREFIX = "/uploads/avatars/";

    /**
     * Constructor-based dependency injection.
     * 
     * @param userMapper MyBatis-Plus mapper for User entity
     * @param userAuthMapper MyBatis-Plus mapper for UserAuth entity
     * @param passwordEncoder BCrypt password encoder from SecurityConfig
     */
    public UserServiceImpl(UserMapper userMapper, UserAuthMapper userAuthMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userAuthMapper = userAuthMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void initAvatarStorage() {
        avatarStoragePath = Paths.get(avatarUploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(avatarStoragePath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize avatar upload directory", e);
        }
    }

    /**
     * Registers a new user with secure password storage.
     * 
     * Process:
    * 1. Validate username uniqueness
    * 2. Validate identifier uniqueness for the given identity type
     * 3. Create User entity with profile information
     * 4. Create UserAuth entity with BCrypt-hashed password
     * 
     * The transaction annotation ensures both records are created atomically.
     * If either operation fails, both are rolled back.
     * 
     * @param request Registration details from the client
     * @return UserDTO with the newly created user's public information
    * @throws IllegalStateException if username or identifier already exists
     */
    @Override
    @Transactional
    public UserDTO registerUser(RegistrationRequest request) {
        // 1. Check if username is unique
        if (userAuthMapper.selectCount(new QueryWrapper<UserAuth>()
               .eq("identity_type", "username")
               .eq("identifier", request.getUsername())) > 0) {
            throw new IllegalStateException("Username is already taken");
        }

        // 2. Check if email is provided and unique
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userAuthMapper.selectCount(new QueryWrapper<UserAuth>()
                   .eq("identity_type", "email")
                   .eq("identifier", request.getEmail())) > 0) {
                throw new IllegalStateException("Email is already registered");
            }
        }

        // 3. Create and save the user entity
        User user = new User();
        user.setEmail(request.getEmail()); // Store email in user profile
        user.setStatus(0); // 0: Normal/Active account
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // 4. Create username auth entry
        UserAuth usernameAuth = new UserAuth();
        usernameAuth.setUserId(user.getId());
        usernameAuth.setIdentityType("username");
        usernameAuth.setIdentifier(request.getUsername());
        // CRITICAL: Password is hashed using BCrypt before storage
        usernameAuth.setCredential(passwordEncoder.encode(request.getPassword()));
        usernameAuth.setCreatedAt(LocalDateTime.now());
        usernameAuth.setUpdatedAt(LocalDateTime.now());
        userAuthMapper.insert(usernameAuth);

        // 5. Create email auth entry if email is provided
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            UserAuth emailAuth = new UserAuth();
            emailAuth.setUserId(user.getId());
            emailAuth.setIdentityType("email");
            emailAuth.setIdentifier(request.getEmail());
            emailAuth.setCredential(passwordEncoder.encode(request.getPassword()));
            emailAuth.setCreatedAt(LocalDateTime.now());
            emailAuth.setUpdatedAt(LocalDateTime.now());
            userAuthMapper.insert(emailAuth);
        }

        return mapToUserDTO(user);
    }

    /**
     * Retrieves a user's public profile by ID.
     * 
     * @param userId The user's unique identifier
     * @return UserDTO with public profile information
     * @throws RuntimeException if user not found (consider using custom exception in production)
     */
    @Override
    public UserDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
        return mapToUserDTO(user);
    }

    /**
     * Retrieves authentication details for credential validation.
     * 
     * This method is called by blog-auth-service during login to:
     * 1. Get the hashed password for verification
     * 2. Check the account status (active, disabled, etc.)
     * 3. Retrieve user metadata for JWT token generation
     * 
     * @param identityType Type of identifier (e.g., "email", "username")
     * @param identifier The unique identifier value
     * @return UserAuthDetailsDTO with credentials and status, or null if not found
     * @throws IllegalStateException if data inconsistency is detected
     */
    @Override
    public UserAuthDetailsDTO findUserAuthDetailsByIdentifier(String identityType, String identifier) {
        // Find the authentication record
        UserAuth userAuth = userAuthMapper.selectOne(new QueryWrapper<UserAuth>()
               .eq("identity_type", identityType)
               .eq("identifier", identifier));
        
        if (userAuth == null) {
            return null; // User not found
        }

        // Retrieve the associated user profile
        User user = userMapper.selectById(userAuth.getUserId());
        if (user == null) {
            // This should never happen if database constraints are properly set
            throw new IllegalStateException("Inconsistent data: Auth record exists but user does not.");
        }

        // Return complete authentication details for auth-service
        return new UserAuthDetailsDTO(
            user.getId(),
            userAuth.getIdentifier(),
            userAuth.getCredential(), // BCrypt hashed password
            userAuth.getIdentifier(),
            user.getStatus()
        );
    }

    @Override
    @Transactional
    public UserDTO updateAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("头像文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("仅支持上传图片文件");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        String newAvatarUrl = storeAvatarFile(userId, file);
        deleteExistingAvatarFile(user.getAvatarUrl());

        user.setAvatarUrl(newAvatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return mapToUserDTO(user);
    }

    /**
     * Maps User entity to UserDTO for public API responses.
     * Excludes sensitive information.
     * 
     * @param user User entity from database
     * @return UserDTO for API response
     */
    private UserDTO mapToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBio(user.getBio());
        
        // Get username from UserAuth table
        UserAuth usernameAuth = userAuthMapper.selectOne(new QueryWrapper<UserAuth>()
               .eq("user_id", user.getId())
               .eq("identity_type", "username"));
        if (usernameAuth != null) {
            dto.setUsername(usernameAuth.getIdentifier());
        }
        
        return dto;
    }

    @Override
    @Transactional
    public UserDTO updateUsername(Long userId, String newUsername) {
        if (!StringUtils.hasText(newUsername)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (newUsername.length() < 3 || newUsername.length() > 50) {
            throw new IllegalArgumentException("用户名长度需在3到50个字符之间");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        UserAuth usernameAuth = userAuthMapper.selectOne(new QueryWrapper<UserAuth>()
                .eq("user_id", userId)
                .eq("identity_type", "username"));

        if (usernameAuth == null) {
            throw new IllegalStateException("Username credential not found for user: " + userId);
        }

        String trimmedUsername = newUsername.trim();
        if (trimmedUsername.equals(usernameAuth.getIdentifier())) {
            return mapToUserDTO(user);
        }

        long existing = userAuthMapper.selectCount(new QueryWrapper<UserAuth>()
                .eq("identity_type", "username")
                .eq("identifier", trimmedUsername)
                .ne("user_id", userId));

        if (existing > 0) {
            throw new IllegalArgumentException("该用户名已被占用");
        }

        usernameAuth.setIdentifier(trimmedUsername);
        usernameAuth.setUpdatedAt(LocalDateTime.now());
        userAuthMapper.updateById(usernameAuth);

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return mapToUserDTO(user);
    }

    private String storeAvatarFile(Long userId, MultipartFile file) {
        try {
            if (avatarStoragePath == null) {
                initAvatarStorage();
            }

            String originalFilename = file.getOriginalFilename();
            String cleanedName = originalFilename != null ? StringUtils.cleanPath(originalFilename) : "";
            String extension = StringUtils.getFilenameExtension(cleanedName);

            if (!StringUtils.hasText(extension) && file.getContentType() != null) {
                extension = switch (file.getContentType()) {
                    case "image/png" -> "png";
                    case "image/jpeg", "image/jpg" -> "jpg";
                    case "image/gif" -> "gif";
                    case "image/webp" -> "webp";
                    default -> "";
                };
            }

            String filename = "user-" + userId + "-" + System.currentTimeMillis();
            if (StringUtils.hasText(extension)) {
                filename += "." + extension.toLowerCase();
            }

            Path target = avatarStoragePath.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return AVATAR_URL_PREFIX + filename;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store avatar file", ex);
        }
    }

    private void deleteExistingAvatarFile(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl) || !avatarUrl.startsWith("/uploads/")) {
            return;
        }

        if (avatarStoragePath == null) {
            initAvatarStorage();
        }

        Path uploadsRoot = avatarStoragePath != null ? avatarStoragePath.getParent() : null;
        if (uploadsRoot == null) {
            uploadsRoot = avatarStoragePath;
        }

        if (uploadsRoot == null) {
            return;
        }

        String relative = avatarUrl.substring("/uploads/".length());
        Path target = uploadsRoot.resolve(relative).normalize();

        try {
            if (target.startsWith(uploadsRoot)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ex) {
            // Ignore deletion failures, log if needed
        }
    }
}
