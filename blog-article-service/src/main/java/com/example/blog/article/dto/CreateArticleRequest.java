package com.example.blog.article.dto;

import java.util.List;

/**
 * DTO for creating a new article.
 */
public class CreateArticleRequest {
    
    private Long authorId;
    private String title;
    private String summary;
    private String coverImageUrl;
    private String content; // MongoDB content
    private String status; // DRAFT or PUBLISHED
    private List<String> tags; // Tag names
    
    // Getters and Setters
    public Long getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getCoverImageUrl() {
        return coverImageUrl;
    }
    
    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
