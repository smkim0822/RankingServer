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

    // Top 50 랭킹 리스트 반환
    @GetMapping("/api/ranks")
    public List<Score> getTopRanks() {
        return rankingService.getTop50();
    }

    // 내 랭킹 정보 조회 API
    @GetMapping("/api/rankings/{uuid}")
    public ResponseEntity<?> getMyRank(@PathVariable String uuid) {
        MyRankResponseDto myRank = rankingService.getMyRank(uuid);

        // 서버에 데이터가 없으면 404 반환
        if (myRank == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(myRank);
    }
}