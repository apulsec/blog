package com.example.blog.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Exposes uploaded avatar files through the /uploads path.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.avatar.upload-dir:uploads/avatars}")
    private String avatarUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path avatarPath = Paths.get(avatarUploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(avatarPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create avatar upload directory", e);
        }

        Path uploadsRoot = avatarPath.getParent() != null ? avatarPath.getParent() : avatarPath;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsRoot.toUri().toString());
    }
}
