package com.example.blog.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.article.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for Tag entity.
 * Provides CRUD operations for the t_tag table.
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
