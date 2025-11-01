package com.example.blog.user.service;

import com.example.blog.user.dto.CreateNotificationRequest;
import com.example.blog.user.dto.NotificationDTO;
import com.example.blog.user.entity.Notification;
import com.example.blog.user.entity.User;
import com.example.blog.user.entity.UserAuth;
import com.example.blog.user.mapper.NotificationMapper;
import com.example.blog.user.mapper.UserAuthMapper;
import com.example.blog.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserAuthMapper userAuthMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void createNotification_returnsEnrichedDto() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(10L);
        request.setActorId(3L);
        request.setArticleId(55L);
        request.setArticleTitle("Spring Tips");
        request.setType("ARTICLE_LIKE");

        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(100L);
            notification.setCreatedAt(LocalDateTime.of(2025, 1, 1, 12, 0));
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));

        User actor = new User();
        actor.setId(3L);
        actor.setAvatarUrl("avatar.png");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(actor));

        UserAuth userAuth = new UserAuth();
        userAuth.setUserId(3L);
        userAuth.setIdentifier("actor-user");
        when(userAuthMapper.selectList(any())).thenReturn(List.of(userAuth));

        NotificationDTO dto = notificationService.createNotification(request);

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getUserId()).isEqualTo(10L);
        assertThat(dto.getActorId()).isEqualTo(3L);
        assertThat(dto.getArticleId()).isEqualTo(55L);
        assertThat(dto.getActorUsername()).isEqualTo("actor-user");
        assertThat(dto.getActorAvatarUrl()).isEqualTo("avatar.png");
        assertThat(dto.getType()).isEqualTo("ARTICLE_LIKE");
        assertThat(dto.isRead()).isFalse();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getActorId()).isEqualTo(3L);
    }

    @Test
    void getNotificationsForUser_returnsEnrichedList() {
        Notification notification = new Notification();
        notification.setId(5L);
        notification.setUserId(10L);
        notification.setActorId(3L);
        notification.setArticleId(55L);
        notification.setArticleTitle("Spring Tips");
        notification.setType("ARTICLE_COMMENT");
        notification.setContent("Nice post!");
        notification.setRead(Boolean.FALSE);
        notification.setCreatedAt(LocalDateTime.of(2025, 1, 2, 9, 30));

        when(notificationMapper.selectList(any())).thenReturn(List.of(notification));

        User actor = new User();
        actor.setId(3L);
        actor.setAvatarUrl("avatar.png");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(actor));

        UserAuth userAuth = new UserAuth();
        userAuth.setUserId(3L);
        userAuth.setIdentifier("actor-user");
        when(userAuthMapper.selectList(any())).thenReturn(List.of(userAuth));

        List<NotificationDTO> notifications = notificationService.getNotificationsForUser(10L, true, 10);

        assertThat(notifications).hasSize(1);
        NotificationDTO dto = notifications.get(0);
        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getType()).isEqualTo("ARTICLE_COMMENT");
        assertThat(dto.getActorUsername()).isEqualTo("actor-user");
        assertThat(dto.getActorAvatarUrl()).isEqualTo("avatar.png");
        assertThat(dto.isRead()).isFalse();
    }

    @Test
    void markAsRead_whenUpdateReturnsZero_shouldThrow() {
        when(notificationMapper.update(eq(null), any())).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> notificationService.markAsRead(10L, 99L));
    }

    @Test
    void markAllAsRead_shouldInvokeMapperUpdate() {
        when(notificationMapper.update(eq(null), any())).thenReturn(2);

        notificationService.markAllAsRead(10L);

        verify(notificationMapper).update(eq(null), any());
    }
}
