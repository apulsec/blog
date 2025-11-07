package com.example.blog.article.controller;

import com.example.blog.article.dto.ArticleMetricsDTO;
import com.example.blog.article.dto.HotArticleDTO;
import com.example.blog.article.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lightweight read-only endpoint exposing analytics snapshots produced by the Spark job.
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleMetricsController {

    private final ArticleService articleService;

    public ArticleMetricsController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<ArticleMetricsDTO> getArticleMetrics(@PathVariable("id") Long articleId) {
        ArticleMetricsDTO metrics = articleService.getLatestArticleMetrics(articleId);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/hot")
    public ResponseEntity<List<HotArticleDTO>> getHotArticles(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "5") int limit) {
        List<HotArticleDTO> hotArticles = articleService.getHotArticles(days, limit);
        return ResponseEntity.ok(hotArticles);
    }
}
