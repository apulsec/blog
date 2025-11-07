package com.example.blog.article.service;

import com.example.blog.article.dto.ArticleMetricsDTO;
import com.example.blog.article.dto.ArticleSummaryDTO;
import com.example.blog.article.dto.HotArticleDTO;
import com.example.blog.article.dto.CreateArticleRequest;
import com.example.blog.article.entity.Article;
import com.example.blog.article.entity.Tag;
import com.example.blog.article.dto.CommentDTO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service interface for article operations.
 * Defines business logic methods for managing and retrieving articles.
 */
public interface ArticleService {
    
    /**
     * Retrieves a paginated list of published articles.
     * Each article includes author information fetched from blog-user-service.
     *
     * @param page Page object containing pagination parameters
     * @return Page of ArticleSummaryDTO with article and author information
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<ArticleSummaryDTO> getPublishedArticles(com.baomidou.mybatisplus.extension.plugins.pagination.Page<Object> page);
    
    /**
     * Retrieves articles by status (PUBLISHED, DRAFT, etc.).
     *
     * @param page Page object containing pagination parameters
     * @param status Article status to filter by
     * @param authorId Optional author ID filter
     * @return Page of ArticleSummaryDTO matching the status
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<ArticleSummaryDTO> getArticlesByStatus(com.baomidou.mybatisplus.extension.plugins.pagination.Page<Object> page, String status, Long authorId);
    
    /**
     * Retrieves articles by tag name.
     *
     * @param page Page object containing pagination parameters
     * @param tagName Tag name to filter by
     * @param authorId Optional author ID filter
     * @return Page of ArticleSummaryDTO with the specified tag
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<ArticleSummaryDTO> getArticlesByTag(com.baomidou.mybatisplus.extension.plugins.pagination.Page<Object> page, String tagName, Long authorId);
    
    /**
     * Search articles by keyword in title or summary.
     *
     * @param page Page object containing pagination parameters
     * @param keyword Search keyword
     * @param authorId Optional author ID filter
     * @return Page of ArticleSummaryDTO matching the keyword
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<ArticleSummaryDTO> searchArticles(com.baomidou.mybatisplus.extension.plugins.pagination.Page<Object> page, String keyword, Long authorId);
    
    /**
     * Retrieves articles by author ID.
     *
     * @param page Page object containing pagination parameters
     * @param authorId Author ID to filter by
     * @return Page of ArticleSummaryDTO by the specified author
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<ArticleSummaryDTO> getArticlesByAuthor(com.baomidou.mybatisplus.extension.plugins.pagination.Page<Object> page, Long authorId);
    
    /**
     * Creates a new article with metadata in PostgreSQL and content in MongoDB.
     *
     * @param request CreateArticleRequest containing article details
     * @return Created Article entity
     */
    Article createArticle(CreateArticleRequest request);
    
    /**
     * Retrieves a single article by ID with its content.
     *
     * @param id Article ID
     * @return ArticleSummaryDTO with complete article information including content
     */
    ArticleSummaryDTO getArticleById(Long id);
    
    /**
     * Updates an existing article.
     *
     * @param id Article ID to update
     * @param request Updated article data
     * @return Updated Article entity
     */
    Article updateArticle(Long id, CreateArticleRequest request);
    
    /**
     * Deletes an article by ID.
     * Removes metadata from PostgreSQL and content from MongoDB.
     *
     * @param id Article ID to delete
     */
    void deleteArticle(Long id);
    
    /**
     * Retrieves all available tags.
     *
     * @return List of all tags
     */
    List<Tag> getAllTags();

    // --- Like and Comment Methods ---

    /**
     * Adds a like to an article for a specific user.
     *
     * @param articleId The ID of the article to like.
     * @param userId The ID of the user who liked the article.
     */
    void likeArticle(Long articleId, Long userId);

    /**
     * Removes a like from an article for a specific user.
     *
     * @param articleId The ID of the article to unlike.
     * @param userId The ID of the user who unliked the article.
     */
    void unlikeArticle(Long articleId, Long userId);

    /**
     * Checks if a user has liked a specific article.
     *
     * @param articleId The ID of the article.
     * @param userId The ID of the user.
     * @return true if the user has liked the article, false otherwise.
     */
    boolean hasUserLikedArticle(Long articleId, Long userId);

    /**
     * Adds a comment to an article.
     *
     * @param articleId The ID of the article.
     * @param userId The ID of the user commenting.
     * @param parentId The ID of the parent comment (if it's a reply).
     * @param content The content of the comment.
     * @return The created Comment entity.
     */
    CommentDTO createComment(Long articleId, Long userId, Long parentId, String content);

    /**
     * Retrieves comments for a specific article, paginated.
     *
     * @param articleId The ID of the article.
     * @param page The page number.
     * @param size The page size.
     * @return A Page of comments.
     */
    Page<CommentDTO> getCommentsByArticle(Long articleId, int page, int size);

    /**
     * Deletes a comment owned by the specified user.
     *
     * @param articleId The ID of the article the comment belongs to.
     * @param commentId The ID of the comment to delete.
     * @param userId The ID of the user requesting deletion.
     */
    void deleteComment(Long articleId, Long commentId, Long userId);

    /**
     * Retrieves the latest analytics snapshot generated by the Spark job for an article.
     *
     * @param articleId The ID of the article.
     * @return Latest metrics or {@code null} when no snapshot exists yet.
     */
    ArticleMetricsDTO getLatestArticleMetrics(Long articleId);

    /**
     * Returns the hottest articles within the provided time window, ordered by aggregated hot score.
     *
     * @param days  Time window in days (inclusive of today).
     * @param limit Maximum number of articles to return.
     * @return Ordered list of hot articles.
     */
    java.util.List<HotArticleDTO> getHotArticles(int days, int limit);
}
