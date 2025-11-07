package com.example.blog.article.dto;

/**
 * Lightweight projection for hot articles ranking, returned by the analytics endpoint.
 */
public class HotArticleDTO {

    private Long articleId;
    private String title;
    private Double hotScore;

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getHotScore() {
        return hotScore;
    }

    public void setHotScore(Double hotScore) {
        this.hotScore = hotScore;
    }
}