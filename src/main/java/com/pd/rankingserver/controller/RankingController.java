package com.pd.rankingserver.controller;

import com.pd.rankingserver.domain.Score;
import com.pd.rankingserver.service.RankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/api/health")
    public String healthCheck() {
        return "ok";
    }

    @PostMapping("/api/scores")
    public ResponseEntity<?> saveScore(@RequestBody ScoreRequestDto request) {
        try {
            Score savedScore = rankingService.recordScore(request);
            return ResponseEntity.ok(savedScore);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/ranks")
    public List<Score> getTop10Ranks() {
        return rankingService.getTop10();
    }
}