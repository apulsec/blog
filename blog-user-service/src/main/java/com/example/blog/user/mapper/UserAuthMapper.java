package com.example.blog.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.user.entity.UserAuth;
import org.springframework.stereotype.Repository;

/**
 * MyBatis-Plus Mapper interface for UserAuth entity.
 * Provides CRUD operations for the t_user_auth table in PostgreSQL.
 * 
 * This mapper is used to manage authentication credentials,
 * including password hashes and authentication identifiers.
 */
@Repository
public interface UserAuthMapper extends BaseMapper<UserAuth> {
    // MyBatis-Plus provides all basic CRUD methods automatically.
    // Custom queries can be added here if needed, e.g.:
    // UserAuth findByIdentifierAndIdentityType(String identifier, String identityType);
}
