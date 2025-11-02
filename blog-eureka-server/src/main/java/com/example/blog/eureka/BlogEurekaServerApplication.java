package com.example.blog.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Entry point for the Eureka Server that provides service discovery for the platform.
 */
@SpringBootApplication
@EnableEurekaServer
public class BlogEurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogEurekaServerApplication.class, args);
    }
}
