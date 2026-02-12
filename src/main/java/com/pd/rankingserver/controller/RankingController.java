package com.pd.rankingserver.controller;

import com.pd.rankingserver.domain.Score;
import com.pd.rankingserver.service.RankingService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    // 데이터를 받을 껍데기 클래스 (DTO)
    @Getter @Setter
    static class ScoreRequest {
        private String name;
        private int score;
        private String hash;
    }

    @PostMapping("/api/scores")
    public ResponseEntity<?> saveScore(@RequestBody ScoreRequest request) {
        try {
            Score savedScore = rankingService.recordScore(request.getName(), request.getScore(), request.getHash());
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