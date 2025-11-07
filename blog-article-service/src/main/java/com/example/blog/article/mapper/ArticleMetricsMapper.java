package com.example.blog.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.article.dto.HotArticleDTO;
import com.example.blog.article.entity.ArticleMetrics;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Mapper exposing CRUD operations for {@code article_metrics} snapshots.
 */
@Repository
public interface ArticleMetricsMapper extends BaseMapper<ArticleMetrics> {

	@Insert("INSERT INTO article_metrics (article_id, metric_date, likes_count, comments_count, hot_score, updated_at) " +
			"VALUES (#{articleId}, #{metricDate}, #{likesCount}, #{commentsCount}, #{hotScore}, #{updatedAt}) " +
			"ON CONFLICT (article_id, metric_date) DO NOTHING")
	int insertSnapshot(ArticleMetrics metrics);

	@Insert("INSERT INTO article_metrics (article_id, metric_date, likes_count, comments_count, hot_score, updated_at) " +
			"VALUES (#{articleId}, #{metricDate}, #{likeDelta}, #{commentDelta}, #{hotScoreDelta}, #{updatedAt}) " +
			"ON CONFLICT (article_id, metric_date) DO UPDATE SET " +
			"likes_count = article_metrics.likes_count + EXCLUDED.likes_count, " +
			"comments_count = article_metrics.comments_count + EXCLUDED.comments_count, " +
			"hot_score = article_metrics.hot_score + EXCLUDED.hot_score, " +
			"updated_at = EXCLUDED.updated_at")
	int upsertInteractionSnapshot(@Param("articleId") Long articleId,
			@Param("metricDate") java.time.LocalDate metricDate,
			@Param("likeDelta") int likeDelta,
			@Param("commentDelta") int commentDelta,
			@Param("hotScoreDelta") double hotScoreDelta,
			@Param("updatedAt") java.time.OffsetDateTime updatedAt);

	@Select("SELECT article_id AS articleId, metric_date AS metricDate, likes_count AS likesCount, " +
			"comments_count AS commentsCount, hot_score AS hotScore, updated_at AS updatedAt " +
			"FROM article_metrics WHERE article_id = #{articleId} ORDER BY metric_date DESC LIMIT 1")
	ArticleMetrics findLatestByArticleId(@Param("articleId") Long articleId);

	@Select("SELECT a.id AS articleId, a.title AS title, SUM(am.hot_score) AS hotScore " +
			"FROM article_metrics am " +
			"JOIN t_article a ON a.id = am.article_id " +
			"WHERE a.status = 'PUBLISHED' AND am.metric_date >= #{startDate} " +
			"GROUP BY a.id, a.title " +
			"ORDER BY hotScore DESC, MAX(am.metric_date) DESC " +
			"LIMIT #{limit}")
	List<HotArticleDTO> findHotArticles(@Param("startDate") LocalDate startDate, @Param("limit") int limit);
}
