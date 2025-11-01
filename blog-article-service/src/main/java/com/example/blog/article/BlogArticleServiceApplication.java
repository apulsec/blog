package com.example.blog.article;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for the Blog Article Service.
 * This is the entry point of the Spring Boot application.
 * 
 * @EnableDiscoveryClient - Enables service registration with Eureka
 * @EnableFeignClients - Enables Feign clients for service-to-service communication
 * @MapperScan - Scans for MyBatis-Plus mapper interfaces
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.example.blog.article.mapper")
public class BlogArticleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogArticleServiceApplication.class, args);
    }

}
