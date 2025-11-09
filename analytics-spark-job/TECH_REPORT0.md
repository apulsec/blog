# analytics-spark-job 技术报告

## 模块概览

- 项目类型：Maven 构建的单模块 Java 17 应用，产出可执行 JAR（`maven-shade-plugin` 指定入口 `com.example.blog.analytics.ArticleMetricsJob`）。
- 主要依赖：Apache Spark 3.5.1（`spark-core_2.12` 与 `spark-sql_2.12`，标记为 `provided` 以适配集群运行时提供的 Spark）、PostgreSQL JDBC 驱动 42.7.3。
- 代码结构：`src/main/java/com/example/blog/analytics/ArticleMetricsJob.java` 为唯一业务类，实现从博客数据库读取交互数据、聚合并写回 `article_metrics` 表的批任务。

## 入口流程（`main` 方法）

1. 调用 `parseArgs` 解析命令行参数，支持 `--jdbc-url`、`--db-user`、`--db-password`、`--metrics-table`、`--master`、`--metric-date`、`--shuffle-partitions`、`--log-level` 等键值对。
2. 设置默认参数：
   - JDBC：`jdbc:postgresql://localhost:15432/blog_article_db`，用户 `postgres`，密码 `password`。
   - Spark master：`local[*]`，shuffle 分区默认 `1`。
   - 指标日期：若未指定 `metric-date`，使用当前 UTC 日期。
3. 调用 `validateTableName` 确保指标表名仅包含字母数字下划线，避免 SQL 注入。
4. 构造 `SparkSession`：显式配置 `spark.sql.shuffle.partitions` 与 `spark.sql.session.timeZone = UTC`，随后设置日志级别。
5. 执行 `runAggregation`，捕获异常并在失败时输出堆栈、返回非零退出码。
6. 始终执行 `spark.stop()` 释放资源。

## 数据读取与转换

`runAggregation` 负责核心计算流程：

- **读取基础文章列表**：通过 JDBC 读取子查询 `SELECT id AS article_id FROM t_article`，确保未有交互的文章也纳入结果集。
- **读取点赞聚合**：子查询 `SELECT article_id, COUNT(*) AS likes_count FROM article_likes GROUP BY article_id`。
- **读取评论聚合**：子查询 `SELECT article_id, COUNT(*) AS comments_count FROM comments GROUP BY article_id`。
- Spark 以 `Dataset<Row>` 形式加载三张临时表，均使用 PostgreSQL 驱动和统一的连接参数。

### 聚合逻辑

1. 左连接基础文章集与点赞、评论聚合（键 `article_id`），保留所有文章。
2. 利用 `functions.coalesce` 将缺失的 `likes_count`、`comments_count` 填充为 `0`。
3. 添加派生字段：
   - `metric_date`：恒为当前批次日期（`java.sql.Date`）。
   - `hot_score`：业务评分=点赞数 ×2 + 评论数。
   - `updated_at`：写入时间戳（`current_timestamp`）。
4. 调整字段类型：`likes_count`、`comments_count` 强制为 `int`，`hot_score` 为 `double`，输出列顺序为 `article_id, metric_date, likes_count, comments_count, hot_score, updated_at`。
5. `aggregated.count()` 获取计划写入的行数；若无文章则记录提示并跳过写入。

## 数据写入与幂等设计

- 在写入前调用 `purgeExistingSnapshots`：使用 JDBC `PreparedStatement` 执行 `DELETE FROM <metrics_table> WHERE metric_date = ?`，确保同一 `metric_date` 的历史数据被清除，实现基于日期的幂等覆盖。
- 使用 `SaveMode.Append` 经 JDBC 将 DataFrame 写入目标表，避免 Spark 在 DDL 上产生附加副作用。
- 完成后打印写入行数、目标日期和表名，便于运维审计。

## 辅助工具方法

- `parseArgs`：遍历 `--key value` 形式的参数列表；支持无值参数（值记为 `null`），结果存入 `HashMap`。
- `validateTableName`：基于正则 `[a-zA-Z0-9_]+` 校验，若失败抛出 `IllegalArgumentException`。
- `parseDate`：对空串返回 `null`，否则按 `yyyy-MM-dd` 默认格式解析为 `LocalDate`。

## 运行时交互关系

- **Spark ↔ 数据库**：所有读写都通过 JDBC 完成；Spark 端不缓存中间结果，仅依赖 DataFrame 操作链执行分布式查询与写入。
- **本地 Java ↔ 数据库**：删除旧快照操作不走 Spark，直接用 JDBC 执行，避免受 Spark 重试机制影响，确保删除一次即可生效。
- **配置参数**：Spark session 配置仅影响当前任务；数据库连接参数贯穿读取与写入阶段，需保持一致性。

## 可扩展性提示（纯技术角度）

- 如需支持更多指标（如浏览量），可在 JDBC 子查询中添加对应聚合并在 DataFrame 转换阶段扩展列。
- 若目标库为分区表，可将 `metric_date` 作为分区键，并在写入时增加 `option("truncate", false)` 等 JDBC 配置。
- 当前 `hot_score` 为线性规则，若需复用可抽象为 UDF 或配置化权重。
- 对于大规模数据，可将 `shuffle-partitions` 参数外部化，以适配集群资源。
