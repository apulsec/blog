git clone YOUR_REMOTE_URL blog
docker compose up -d
docker compose ps
docker exec -it blog-postgres psql -U postgres -c "SELECT version();"
docker exec -it blog-mongo mongosh --eval "db.runCommand({ ping: 1 })"
docker exec -it blog-redis redis-cli ping
docker exec -it blog-postgres psql -U postgres -c "\l"
docker exec -it blog-postgres psql -U postgres -d blog_article_db -c "\dt"
docker exec -it blog-postgres psql -U postgres -d blog_user_db -c "\dt"

# 本地环境搭建手册

本文档面向第一天加入项目的同学，按照顺序执行脚本即可在本地启动完整的博客平台（前端 + 三个后端微服务）。所有示例均以 Windows PowerShell (`pwsh`) 为例。

## 1. 一图速览

| 步骤 | 目标                                     | 对应脚本/命令                                                              |
| ---- | ---------------------------------------- | -------------------------------------------------------------------------- |
| ①    | 启动基础设施（PostgreSQL/MongoDB/Redis） | `docker compose up -d`                                                     |
| ②    | 初始化业务数据                           | `blog-article-service\init-db-docker.ps1`、`blog-user-service\init-db.ps1` |
| ③    | 启动所有后端与前端                       | `./start-all-services.ps1`                                                 |
| ④    | 冒烟验证                                 | `test-*.ps1` 系列脚本                                                      |

## 2. 环境准备

确保以下组件已安装且命令可用：

- Git ≥ 2.30：`git --version`
- Docker Desktop ≥ 4.27：`docker version`
- Node.js 18 LTS（附带 npm）：`node --version`、`npm --version`
- JDK 17：`java -version`
- Apache Maven ≥ 3.9：`mvn -v`
- PowerShell 7（推荐）：`pwsh -v`

如未安装，请先完成安装并将其加入 `PATH`。

## 3. 获取源码

```powershell
cd ~\Desktop
git clone YOUR_REMOTE_URL blog
cd blog
```

仓库根目录即 `c:\Users\lenovo\Desktop\blog`。如已在该目录可直接进入下一步。

## 4. 启动基础设施

1. 确认 Docker Desktop 处于 Running 状态。
2. 在项目根目录执行：

   ```powershell
   docker compose up -d
   ```

   根目录自带的 `docker-compose.yml` 会启动 PostgreSQL、MongoDB 与 Redis，并暴露默认端口。

3. 使用现成脚本快速检查容器与端口：

   ```powershell
   ./check-db-status.ps1
   ```

   输出包含 Redis、PostgreSQL、MongoDB 的连通性检测，全部为绿色 ✓ 即表示基础设施就绪。

## 5. 初始化数据库

依次执行以下脚本（均需在仓库根目录）：

```powershell
./blog-article-service/init-db-docker.ps1   # 初始化文章库、样例数据
./blog-user-service/init-db.ps1             # 初始化用户库结构
```

如果需要补充点赞/评论表，可额外执行 `blog-article-service/add-likes-and-comments-tables.sql`。

## 6. 启动后端与前端

根目录提供了“一键启动”脚本，会自动：

- 检查 Redis/PostgreSQL 是否在线
- 探测 Eureka（支持 Docker 18761 或本地 8761）并按需启动
- 依次启动 `blog-user-service`、`blog-article-service`、`blog-auth-service`
- 最后拉起 `blog-frontend`（Vite dev server）

执行：

```powershell
./start-all-services.ps1
```

脚本会为每个服务打开独立的 PowerShell 窗口，等待日志出现 `Started ...` 即表示成功。完成后可访问：

- 前端：`http://localhost:5173`
- 用户服务：`http://localhost:8083`
- 文章服务：`http://localhost:8082`
- 认证服务：`http://localhost:8081`

## 7. 冒烟测试

项目根目录提供了一组 PowerShell 测试脚本，确保主要链路可用：

```powershell
./test-frontend-services.ps1   # 检查前端关键页面
./test-create-article.ps1      # 模拟创建文章
./test-article-list.ps1        # 拉取文章列表并校验结构
./test-notifications.ps1       # 验证通知推送
./test-jwt-token.ps1           # 校验登录/刷新令牌流程
```

脚本会输出详细的通过/失败信息，如需关注某个服务的日志，可配合 `check-article-service-logs.ps1` 或各服务工程里的 `check-status.ps1`。

## 8. 常见问题

- **端口占用**：如果脚本提示端口被占用，可先停止对应程序，或在 `start-all-services.ps1` 中调整端口并同步修改前端代理。
- **依赖服务未启动**：运行 `docker compose ps` 与 `check-db-status.ps1` 检查容器，必要时执行 `docker compose logs <服务名>` 获取详情。
- **Maven/Node 下载缓慢**：可临时切换镜像，例如 `npm config set registry https://registry.npmmirror.com`。
- **脚本异常中断**：查看终端中的错误提示，修复后可重新执行同一个脚本；重复执行不会破坏已有数据。

## 9. 清理与重置

需要重置环境时执行：

```powershell
docker compose down -v   # 停止并清空容器与数据卷
```

随后重新按照本文档顺序运行脚本即可。项目根目录的 `docker操作命令.md` 还整理了更多 Docker 常用命令，供深入参考。
