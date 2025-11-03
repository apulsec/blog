# Blog Platform Monorepo

本仓库包含一个基于 Spring Boot + Vue 3 的博客平台，采用微服务架构，涵盖文章、用户、认证三个核心服务以及前端应用和 Eureka 服务发现中心。仓库同时提供一套 PowerShell 脚本与 SQL 资产，帮助开发者在本地快速启动并验证整套系统。

## 模块一览

- `blog-frontend`：Vue 3 + Vite 单页面应用，负责用户界面与交互。
- `blog-article-service`：Spring Boot 文章服务，使用 PostgreSQL + MongoDB 存储文章元数据与正文，并通过 RabbitMQ 发布点赞/评论消息。
- `blog-user-service`：Spring Boot 用户服务，管理用户资料、认证信息，并订阅 RabbitMQ 推送生成通知。
- `blog-auth-service`：Spring Boot 认证服务，提供基于 JWT 的登录、刷新、登出能力，同时使用 Redis 管理令牌黑名单。
- `blog-eureka-server`：服务发现中心，为各后端提供注册与发现。
- `database/sql`：集中化的数据库初始化脚本，配合根目录 PowerShell 脚本使用。

## 快速开始

1. **准备依赖**
   - Docker Desktop、Node.js 18、JDK 17、Maven 3.9、PowerShell 7。
2. **克隆仓库并进入目录**
   ```powershell
   git clone <repo-url> blog
   cd blog
   ```
3. **启动基础设施**
   ```powershell
   docker compose up -d
   ./check-db-status.ps1
   ```
4. **初始化数据库**
   ```powershell
   ./init-databases.ps1
   ```
   该脚本会自动定位 Docker 中的 PostgreSQL、MongoDB 容器，复制 `database/sql/article-db-init.sql` 与 `database/sql/user-db-init.sql` 到容器执行，并清空 MongoDB 内容。
5. **启动前后端服务**
   ```powershell
   ./start-all-services.ps1
   ```
   脚本会依次启动 Eureka、用户服务、文章服务、认证服务以及前端应用。
6. **冒烟测试**（可选）
   ```powershell
   ./test-frontend-services.ps1
   ./test-create-article.ps1
   ./test-notifications.ps1
   ```

访问 `http://localhost:5173` 验证前端页面，其他服务默认端口为 8081（认证）、8082（文章）、8083（用户）。

## 脚本与自动化

- `init-databases.ps1`：集中初始化文章/用户数据库并清理 MongoDB。
- `start-all-services.ps1`：带端口检测的服务启动脚本，支持与 Docker 基础设施协同。
- `check-db-status.ps1`：快速验证 PostgreSQL、MongoDB、Redis 是否可达。
- `test-*.ps1`：一组端到端脚本覆盖文章列表、通知、JWT 流程等关键链路。

## 开发计划

使用 `.todo.md` 跟踪功能迭代与技术债务；更详细的架构说明请参考 `项目概览.md`，搭建步骤细节可查看 `.项目快速搭建指南.md`。
