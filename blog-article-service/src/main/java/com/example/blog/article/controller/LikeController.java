package com.example.blog.article.controller;

import com.example.blog.article.service.ArticleService;
import com.example.blog.article.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/articles/{articleId}/likes")
public class LikeController {

    private final ArticleService articleService;
    private final JwtUtil jwtUtil;

    public LikeController(ArticleService articleService, JwtUtil jwtUtil) {
        this.articleService = articleService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<Void> likeArticle(
            @PathVariable Long articleId,
            @RequestHeader("Authorization") String token) {
        
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        articleService.likeArticle(articleId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unlikeArticle(
            @PathVariable Long articleId,
            @RequestHeader("Authorization") String token) {
        
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        articleService.unlikeArticle(articleId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getLikeStatus(
            @PathVariable Long articleId,
            @RequestHeader("Authorization") String token) {
        
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        boolean liked = articleService.hasUserLikedArticle(articleId, userId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }
}
