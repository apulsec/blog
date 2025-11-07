# Blog Platform Monorepo

本仓库提供一个基于 **Spring Boot + Vue 3** 的博客平台，采用微服务架构，涵盖文章、用户、认证三个核心服务，以及前端应用和 Eureka 服务发现中心。仓库内置脚本与 SQL 资产，支持一键完成本地环境部署、数据库初始化与冒烟验证。

## 模块一览

- `blog-frontend`：Vue 3 + Vite 单页应用，负责 UI 与交互。
- `blog-article-service`：Spring Boot 文章服务，使用 PostgreSQL 存储元数据、MongoDB 保存正文，并通过 RabbitMQ 发布点赞/评论通知。
- `blog-user-service`：Spring Boot 用户服务，管理用户资料与认证信息，消费 RabbitMQ 消息生成通知。
- `blog-auth-service`：Spring Boot 认证服务，提供基于 JWT 的登录/刷新/登出能力，并借助 Redis 管理令牌黑名单。
- `blog-eureka-server`：Spring Cloud Eureka 服务发现中心。
- `analytics-spark-job`：Apache Spark 批处理作业（本地模式），每天聚合文章互动指标并写入 PostgreSQL 的 `article_metrics` 表。
- `database/sql`：集中化的数据库初始化脚本目录，与根目录 PowerShell 脚本配合使用。

## 快速开始

1. **准备依赖**：Docker Desktop、Node.js 18、JDK 17、Maven 3.9、PowerShell 7。
2. **克隆仓库**
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
   脚本会自动定位 Docker 中的 PostgreSQL、MongoDB 容器，将 `database/sql/article-db-init.sql` 与 `database/sql/user-db-init.sql` 复制进容器执行，并清空 MongoDB 内容。
5. **启动前后端服务**
   ```powershell
   ./start-all-services.ps1
   ```
   脚本依次启动 Eureka、用户服务、文章服务、认证服务以及前端应用，自动处理端口探测与依赖检查。
6. **冒烟测试**（可选）
   ```powershell
   ./test-frontend-services.ps1
   ./test-create-article.ps1
   ./test-notifications.ps1
   ```

访问 `http://localhost:5173` 验证前端页面；后端默认端口：认证 8081、文章 8082、用户 8083。

## 脚本与自动化

- `init-databases.ps1`：集中初始化文章库与用户库，并清理 MongoDB。
- `start-all-services.ps1`：一键启动后端与前端，附带端口冲突检测与环境准备。
- `check-db-status.ps1`：快速检查 PostgreSQL/MongoDB/Redis 连通性。
- `test-*.ps1`：端到端验证脚本，覆盖文章列表、通知、JWT 流程等关键链路。
- `run-article-metrics.ps1`：在本机 Spark 环境下构建并执行批处理作业，将聚合结果写入 `article_metrics` 表，供文章服务读取。

## Spark 数据分析快速开始

1. 安装 [Apache Spark 3.5.x](https://spark.apache.org/downloads.html)，并在终端中配置 `SPARK_HOME` 环境变量，确保 `%SPARK_HOME%\bin\spark-submit` 可执行。
2. 初始化数据库：`./init-databases.ps1` 会自动创建 `article_metrics` 表。
3. 构建并运行分析作业：
   ```powershell
   ./run-article-metrics.ps1
   ```
   可通过参数覆盖默认值，例如：
   ```powershell
   ./run-article-metrics.ps1 -MetricDate 2025-11-06 -JdbcUrl "jdbc:postgresql://localhost:15432/blog_article_db"
   ```
4. 调用 `GET /api/articles/{id}/metrics`（由 `blog-article-service` 提供）即可验证聚合结果。

## 状态追踪与进一步阅读

- `./.todo.md`：功能迭代与技术债务清单。
- `项目概览.md`：架构与业务流程说明。
- `.项目快速搭建指南.md`：面向新成员的详细环境搭建手册。
