package com.example.blog.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.user.entity.User;
import org.springframework.stereotype.Repository;

/**
 * MyBatis-Plus Mapper interface for User entity.
 * Provides CRUD operations for the t_user table in PostgreSQL.
 * 
 * By extending BaseMapper, this interface automatically inherits
 * common database operations without requiring custom SQL or XML mappers.
 */
@Repository
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus provides all basic CRUD methods automatically.
    // Custom queries can be added here if needed, e.g.:
    // User findByUsername(String username);
}
