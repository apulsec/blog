package com.example.blog.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.article.entity.Article;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
 * MyBatis-Plus mapper interface for Article entity.
 * Provides CRUD operations for the t_article table in PostgreSQL.
 * 
 * BaseMapper provides common methods like:
 * - selectById, selectList, selectPage
 * - insert, updateById, deleteById
 */
@Repository
public interface ArticleMapper extends BaseMapper<Article> {
    @Update("UPDATE t_article SET likes_count = likes_count + 1 WHERE id = #{articleId}")
    void incrementLikesCount(@Param("articleId") Long articleId);

    @Update("UPDATE t_article SET likes_count = GREATEST(0, likes_count - 1) WHERE id = #{articleId}")
    void decrementLikesCount(@Param("articleId") Long articleId);

    @Update("UPDATE t_article SET comments_count = comments_count + 1 WHERE id = #{articleId}")
    void incrementCommentsCount(@Param("articleId") Long articleId);

    @Update("UPDATE t_article SET comments_count = GREATEST(0, comments_count - 1) WHERE id = #{articleId}")
    void decrementCommentsCount(@Param("articleId") Long articleId);
}
