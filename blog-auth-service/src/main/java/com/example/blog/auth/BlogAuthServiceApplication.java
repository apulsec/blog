package com.example.blog.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for the Blog Authentication Service.
 * This is the entry point of the Spring Boot application.
 * 
 * @EnableDiscoveryClient - Registers the service with Eureka for discovery
 * @EnableFeignClients - Enables Feign client scanning for declarative REST clients
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class BlogAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogAuthServiceApplication.class, args);
    }

}
