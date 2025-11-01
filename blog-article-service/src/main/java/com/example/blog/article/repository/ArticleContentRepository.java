package com.example.blog.article.repository;

import com.example.blog.article.entity.ArticleContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB repository for ArticleContent.
 * Provides CRUD operations for article content stored in MongoDB.
 */
@Repository
public interface ArticleContentRepository extends MongoRepository<ArticleContent, String> {
    
    /**
     * Finds article content by article ID.
     *
     * @param articleId The article ID from PostgreSQL
     * @return Optional containing ArticleContent if found
     */
    Optional<ArticleContent> findByArticleId(String articleId);
    
    /**
     * Deletes article content by article ID.
     *
     * @param articleId The article ID from PostgreSQL
     */
    void deleteByArticleId(String articleId);
}
