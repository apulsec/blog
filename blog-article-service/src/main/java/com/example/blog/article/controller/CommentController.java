package com.example.blog.article.controller;

import com.example.blog.article.dto.CommentDTO;
import com.example.blog.article.service.ArticleService;
import com.example.blog.article.util.JwtUtil;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/articles/{articleId}/comments")
public class CommentController {

    private final ArticleService articleService;
    private final JwtUtil jwtUtil;

    public CommentController(ArticleService articleService, JwtUtil jwtUtil) {
        this.articleService = articleService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<CommentDTO> createComment(
            @PathVariable Long articleId,
            @RequestBody Map<String, String> payload,
            @RequestHeader("Authorization") String token) {
        
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        String content = payload.get("content");
        Long parentId = payload.containsKey("parentId") ? Long.parseLong(payload.get("parentId")) : null;

        CommentDTO createdComment = articleService.createComment(articleId, userId, parentId, content);
        return ResponseEntity.ok(createdComment);
    }

    @GetMapping
    public ResponseEntity<Page<CommentDTO>> getComments(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<CommentDTO> comments = articleService.getCommentsByArticle(articleId, page, size);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long articleId,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String token) {

        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        articleService.deleteComment(articleId, commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
