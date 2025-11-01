package com.example.blog.article.service;

import com.example.blog.article.client.NotificationClient;
import com.example.blog.article.client.UserServiceClient;
import com.example.blog.article.dto.CommentDTO;
import com.example.blog.article.dto.CreateNotificationRequest;
import com.example.blog.article.dto.UserDTO;
import com.example.blog.article.entity.Article;
import com.example.blog.article.entity.ArticleLike;
import com.example.blog.article.entity.Comment;
import com.example.blog.article.mapper.ArticleMapper;
import com.example.blog.article.mapper.ArticleTagMapper;
import com.example.blog.article.mapper.TagMapper;
import com.example.blog.article.repository.ArticleContentRepository;
import com.example.blog.article.repository.ArticleLikeRepository;
import com.example.blog.article.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private ArticleContentRepository articleContentRepository;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private ArticleTagMapper articleTagMapper;

    @Mock
    private ArticleLikeRepository articleLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Test
    void likeArticle_shouldCreateNotificationForAuthor() {
        Article article = new Article();
        article.setId(12L);
        article.setAuthorId(42L);
        article.setTitle("Spring Patterns");

        when(articleMapper.selectById(12L)).thenReturn(article);
        when(articleLikeRepository.findByArticleIdAndUserId(12L, 7L)).thenReturn(Optional.empty());
        when(articleLikeRepository.save(any(ArticleLike.class))).thenAnswer(invocation -> invocation.getArgument(0));

        articleService.likeArticle(12L, 7L);

        verify(articleMapper).incrementLikesCount(12L);

        ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationClient).createNotification(captor.capture());
        CreateNotificationRequest request = captor.getValue();

        assertThat(request.getUserId()).isEqualTo(42L);
        assertThat(request.getActorId()).isEqualTo(7L);
        assertThat(request.getArticleId()).isEqualTo(12L);
        assertThat(request.getType()).isEqualTo("ARTICLE_LIKE");
        assertThat(request.getContent()).isNull();
    }

    @Test
    void likeArticle_whenArticleMissing_shouldThrow() {
        when(articleMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.likeArticle(99L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Article not found");

        verifyNoInteractions(notificationClient);
    }

    @Test
    void likeArticle_shouldSkipNotificationWhenActorIsAuthor() {
        Article article = new Article();
        article.setId(33L);
        article.setAuthorId(5L);
        article.setTitle("DDD Intro");

        when(articleMapper.selectById(33L)).thenReturn(article);
        when(articleLikeRepository.findByArticleIdAndUserId(33L, 5L)).thenReturn(Optional.empty());
        when(articleLikeRepository.save(any(ArticleLike.class))).thenAnswer(invocation -> invocation.getArgument(0));

        articleService.likeArticle(33L, 5L);

        verify(articleMapper).incrementLikesCount(33L);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void createComment_shouldCreateNotificationWithPreview() {
        Article article = new Article();
        article.setId(50L);
        article.setAuthorId(10L);
        article.setTitle("Advanced Java");

        when(articleMapper.selectById(50L)).thenReturn(article);
    doNothing().when(articleMapper).incrementCommentsCount(50L);

        Comment saved = new Comment();
        saved.setId(200L);
        saved.setArticleId(50L);
        saved.setUserId(77L);
        saved.setContent("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());

        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        UserDTO actor = new UserDTO();
        actor.setId(77L);
        actor.setUsername("reader77");
        actor.setAvatarUrl("avatar.png");
        when(userServiceClient.getUserById(77L)).thenReturn(actor);

        CommentDTO dto = articleService.createComment(50L, 77L, null,
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                        "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");

        assertThat(dto.getId()).isEqualTo(200L);
        assertThat(dto.getArticleId()).isEqualTo(50L);
        assertThat(dto.getAuthor().getUsername()).isEqualTo("reader77");

        ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationClient).createNotification(captor.capture());
        CreateNotificationRequest request = captor.getValue();

        assertThat(request.getUserId()).isEqualTo(10L);
        assertThat(request.getActorId()).isEqualTo(77L);
        assertThat(request.getArticleId()).isEqualTo(50L);
        assertThat(request.getType()).isEqualTo("ARTICLE_COMMENT");
    assertThat(request.getContent()).hasSize(123)
        .endsWith("...");
    }

    @Test
    void createComment_whenArticleMissing_shouldThrow() {
        when(articleMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.createComment(1L, 2L, null, "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Article not found");

        verifyNoInteractions(notificationClient);
    }

    @Test
    void createComment_shouldSkipNotificationWhenActorIsAuthor() {
        Article article = new Article();
        article.setId(60L);
        article.setAuthorId(77L);
        article.setTitle("JPA Mappings");

        when(articleMapper.selectById(60L)).thenReturn(article);
    doNothing().when(articleMapper).incrementCommentsCount(60L);

        Comment saved = new Comment();
        saved.setId(301L);
        saved.setArticleId(60L);
        saved.setUserId(77L);
        saved.setContent("Thanks for reading");
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        UserDTO actor = new UserDTO();
        actor.setId(77L);
        actor.setUsername("author");
        when(userServiceClient.getUserById(77L)).thenReturn(actor);

        articleService.createComment(60L, 77L, null, "Thanks for reading");

        verifyNoInteractions(notificationClient);
    }
}
