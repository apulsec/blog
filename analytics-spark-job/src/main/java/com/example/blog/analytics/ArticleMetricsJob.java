package com.example.blog.analytics;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal Spark batch job that aggregates article interactions into the article_metrics table.
 */
public final class ArticleMetricsJob {

    private static final String POSTGRES_DRIVER = "org.postgresql.Driver";

    private ArticleMetricsJob() {
    }

    public static void main(String[] args) {
        Map<String, String> options = parseArgs(args);

        String jdbcUrl = options.getOrDefault("jdbc-url", "jdbc:postgresql://localhost:15432/blog_article_db");
        String dbUser = options.getOrDefault("db-user", "postgres");
        String dbPassword = options.getOrDefault("db-password", "password");
        String metricsTable = options.getOrDefault("metrics-table", "article_metrics");
        String master = options.getOrDefault("master", "local[*]");
        LocalDate metricDate = parseDate(options.getOrDefault("metric-date", null));
        if (metricDate == null) {
            metricDate = LocalDate.now(ZoneOffset.UTC);
        }

        validateTableName(metricsTable);

        SparkSession spark = SparkSession.builder()
                .appName("ArticleMetricsJob")
                .master(master)
                .config("spark.sql.shuffle.partitions", options.getOrDefault("shuffle-partitions", "1"))
                .config("spark.sql.session.timeZone", "UTC")
                .getOrCreate();
        spark.sparkContext().setLogLevel(options.getOrDefault("log-level", "WARN"));

        int exitCode = 0;
        try {
            runAggregation(spark, jdbcUrl, dbUser, dbPassword, metricsTable, metricDate);
        } catch (Exception ex) {
            exitCode = 1;
            System.err.println("Failed to aggregate article metrics: " + ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            spark.stop();
        }

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static void runAggregation(SparkSession spark,
                                       String jdbcUrl,
                                       String dbUser,
                                       String dbPassword,
                                       String metricsTable,
                                       LocalDate metricDate) throws SQLException {
        Dataset<Row> articles = spark.read()
                .format("jdbc")
                .option("url", jdbcUrl)
                .option("dbtable", "(SELECT id AS article_id FROM t_article) AS base")
                .option("user", dbUser)
                .option("password", dbPassword)
                .option("driver", POSTGRES_DRIVER)
                .load();

        Dataset<Row> likes = spark.read()
                .format("jdbc")
                .option("url", jdbcUrl)
                .option("dbtable", "(SELECT article_id, COUNT(*) AS likes_count FROM article_likes GROUP BY article_id) AS likes")
                .option("user", dbUser)
                .option("password", dbPassword)
                .option("driver", POSTGRES_DRIVER)
                .load();

        Dataset<Row> comments = spark.read()
                .format("jdbc")
                .option("url", jdbcUrl)
                .option("dbtable", "(SELECT article_id, COUNT(*) AS comments_count FROM comments GROUP BY article_id) AS comments")
                .option("user", dbUser)
                .option("password", dbPassword)
                .option("driver", POSTGRES_DRIVER)
                .load();

        Dataset<Row> aggregated = articles
                .join(likes, "article_id", "left")
                .join(comments, "article_id", "left")
                .withColumn("likes_count", functions.coalesce(functions.col("likes_count"), functions.lit(0)))
                .withColumn("comments_count", functions.coalesce(functions.col("comments_count"), functions.lit(0)))
                .withColumn("metric_date", functions.lit(java.sql.Date.valueOf(metricDate)))
                .withColumn("hot_score", functions.col("likes_count").multiply(functions.lit(2)).plus(functions.col("comments_count")))
                .withColumn("updated_at", functions.current_timestamp())
                .select(
                        functions.col("article_id"),
                        functions.col("metric_date"),
                        functions.col("likes_count").cast("int"),
                        functions.col("comments_count").cast("int"),
                        functions.col("hot_score").cast("double"),
                        functions.col("updated_at"));

        long rowCount = aggregated.count();
        if (rowCount == 0) {
            System.out.println("No articles found; skipping write.");
            return;
        }

        purgeExistingSnapshots(jdbcUrl, dbUser, dbPassword, metricsTable, metricDate);

        aggregated.write()
                .mode(SaveMode.Append)
                .format("jdbc")
                .option("url", jdbcUrl)
                .option("dbtable", metricsTable)
                .option("user", dbUser)
                .option("password", dbPassword)
                .option("driver", POSTGRES_DRIVER)
                .save();

        System.out.printf(Locale.ROOT,
                "Wrote %d metric rows for %s into %s%n",
                rowCount, metricDate, metricsTable);
    }

    private static void purgeExistingSnapshots(String jdbcUrl,
                                               String dbUser,
                                               String dbPassword,
                                               String metricsTable,
                                               LocalDate metricDate) throws SQLException {
        String sql = "DELETE FROM " + metricsTable + " WHERE metric_date = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, metricDate);
            int deleted = statement.executeUpdate();
            if (deleted > 0) {
                System.out.printf(Locale.ROOT,
                        "Removed %d existing rows for %s from %s before insert.%n",
                        deleted, metricDate, metricsTable);
            }
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        if (args == null) {
            return options;
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String key = arg.substring(2);
                String value = null;
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[++i];
                }
                options.put(key, value);
            }
        }
        return options;
    }

    private static void validateTableName(String tableName) {
        if (tableName == null || !tableName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid metrics table name: " + tableName);
        }
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date);
    }
}
