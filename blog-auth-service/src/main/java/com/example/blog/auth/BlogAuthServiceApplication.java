package com.example.blog.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for the Blog Authentication Service.
 * This is the entry point of the Spring Boot application.
 * 
 * @EnableFeignClients - Enables Feign client scanning for declarative REST clients
 */
@SpringBootApplication
@EnableFeignClients
public class BlogAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogAuthServiceApplication.class, args);
    }

}
