package com.example.blog.article.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.article.client.UserServiceClient;
import com.example.blog.article.dto.ArticleSummaryDTO;
import com.example.blog.article.dto.CreateArticleRequest;
import com.example.blog.article.dto.CommentDTO;
import com.example.blog.article.dto.CreateNotificationRequest;
import com.example.blog.article.dto.UserDTO;
import com.example.blog.article.entity.Article;
import com.example.blog.article.entity.ArticleContent;
import com.example.blog.article.entity.ArticleTag;
import com.example.blog.article.entity.Tag;
import com.example.blog.article.mapper.ArticleMapper;
import com.example.blog.article.mapper.ArticleTagMapper;
import com.example.blog.article.mapper.TagMapper;
import com.example.blog.article.messaging.ArticleNotificationPublisher;
import com.example.blog.article.repository.ArticleContentRepository;
import com.example.blog.article.repository.ArticleLikeRepository;
import com.example.blog.article.repository.CommentRepository;
import com.example.blog.article.entity.ArticleLike;
import com.example.blog.article.entity.Comment;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * Implementation of ArticleService.
 * Handles the core business logic for article retrieval and aggregation with author information.
 * 
 * This service demonstrates:
 * - Pagination with MyBatis-Plus
 * - Service-to-service communication with OpenFeign
 * - Circuit breaker pattern with Resilience4j for fault tolerance
 */
