package com.example.blog.article.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO exposed via REST to surface the latest analytics snapshot for an article.
 */
public class ArticleMetricsDTO {

	private Long articleId;
	private LocalDate metricDate;
	private Integer likesCount;
	private Integer commentsCount;
	private Double hotScore;
	private OffsetDateTime updatedAt;

	public Long getArticleId() {
		return articleId;
	}

	public void setArticleId(Long articleId) {
		this.articleId = articleId;
	}

	public LocalDate getMetricDate() {
		return metricDate;
	}

	public void setMetricDate(LocalDate metricDate) {
		this.metricDate = metricDate;
	}

	public Integer getLikesCount() {
		return likesCount;
	}

	public void setLikesCount(Integer likesCount) {
		this.likesCount = likesCount;
	}

	public Integer getCommentsCount() {
		return commentsCount;
	}

	public void setCommentsCount(Integer commentsCount) {
		this.commentsCount = commentsCount;
	}

	public Double getHotScore() {
		return hotScore;
	}

	public void setHotScore(Double hotScore) {
		this.hotScore = hotScore;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
