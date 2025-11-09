# blog-eureka-server 技术报告

## 概述

- 基于 Spring Boot 3.2.5 的应用，依托 Netflix Eureka 为博客微服务套件提供服务发现能力。
- 单一入口 `BlogEurekaServerApplication` 同时使用 `@SpringBootApplication` 与 `@EnableEurekaServer` 完成注册中心装配。
- 运行环境定位为 Java 17，并依赖 Spring Cloud 2023.0.1（Kilburn）BOM 对 Netflix 生态进行版本对齐。

## 构建与依赖

- `pom.xml` 通过 Spring Boot Parent 管理依赖版本，并导入 Spring Cloud BOM 保持 Netflix 相关组件一致。
- 关键依赖：
  - `spring-cloud-starter-netflix-eureka-server` 提供注册中心 Servlet 端点、注册表存储及租约管理。
  - `spring-boot-starter-web` 引入内嵌 Tomcat 与 MVC 基础设施（控制台视图由该 starter 附带的 JSP/Thymeleaf 模板渲染）。
  - `spring-boot-starter-actuator` 暴露健康检查与信息端点，便于外部监控。
  - `spring-boot-starter-test` 在测试范围启用（当前暂无测试），为后续扩展提供基础。
  - 可选依赖 Lombok 已引入但尚未使用，方便未来增加配置类或工具类。
- 构建插件 `spring-boot-maven-plugin` 产出可执行的 fat JAR，供 Docker 运行阶段使用。

## 应用启动

- `src/main/java/com/example/blog/eureka/BlogEurekaServerApplication.java`
  ```java
  @SpringBootApplication
  @EnableEurekaServer
  public class BlogEurekaServerApplication {
      public static void main(String[] args) {
          SpringApplication.run(BlogEurekaServerApplication.class, args);
      }
  }
  ```
  - `@EnableEurekaServer` 激活自动配置，暴露 `/eureka/*` REST 端点、托管注册表，并提供 HTML 控制台。
  - 应用类位于 `com.example.blog.eureka` 包下，Spring 组件扫描覆盖该命名空间（当前未定义额外 Bean）。

## 配置（`application.yml`）

- `server.port: 8761` 使用 Eureka 常用端口。
- `spring.application.name: blog-eureka-server` 使日志、Actuator 元数据中的应用标识保持一致。
- 管理端点开放 `health` 与 `info`，满足监控集成需求。
- Eureka 设置：
  - `client.register-with-eureka=false` 与 `client.fetch-registry=false` 禁止服务器自注册或从其他节点同步注册表，适合单节点开发环境。
  - `client.serviceUrl.defaultZone=http://localhost:8761/eureka/` 定义客户端注册的目标地址，同时用于控制台生成访问链接。
  - `server.enable-self-preservation=false` 关闭自我保护机制，一旦心跳缺失即快速驱逐实例，方便本地调试时及时更新路由信息（生产环境应重新评估）。

## Docker 打包

- 多阶段 Dockerfile：
  1. 构建阶段使用 `maven:3.9.6-eclipse-temurin-17` 运行 `mvn -B -DskipTests package` 生成 fat JAR。
  2. 运行阶段基于 `eclipse-temurin:17-jre`，拷贝产物、暴露 8761 端口，并以 `java -jar` 启动。
- 此方案保持镜像精简，同时确保运行时 Java 版本与构建阶段一致。

## 运行职责与交互

- 托管 Eureka 控制台（`/`）及 `/eureka` 下的 REST 端点，接受文章、认证、用户等服务的注册、心跳与状态更新。
- 配合前端网关与后端微服务，可通过逻辑服务 ID 实现位置透明，避免硬编码主机地址。
- 关闭自我保护后，客户端必须保持及时心跳；一旦中断即被驱逐，可防止迭代开发中出现陈旧路由。

## 扩展点

- 可在 `application.yml` 中追加对等节点、认证等配置，或在同包下新增 Spring `@Configuration` 类强化功能。
- 通过调整管理端点暴露列表，可扩展 Actuator（如 `/metrics`、`/prometheus`）。
- 若面向生产硬化，建议：
  - 引入 Spring Security 保护控制台访问。
  - 在反向代理或内嵌 Tomcat 中配置 TLS。
  - 部署多节点集群（在 `eureka.client.serviceUrl.defaultZone` 中列出多个对等地址），并重新启用自我保护机制。
