package com.example.blog.user.controller;

import com.example.blog.user.dto.CreateNotificationRequest;
import com.example.blog.user.dto.NotificationDTO;
import com.example.blog.user.service.NotificationService;
import com.example.blog.user.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST endpoints for user notifications including internal creation API
 * and user-facing retrieval/acknowledgement operations.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    public NotificationController(NotificationService notificationService, JwtUtil jwtUtil) {
        this.notificationService = notificationService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Internal endpoint for other services (e.g., article-service) to create notifications.
     */
    @PostMapping("/internal")
    public ResponseEntity<NotificationDTO> createNotification(@Valid @RequestBody CreateNotificationRequest request) {
        NotificationDTO dto = notificationService.createNotification(request);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    /**
     * Returns the authenticated user's notifications ordered by most recent first.
     */
    @GetMapping("/me")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @RequestParam(name = "limit", required = false) Integer limit) {

        Long userId = resolveUserId(authorizationHeader);
        List<NotificationDTO> notifications = notificationService.getNotificationsForUser(userId, unreadOnly, limit);
        return ResponseEntity.ok(notifications);
    }

    /** Marks a single notification as read for the authenticated user. */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable Long notificationId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        Long userId = resolveUserId(authorizationHeader);
        try {
            notificationService.markAsRead(userId, notificationId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
        return ResponseEntity.noContent().build();
    }

    /** Marks all unread notifications as read for the authenticated user. */
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        Long userId = resolveUserId(authorizationHeader);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        return userId;
    }
}
