package com.example.blog.user.messaging;

import com.example.blog.user.dto.CreateNotificationRequest;
import com.example.blog.user.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArticleNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ArticleNotificationListener listener;

    @Test
    void handleArticleNotification_shouldInvokeNotificationService() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(10L);
        request.setActorId(20L);
        request.setArticleId(30L);
        request.setType("ARTICLE_LIKE");

        listener.handleArticleNotification(request);

        verify(notificationService).createNotification(request);
    }

    @Test
    void handleArticleNotification_whenPayloadNull_shouldSkip() {
        listener.handleArticleNotification(null);

        verify(notificationService, never()).createNotification(null);
    }
}
