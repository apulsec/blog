package com.example.blog.user.service;

import com.example.blog.user.dto.CreateNotificationRequest;
import com.example.blog.user.dto.NotificationDTO;
import java.util.List;

/**
 * Business contract for managing user notifications.
 */
public interface NotificationService {

    /**
     * Creates a new notification for the specified recipient.
     *
     * @param request notification payload from internal services
     * @return persisted notification details
     */
    NotificationDTO createNotification(CreateNotificationRequest request);

    /**
     * Retrieves notifications for the given user ordered by creation time (desc).
     *
     * @param userId recipient ID
     * @param unreadOnly filter to only unread notifications when true
     * @param limit maximum number of results to return (null for default)
     * @return list of notifications formatted for API responses
     */
    List<NotificationDTO> getNotificationsForUser(Long userId, boolean unreadOnly, Integer limit);

    /** Marks the specified notification as read if it belongs to the user. */
    void markAsRead(Long userId, Long notificationId);

    /** Marks all unread notifications for the user as read. */
    void markAllAsRead(Long userId);
}
