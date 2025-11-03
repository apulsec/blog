# Docker 操作命令

## PostgreSQL 容器

**查看容器状态**

```bash
docker ps
```

**进入容器并连接 psql**

```bash
docker exec -it <PostgreSQL 容器 ID 或名称> psql -U postgres
```

**常用 psql 操作**

| 场景                 | 命令                              | 说明                     |
| -------------------- | --------------------------------- | ------------------------ |
| 查看所有数据库       | `\l`                              | 列出所有数据库           |
| 切换数据库           | `\c <数据库名>`                   | 连接到指定数据库         |
| 查看当前数据库中的表 | `\dt`                             | 列出当前数据库中的所有表 |
| 查看特定表结构       | `\d <表名>`                       | 显示表的列、类型等信息   |
| 执行查询             | `SELECT * FROM <表名>;`           | 查看表中的数据           |
| 查看连接/进程        | `SELECT * FROM pg_stat_activity;` | 查看当前的活动连接和查询 |
| 退出 psql            | `\q`                              | 退出 PostgreSQL 命令行   |

**切换到业务数据库**

```sql
\c blog_article_db
```

**数据库对象管理**

| 目的                   | 命令  | 说明                                            |
| ---------------------- | ----- | ----------------------------------------------- |
| 查看所有表、视图、序列 | `\dt` | 列出当前数据库中的关系（表），`t` 代表 `Tables` |
| 查看所有模式           | `\dn` | 列出数据库中的所有模式                          |
| 查看所有函数           | `\df` | 列出所有函数                                    |
| 查看所有索引           | `\di` | 列出所有索引                                    |
| 查看当前模式对象       | `\d`  | 列出当前模式（默认为 `public`）中的所有对象     |

**查看指定表结构**

```sql
\d articles
-- 之后可以继续运行需要的 SQL 语句
```

---

## MongoDB 容器

**启动容器**

```bash
docker run -d --name blog-mongodb -p 27017:27017 \
	-e MONGO_INITDB_ROOT_USERNAME=root \
	-e MONGO_INITDB_ROOT_PASSWORD=password mongo:latest
```

**进入容器并连接 mongosh**

```bash
docker exec -it blog-mongo mongosh -u root -p password
# 或
docker exec -it <MongoDB 容器 ID 或名称> mongosh
```

**常用 mongosh 操作**

| 场景           | 命令                  | 说明                         |
| -------------- | --------------------- | ---------------------------- |
| 查看所有数据库 | `show dbs`            | 列出所有数据库               |
| 切换数据库     | `use <数据库名>`      | 切换到指定数据库             |
| 查看集合       | `show collections`    | 列出当前数据库中的所有集合   |
| 查看集合数据   | `db.<集合名>.find()`  | 查看集合中的文档             |
| 查看集合统计   | `db.<集合名>.stats()` | 查看集合大小、索引等统计信息 |
| 查看服务器状态 | `db.serverStatus()`   | 查看 MongoDB 服务器运行状态  |
| 查看副本集状态 | `rs.status()`         | 如果配置副本集，查看其状态   |
| 退出 shell     | `exit`                | 退出 MongoDB shell           |