@Service
public class ArticleServiceImpl implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);
    private static final String NOTIFICATION_TYPE_LIKE = "ARTICLE_LIKE";
    private static final String NOTIFICATION_TYPE_COMMENT = "ARTICLE_COMMENT";

    private final ArticleMapper articleMapper;
    private final UserServiceClient userServiceClient;
    private final ArticleNotificationPublisher notificationPublisher;
    private final ArticleContentRepository articleContentRepository;
    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleLikeRepository articleLikeRepository;
    private final CommentRepository commentRepository;

    public ArticleServiceImpl(ArticleMapper articleMapper,
                              UserServiceClient userServiceClient,
                              ArticleNotificationPublisher notificationPublisher,
                              ArticleContentRepository articleContentRepository,
                              TagMapper tagMapper,
                              ArticleTagMapper articleTagMapper,
                              ArticleLikeRepository articleLikeRepository,
                              CommentRepository commentRepository) {
        this.articleMapper = articleMapper;
        this.userServiceClient = userServiceClient;
    this.notificationPublisher = notificationPublisher;
        this.articleContentRepository = articleContentRepository;
        this.tagMapper = tagMapper;
        this.articleTagMapper = articleTagMapper;
        this.articleLikeRepository = articleLikeRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Retrieves published articles with pagination and enriches them with author information.
     * 
     * Implementation steps:
     * 1. Query published articles (status = 1) from PostgreSQL
     * 2. Extract unique author IDs
     * 3. Fetch author information from blog-user-service (with circuit breaker protection)
     * 4. Combine article and author data into DTOs
     * 5. Return paginated results
     *
     * @param page Page object containing current page and page size
     * @return Page of ArticleSummaryDTO with complete article and author information
     */
    @Override
    public Page<ArticleSummaryDTO> getPublishedArticles(Page<Object> page) {
        // Step 1: Query published articles with pagination
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "PUBLISHED"); // Only published articles
        queryWrapper.orderByDesc("publish_time"); // Latest first
        
        Page<Article> articlePage = articleMapper.selectPage(
            new Page<>(page.getCurrent(), page.getSize()), 
            queryWrapper
        );

        List<Article> articles = articlePage.getRecords();
        if (articles.isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // Step 2: Extract unique author IDs
        List<Long> authorIds = articles.stream()
                .map(Article::getAuthorId)
                .distinct()
                .collect(Collectors.toList());

        // Step 3: Fetch author information with circuit breaker protection
        Map<Long, UserDTO> authorMap = authorIds.stream()
                .collect(Collectors.toMap(
                        authorId -> authorId,
                        this::getAuthorInfoWithCircuitBreaker
                ));

        // Step 4: Combine article and author data
        List<ArticleSummaryDTO> dtos = articles.stream().map(article -> {
            ArticleSummaryDTO dto = new ArticleSummaryDTO();
            dto.setId(article.getId());
            dto.setTitle(article.getTitle());
            dto.setSummary(article.getSummary());
            dto.setCoverImageUrl(article.getCoverImageUrl());
            dto.setPublishTime(article.getPublishTime());
            dto.setStatus(article.getStatus());
            dto.setLikesCount(article.getLikesCount() != null ? article.getLikesCount() : 0);
            dto.setCommentsCount(article.getCommentsCount() != null ? article.getCommentsCount() : 0);

            UserDTO user = authorMap.get(article.getAuthorId());
            if (user != null) {
                ArticleSummaryDTO.AuthorDTO authorDTO = new ArticleSummaryDTO.AuthorDTO();
                authorDTO.setId(user.getId());
                authorDTO.setUsername(user.getUsername());
                authorDTO.setAvatarUrl(user.getAvatarUrl());
                dto.setAuthor(authorDTO);
            }
            
            // Load tags for this article
            dto.setTags(loadArticleTags(article.getId()));
            
            return dto;
        }).collect(Collectors.toList());

        // Step 5: Build and return paginated result
        Page<ArticleSummaryDTO> resultPage = new Page<>(
            articlePage.getCurrent(), 
            articlePage.getSize(), 
            articlePage.getTotal()
        );
        resultPage.setRecords(dtos);
        return resultPage;
    }

    /**
     * Fetches author information with circuit breaker protection.
     * The circuit breaker prevents cascading failures when blog-user-service is down.
     *
     * @param userId The ID of the author to fetch
     * @return UserDTO containing author information
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "getAuthorInfoFallback")
    public UserDTO getAuthorInfoWithCircuitBreaker(Long userId) {
        return userServiceClient.getUserById(userId);
    }

    /**
     * Fallback method invoked when the circuit breaker opens.
     * Provides default author information to maintain service availability.
     *
     * @param userId The ID of the author that was being fetched
     * @param t The throwable that triggered the fallback
     * @return Default UserDTO with "Unknown Author" information
     */
    public UserDTO getAuthorInfoFallback(Long userId, Throwable t) {
        log.error("Circuit breaker opened for userService. Falling back for user ID: {}. Error: {}", 
                  userId, t.getMessage());
        UserDTO defaultUser = new UserDTO();
        defaultUser.setId(userId);
    defaultUser.setUsername("Unknown Author");
        defaultUser.setAvatarUrl("default-avatar.png");
        return defaultUser;
    }
    
    /**
     * Creates a new article with metadata in PostgreSQL and content in MongoDB.
     * 
     * @param request CreateArticleRequest containing article details
     * @return Created Article entity
     */
    @Override
    @Transactional
    public Article createArticle(CreateArticleRequest request) {
        // Step 1: Create article metadata entity
        Article article = new Article();
        article.setAuthorId(request.getAuthorId());
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setCoverImageUrl(request.getCoverImageUrl());
        article.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");
        article.setPublishTime(LocalDateTime.now());
    article.setViewCount(0);
    article.setLikesCount(0);
    article.setCommentsCount(0);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        
        // Step 2: Save to PostgreSQL
        articleMapper.insert(article);
        
        // Step 3: Save content to MongoDB
        if (request.getContent() != null && !request.getContent().isEmpty()) {
            ArticleContent content = new ArticleContent();
            content.setArticleId(article.getId().toString());
            content.setContent(request.getContent());
            content.setCreatedAt(LocalDateTime.now());
            content.setUpdatedAt(LocalDateTime.now());
            articleContentRepository.save(content);
        }
        
        // Step 4: Save tags
        saveArticleTags(article.getId(), request.getTags());
        
        log.info("Created new article with ID: {} by author: {}", article.getId(), article.getAuthorId());
        return article;
    }
    
    /**
     * Retrieves a single article by ID with its content.
     */
    @Override
    public ArticleSummaryDTO getArticleById(Long id) {
        // Step 1: Get article metadata
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("Article not found with ID: " + id);
        }
        
        // Step 2: Get article content from MongoDB
        ArticleContent content = articleContentRepository
            .findByArticleId(id.toString())
            .orElse(null);
        
        // Step 3: Get author information
        UserDTO author = getAuthorInfoWithCircuitBreaker(article.getAuthorId());
        
        // Step 4: Build DTO
        ArticleSummaryDTO dto = new ArticleSummaryDTO();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setSummary(article.getSummary());
        dto.setCoverImageUrl(article.getCoverImageUrl());
        dto.setPublishTime(article.getPublishTime());
        dto.setStatus(article.getStatus());
    dto.setLikesCount(article.getLikesCount() != null ? article.getLikesCount() : 0);
    dto.setCommentsCount(article.getCommentsCount() != null ? article.getCommentsCount() : 0);
        
        if (author != null) {
            ArticleSummaryDTO.AuthorDTO authorDTO = new ArticleSummaryDTO.AuthorDTO();
            authorDTO.setId(author.getId());
            authorDTO.setUsername(author.getUsername());
            authorDTO.setAvatarUrl(author.getAvatarUrl());
            dto.setAuthor(authorDTO);
        }
        
        dto.setContent(content != null ? content.getContent() : "");
        dto.setTags(loadArticleTags(article.getId()));
        
        return dto;
    }
    
    /**
     * Updates an existing article.
     */
    @Override
    @Transactional
    public Article updateArticle(Long id, CreateArticleRequest request) {
        // Step 1: Get existing article
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("Article not found with ID: " + id);
        }
        
        // Step 2: Update article metadata
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setCoverImageUrl(request.getCoverImageUrl());
        article.setStatus(request.getStatus());
        article.setUpdatedAt(LocalDateTime.now());
        
        // Update in PostgreSQL
        articleMapper.updateById(article);
        
        // Step 3: Update content in MongoDB
        if (request.getContent() != null) {
            ArticleContent content = articleContentRepository
                .findByArticleId(id.toString())
                .orElse(new ArticleContent());
            
            content.setArticleId(id.toString());
            content.setContent(request.getContent());
            content.setUpdatedAt(LocalDateTime.now());
            
            if (content.getCreatedAt() == null) {
                content.setCreatedAt(LocalDateTime.now());
            }
            
            articleContentRepository.save(content);
        }
        
        // Step 4: Update tags - remove old ones and add new ones
        QueryWrapper<ArticleTag> atWrapper = new QueryWrapper<>();
        atWrapper.eq("article_id", id);
        articleTagMapper.delete(atWrapper);
        
        saveArticleTags(id, request.getTags());
        
        log.info("Updated article with ID: {}", id);
        return article;
    }
    
    /**
     * Deletes an article by ID.
     */
    @Override
    @Transactional
    public void deleteArticle(Long id) {
        // Step 1: Delete article-tag relationships
        QueryWrapper<ArticleTag> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", id);
        articleTagMapper.delete(wrapper);
        
        // Step 2: Delete from PostgreSQL
        int deleted = articleMapper.deleteById(id);
        if (deleted == 0) {
            throw new RuntimeException("Article not found with ID: " + id);
        }
        
        // Step 3: Delete content from MongoDB
        articleContentRepository.deleteByArticleId(id.toString());
        
        log.info("Deleted article with ID: {}", id);
    }
    
    /**
     * Retrieves articles by status (PUBLISHED, DRAFT, etc.).
     */
    @Override
    public Page<ArticleSummaryDTO> getArticlesByStatus(Page<Object> page, String status, Long authorId) {
        // Query articles by status
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("status", status);
        if (authorId != null) {
            wrapper.eq("author_id", authorId);
        }
        wrapper.orderByDesc("publish_time");
        
        Page<Article> articlePage = new Page<>(page.getCurrent(), page.getSize());
        articleMapper.selectPage(articlePage, wrapper);
        
        // Use same logic as getPublishedArticles but with different query
        return buildArticleDTOPage(articlePage);
    }
    
    /**
     * Retrieves articles by tag name.
     */
    @Override
    public Page<ArticleSummaryDTO> getArticlesByTag(Page<Object> page, String tagName, Long authorId) {
        // Step 1: Find tag by name
        QueryWrapper<Tag> tagWrapper = new QueryWrapper<>();
        tagWrapper.eq("name", tagName);
        Tag tag = tagMapper.selectOne(tagWrapper);
        
        if (tag == null) {
            // Return empty page if tag doesn't exist
            Page<ArticleSummaryDTO> emptyPage = new Page<>(page.getCurrent(), page.getSize(), 0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }
        
        // Step 2: Find article IDs with this tag
        QueryWrapper<ArticleTag> atWrapper = new QueryWrapper<>();
        atWrapper.eq("tag_id", tag.getId());
        List<ArticleTag> articleTags = articleTagMapper.selectList(atWrapper);
        
        if (articleTags.isEmpty()) {
            Page<ArticleSummaryDTO> emptyPage = new Page<>(page.getCurrent(), page.getSize(), 0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }
        
        List<Long> articleIds = articleTags.stream()
            .map(ArticleTag::getArticleId)
            .collect(Collectors.toList());
        
        // Step 3: Query articles
        QueryWrapper<Article> articleWrapper = new QueryWrapper<>();
        articleWrapper.in("id", articleIds);
        if (authorId != null) {
            articleWrapper.eq("author_id", authorId);
        }
        articleWrapper.orderByDesc("publish_time");
        
        Page<Article> articlePage = new Page<>(page.getCurrent(), page.getSize());
        articleMapper.selectPage(articlePage, articleWrapper);
        
        return buildArticleDTOPage(articlePage);
    }
    
    /**
     * Retrieves all available tags.
     */
    @Override
    public List<Tag> getAllTags() {
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("name");
        return tagMapper.selectList(wrapper);
    }
    
    /**
     * Search articles by keyword in title or summary.
     * 
     * @param page Page object containing pagination parameters
     * @param keyword Search keyword
     * @param authorId Optional author ID filter
     * @return Page of ArticleSummaryDTO matching the keyword
     */
    @Override
    public Page<ArticleSummaryDTO> searchArticles(Page<Object> page, String keyword, Long authorId) {
        // Build query wrapper for keyword search
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        
        // Search in title or summary (case-insensitive)
        queryWrapper.and(wrapper -> wrapper
            .like("title", keyword)
            .or()
            .like("summary", keyword)
        );
        
        if (authorId != null) {
            queryWrapper.eq("author_id", authorId);
        }
        
        // Order by relevance (title matches first) and publish time
        queryWrapper.orderByDesc("publish_time");
        
        Page<Article> articlePage = new Page<>(page.getCurrent(), page.getSize());
        articleMapper.selectPage(articlePage, queryWrapper);
        
        return buildArticleDTOPage(articlePage);
    }
    
    /**
     * Retrieves articles by author ID.
     */
    @Override
    public Page<ArticleSummaryDTO> getArticlesByAuthor(Page<Object> page, Long authorId) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("author_id", authorId);
        wrapper.orderByDesc("publish_time");
        
        Page<Article> articlePage = new Page<>(page.getCurrent(), page.getSize());
        articleMapper.selectPage(articlePage, wrapper);
        
        return buildArticleDTOPage(articlePage);
    }
    
    /**
     * Helper method to load tags for an article.
     */
    private List<ArticleSummaryDTO.TagDTO> loadArticleTags(Long articleId) {
        // Get article-tag relationships
        QueryWrapper<ArticleTag> atWrapper = new QueryWrapper<>();
        atWrapper.eq("article_id", articleId);
        List<ArticleTag> articleTags = articleTagMapper.selectList(atWrapper);
        
        if (articleTags.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Get tag IDs
        List<Long> tagIds = articleTags.stream()
            .map(ArticleTag::getTagId)
            .collect(Collectors.toList());
        
        // Get tags
        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        
        // Convert to DTOs
        return tags.stream()
            .map(tag -> new ArticleSummaryDTO.TagDTO(tag.getId(), tag.getName(), tag.getColor()))
            .collect(Collectors.toList());
    }
    
    /**
     * Helper method to find or create a tag.
     */
    private Tag findOrCreateTag(String tagName) {
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        wrapper.eq("name", tagName);
        Tag tag = tagMapper.selectOne(wrapper);
        
        if (tag == null) {
            tag = new Tag();
            tag.setName(tagName);
            tag.setColor(generateTagColor(tagName));
            tag.setCreatedAt(LocalDateTime.now());
            tag.setUpdatedAt(LocalDateTime.now());
            tagMapper.insert(tag);
        }
        
        return tag;
    }
    
    /**
     * Generates a consistent color for a tag based on its name.
     */
    private String generateTagColor(String tagName) {
        String[] colors = {"#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899"};
        int index = Math.abs(tagName.hashCode()) % colors.length;
        return colors[index];
    }
    
    /**
     * Helper method to save article tags.
     */
    private void saveArticleTags(Long articleId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        
        for (String tagName : tagNames) {
            Tag tag = findOrCreateTag(tagName);
            
            // Create article-tag relationship
            ArticleTag articleTag = new ArticleTag();
            articleTag.setArticleId(articleId);
            articleTag.setTagId(tag.getId());
            articleTag.setCreatedAt(LocalDateTime.now());
            
            articleTagMapper.insert(articleTag);
        }
    }
    
    /**
     * Helper method to build ArticleSummaryDTO page from Article page.
     */
    private Page<ArticleSummaryDTO> buildArticleDTOPage(Page<Article> articlePage) {
        List<Article> articles = articlePage.getRecords();
        
        if (articles.isEmpty()) {
            Page<ArticleSummaryDTO> emptyPage = new Page<>(
                articlePage.getCurrent(), 
                articlePage.getSize(), 
                articlePage.getTotal()
            );
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }
        
        // Extract unique author IDs
        List<Long> authorIds = articles.stream()
                .map(Article::getAuthorId)
                .distinct()
                .collect(Collectors.toList());
        
        // Fetch author information with circuit breaker protection
        Map<Long, UserDTO> authorMap = authorIds.stream()
                .collect(Collectors.toMap(
                        authorId -> authorId,
                        this::getAuthorInfoWithCircuitBreaker
                ));
        
        // Combine article and author data
        List<ArticleSummaryDTO> dtos = articles.stream().map(article -> {
            ArticleSummaryDTO dto = new ArticleSummaryDTO();
            dto.setId(article.getId());
            dto.setTitle(article.getTitle());
            dto.setSummary(article.getSummary());
            dto.setCoverImageUrl(article.getCoverImageUrl());
            dto.setPublishTime(article.getPublishTime());
            dto.setStatus(article.getStatus());
            dto.setLikesCount(article.getLikesCount() != null ? article.getLikesCount() : 0);
            dto.setCommentsCount(article.getCommentsCount() != null ? article.getCommentsCount() : 0);
            
            UserDTO user = authorMap.get(article.getAuthorId());
            if (user != null) {
                ArticleSummaryDTO.AuthorDTO authorDTO = new ArticleSummaryDTO.AuthorDTO();
                authorDTO.setId(user.getId());
                authorDTO.setUsername(user.getUsername());
                authorDTO.setAvatarUrl(user.getAvatarUrl());
                dto.setAuthor(authorDTO);
            }
            
            // Load tags for this article
            dto.setTags(loadArticleTags(article.getId()));
            
            return dto;
        }).collect(Collectors.toList());
        
        // Build and return paginated result
        Page<ArticleSummaryDTO> resultPage = new Page<>(
            articlePage.getCurrent(), 
            articlePage.getSize(), 
            articlePage.getTotal()
        );
        resultPage.setRecords(dtos);
        return resultPage;
    }

    @Override
    @Transactional
    public void likeArticle(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("Article not found with ID: " + articleId);
        }

        if (articleLikeRepository.findByArticleIdAndUserId(articleId, userId).isPresent()) {
            return; // User has already liked this article
        }

        ArticleLike like = new ArticleLike();
        like.setArticleId(articleId);
        like.setUserId(userId);
        articleLikeRepository.save(like);

        // Update likes_count in articles table
        articleMapper.incrementLikesCount(articleId);

        sendArticleInteractionNotification(article, userId, NOTIFICATION_TYPE_LIKE, null);
    }

    @Override
    @Transactional
    public void unlikeArticle(Long articleId, Long userId) {
        articleLikeRepository.findByArticleIdAndUserId(articleId, userId).ifPresent(like -> {
            articleLikeRepository.delete(Objects.requireNonNull(like));
            // Update likes_count in articles table
            articleMapper.decrementLikesCount(articleId);
        });
    }

    @Override
    public boolean hasUserLikedArticle(Long articleId, Long userId) {
        return articleLikeRepository.findByArticleIdAndUserId(articleId, userId).isPresent();
    }

    @Override
    @Transactional
    public CommentDTO createComment(Long articleId, Long userId, Long parentId, String content) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("Article not found with ID: " + articleId);
        }

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);

        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            comment.setParent(parentComment);
        }

        Comment savedComment = commentRepository.save(comment);

        // Update comments_count in articles table
        articleMapper.incrementCommentsCount(articleId);

    sendArticleInteractionNotification(article, userId, NOTIFICATION_TYPE_COMMENT, buildCommentPreview(content));

        Map<Long, UserDTO> userCache = new HashMap<>();
        UserDTO author = getAuthorInfoWithCircuitBreaker(userId);
        if (author != null) {
            userCache.put(userId, author);
        }

        return mapCommentToDTO(savedComment, userCache);
    }

    @Override
    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CommentDTO> getCommentsByArticle(Long articleId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        org.springframework.data.domain.Page<Comment> commentPage =
                commentRepository.findByArticleIdAndParentIsNull(articleId, pageable);

        if (commentPage.isEmpty()) {
            return new PageImpl<>(Collections.<CommentDTO>emptyList(), pageable, commentPage.getTotalElements());
        }

        Set<Long> userIds = new HashSet<>();
        commentPage.getContent().forEach(comment -> collectCommentUserIds(comment, userIds));

        Map<Long, UserDTO> userCache = new HashMap<>();
        userIds.forEach(userId -> userCache.put(userId, getAuthorInfoWithCircuitBreaker(userId)));

        List<CommentDTO> dtoList = commentPage.getContent().stream()
                .map(comment -> mapCommentToDTO(comment, userCache))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, commentPage.getPageable(), commentPage.getTotalElements());
    }

    @Override
    @SuppressWarnings("null")
    @Transactional
    public void deleteComment(Long articleId, Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getArticleId().equals(articleId)) {
            throw new IllegalArgumentException("Comment does not belong to the specified article");
        }

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalStateException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
        articleMapper.decrementCommentsCount(articleId);
    }

    private void sendArticleInteractionNotification(Article article, Long actorId, String type, String content) {
        if (article == null) {
            return;
        }

        Long recipientId = article.getAuthorId();
        if (recipientId == null || Objects.equals(recipientId, actorId)) {
            return; // Do not notify when the actor is the author or recipient is missing
        }

        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(recipientId);
        request.setActorId(actorId);
        request.setArticleId(article.getId());
        request.setArticleTitle(article.getTitle());
        request.setType(type);
        if (StringUtils.hasText(content)) {
            request.setContent(content);
        }

        notificationPublisher.publish(request);
    }

    private String buildCommentPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.length() <= 120) {
            return trimmed;
        }
        return trimmed.substring(0, 120) + "...";
    }

    private void collectCommentUserIds(Comment comment, Set<Long> userIds) {
        if (comment == null) {
            return;
        }
        userIds.add(comment.getUserId());
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            comment.getReplies().forEach(reply -> collectCommentUserIds(reply, userIds));
        }
    }

    private CommentDTO mapCommentToDTO(Comment comment, Map<Long, UserDTO> userCache) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setArticleId(comment.getArticleId());
        dto.setUserId(comment.getUserId());
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());

        UserDTO userInfo = userCache.get(comment.getUserId());
        if (userInfo == null) {
            userInfo = getAuthorInfoWithCircuitBreaker(comment.getUserId());
            userCache.put(comment.getUserId(), userInfo);
        }

        if (userInfo != null) {
            CommentDTO.AuthorDTO authorDTO = new CommentDTO.AuthorDTO();
            authorDTO.setId(userInfo.getId());
            authorDTO.setUsername(userInfo.getUsername());
            authorDTO.setAvatarUrl(userInfo.getAvatarUrl());
            dto.setAuthor(authorDTO);
        }

        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentDTO> replyDtos = comment.getReplies().stream()
                    .sorted(Comparator.comparing(Comment::getCreatedAt))
                    .map(reply -> mapCommentToDTO(reply, userCache))
                    .collect(Collectors.toList());
            dto.setReplies(replyDtos);
        } else {
            dto.setReplies(Collections.emptyList());
        }

        return dto;
    }
}
