package com.example.blog.article.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.article.dto.ArticleSummaryDTO;
import com.example.blog.article.dto.CreateArticleRequest;
import com.example.blog.article.entity.Article;
import com.example.blog.article.entity.Tag;
import com.example.blog.article.service.ArticleService;
import com.example.blog.article.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for article-related endpoints.
 * Exposes APIs for retrieving and creating articles.
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private static final Logger log = LoggerFactory.getLogger(ArticleController.class);

    private final ArticleService articleService;
    private final JwtUtil jwtUtil;

    public ArticleController(ArticleService articleService, JwtUtil jwtUtil) {
        this.articleService = articleService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Retrieves a paginated list of published articles.
     * Each article includes author information fetched from blog-user-service.
     *
     * @param page Zero-based page number (default: 0)
     * @param size Number of items per page (default: 10)
     * @param status Optional status filter (PUBLISHED, DRAFT, etc.)
     * @param tag Optional tag name filter
     * @param keyword Optional keyword search (title or summary)
     * @param authorId Optional author ID filter
     * @return ResponseEntity containing a Page of ArticleSummaryDTO
     */
    @GetMapping
    public ResponseEntity<Page<ArticleSummaryDTO>> getArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long authorId) {
        
        // Convert zero-based page to one-based for MyBatis-Plus
        Page<Object> pageInfo = new Page<>(page + 1, size);
        
        Page<ArticleSummaryDTO> articles;
        if (keyword != null && !keyword.isEmpty()) {
            articles = articleService.searchArticles(pageInfo, keyword, authorId);
        } else if (tag != null && !tag.isEmpty()) {
            articles = articleService.getArticlesByTag(pageInfo, tag, authorId);
        } else if (status != null && !status.isEmpty()) {
            articles = articleService.getArticlesByStatus(pageInfo, status, authorId);
        } else if (authorId != null) {
            articles = articleService.getArticlesByAuthor(pageInfo, authorId);
        } else {
            articles = articleService.getPublishedArticles(pageInfo);
        }
        
        return ResponseEntity.ok(articles);
    }
    
    /**
     * Creates a new article.
     * Saves metadata to PostgreSQL and content to MongoDB.
     * The author ID is extracted from the JWT token, not from the request body.
     *
     * @param request CreateArticleRequest containing article details
     * @param authorization Authorization header containing JWT token
     * @return ResponseEntity containing the created Article
     */
    @PostMapping
    public ResponseEntity<Article> createArticle(
            @RequestBody CreateArticleRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        log.info("Received article creation request: title={}", request.getTitle());
        log.debug("Authorization header present: {}", authorization != null);
        
        // Extract user ID from JWT token
        Long authorId = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            log.debug("Extracting userId from JWT token");
            authorId = jwtUtil.getUserIdFromToken(token);
            log.info("Extracted authorId from token: {}", authorId);
        }
        
        if (authorId == null) {
            log.warn("No valid authorId found in JWT token, returning 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Set the author ID from the token (override any value from request)
        request.setAuthorId(authorId);
        log.debug("Creating article with authorId: {}", authorId);
        
        try {
            Article createdArticle = articleService.createArticle(request);
            log.info("Article created successfully: id={}, authorId={}", createdArticle.getId(), createdArticle.getAuthorId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdArticle);
        } catch (Exception e) {
            log.error("Failed to create article: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Retrieves a single article by ID.
     * Includes full content and author information.
     *
     * @param id Article ID
     * @return ResponseEntity containing ArticleSummaryDTO with content
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleSummaryDTO> getArticleById(@PathVariable Long id) {
        ArticleSummaryDTO article = articleService.getArticleById(id);
        return ResponseEntity.ok(article);
    }
    
    /**
     * Updates an existing article.
     * Updates both PostgreSQL metadata and MongoDB content.
     * Only the author can update their own article.
     *
     * @param id Article ID
     * @param request CreateArticleRequest containing updated article details
     * @param authorization Authorization header containing JWT token
     * @return ResponseEntity containing the updated Article
     */
    @PutMapping("/{id}")
    public ResponseEntity<Article> updateArticle(
            @PathVariable Long id,
            @RequestBody CreateArticleRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        // Extract user ID from JWT token
        Long userId = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            userId = jwtUtil.getUserIdFromToken(token);
        }
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Set the author ID (for ownership verification if needed)
        request.setAuthorId(userId);
        
        Article updatedArticle = articleService.updateArticle(id, request);
        return ResponseEntity.ok(updatedArticle);
    }
    
    /**
     * Deletes an article by ID.
     * Removes from both PostgreSQL and MongoDB.
     *
     * @param id Article ID
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Retrieves all available tags.
     *
     * @return ResponseEntity containing list of tags
     */
    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = articleService.getAllTags();
        return ResponseEntity.ok(tags);
    }
}