package com.example.blog.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.article.entity.ArticleTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for ArticleTag entity.
 * Provides CRUD operations for the t_article_tag table.
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {
}
