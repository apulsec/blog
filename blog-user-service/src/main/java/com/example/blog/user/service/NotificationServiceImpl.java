package com.example.blog.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.blog.user.dto.CreateNotificationRequest;
import com.example.blog.user.dto.NotificationDTO;
import com.example.blog.user.entity.Notification;
import com.example.blog.user.entity.User;
import com.example.blog.user.entity.UserAuth;
import com.example.blog.user.mapper.NotificationMapper;
import com.example.blog.user.mapper.UserAuthMapper;
import com.example.blog.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link NotificationService} backed by MyBatis-Plus.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   UserMapper userMapper,
                                   UserAuthMapper userAuthMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.userAuthMapper = userAuthMapper;
    }

    @Override
    @Transactional
    public NotificationDTO createNotification(CreateNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setActorId(request.getActorId());
        notification.setArticleId(request.getArticleId());
        notification.setArticleTitle(request.getArticleTitle());
        notification.setType(request.getType());
        notification.setContent(resolveContent(request));
        notification.setRead(Boolean.FALSE);
        notification.setCreatedAt(LocalDateTime.now());

        notificationMapper.insert(notification);
        List<NotificationDTO> dtos = enrich(Collections.singletonList(notification));
        if (dtos.isEmpty()) {
            throw new IllegalStateException("Failed to map notification DTO");
        }
        return dtos.get(0);
    }

    @Override
    public List<NotificationDTO> getNotificationsForUser(Long userId, boolean unreadOnly, Integer limit) {
        int effectiveLimit = limit != null ? Math.min(Math.max(limit, 1), MAX_LIMIT) : DEFAULT_LIMIT;

        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (unreadOnly) {
            wrapper.eq("is_read", false);
        }
        wrapper.orderByDesc("created_at");
        wrapper.last("LIMIT " + effectiveLimit);

        List<Notification> notifications = notificationMapper.selectList(wrapper);
        if (notifications.isEmpty()) {
            return Collections.emptyList();
        }
        return enrich(notifications);
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        UpdateWrapper<Notification> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", notificationId)
               .eq("user_id", userId)
               .set("is_read", true);

        int updated = notificationMapper.update(null, wrapper);
        if (updated == 0) {
            throw new IllegalArgumentException("Notification not found for user");
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        UpdateWrapper<Notification> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("is_read", false)
               .set("is_read", true);
        notificationMapper.update(null, wrapper);
    }

    private String resolveContent(CreateNotificationRequest request) {
        if (StringUtils.hasText(request.getContent())) {
            return request.getContent();
        }

        String actorUsername = resolveUsername(request.getActorId());
        String articleTitle = StringUtils.hasText(request.getArticleTitle())
                ? "《" + request.getArticleTitle() + "》"
                : "你的文章";

        String actorPart = StringUtils.hasText(actorUsername) ? actorUsername : "有人";

        return switch (request.getType()) {
            case "ARTICLE_LIKE" -> actorPart + "点赞了" + articleTitle;
            case "ARTICLE_COMMENT" -> actorPart + "评论了" + articleTitle;
            default -> actorPart + "有一条新的通知";
        };
    }

    private String resolveUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        UserAuth userAuth = userAuthMapper.selectOne(new QueryWrapper<UserAuth>()
                .eq("user_id", userId)
                .eq("identity_type", "username"));
        return userAuth != null ? userAuth.getIdentifier() : null;
    }

    private List<NotificationDTO> enrich(List<Notification> notifications) {
        Set<Long> actorIds = notifications.stream()
                .map(Notification::getActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = actorIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(actorIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, String> usernameMap;
        if (actorIds.isEmpty()) {
            usernameMap = Collections.emptyMap();
        } else {
        QueryWrapper<UserAuth> authWrapper = new QueryWrapper<>();
        authWrapper.in("user_id", actorIds)
            .eq("identity_type", "username");
            List<UserAuth> authList = userAuthMapper.selectList(authWrapper);
            usernameMap = authList.stream()
                    .collect(Collectors.toMap(UserAuth::getUserId, UserAuth::getIdentifier, (first, second) -> first));
        }

        return notifications.stream()
                .map(notification -> mapToDTO(notification, usernameMap, userMap))
                .collect(Collectors.toList());
    }

    private NotificationDTO mapToDTO(Notification notification,
                                     Map<Long, String> usernameMap,
                                     Map<Long, User> userMap) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setActorId(notification.getActorId());
        dto.setArticleId(notification.getArticleId());
        dto.setArticleTitle(notification.getArticleTitle());
        dto.setType(notification.getType());
        dto.setContent(notification.getContent());
        dto.setRead(Boolean.TRUE.equals(notification.getRead()));
        dto.setCreatedAt(notification.getCreatedAt());

        Long actorId = notification.getActorId();
        if (actorId != null) {
            dto.setActorUsername(usernameMap.get(actorId));
            User user = userMap.get(actorId);
            if (user != null) {
                dto.setActorAvatarUrl(user.getAvatarUrl());
            }
        }

        return dto;
    }
}
