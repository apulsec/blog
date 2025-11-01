package com.example.blog.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main entry point for the Blog User Service application.
 * This service manages user accounts, authentication credentials, and user profiles.
 * It provides both public registration endpoints and internal APIs for credential validation.
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.example.blog.user.mapper")
public class BlogUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogUserServiceApplication.class, args);
    }

}
