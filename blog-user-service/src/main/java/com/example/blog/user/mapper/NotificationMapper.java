package com.example.blog.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.user.entity.Notification;
import org.springframework.stereotype.Repository;

/**
 * MyBatis-Plus mapper providing CRUD access for Notification entities.
 */
@Repository
public interface NotificationMapper extends BaseMapper<Notification> {
    // BaseMapper supplies default insert/select/update/delete implementations.
}
