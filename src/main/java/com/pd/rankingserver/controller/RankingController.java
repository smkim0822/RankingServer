package com.pd.rankingserver.controller;

import com.pd.rankingserver.domain.Score;
import com.pd.rankingserver.service.RankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.pd.rankingserver.dto.NicknameUpdateDto;

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

    // 점수 등록
    @PostMapping("/api/rankings")
    public ResponseEntity<?> saveScore(@RequestBody ScoreRequestDto request) {
        try {
            Score savedScore = rankingService.recordScore(request);
            return ResponseEntity.ok(savedScore);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Top 50 조회
    @GetMapping("/api/rankings")
    public List<Score> getTopRanks() {
        return rankingService.getTop50();
    }

    // 내 랭킹 정보 조회 API
    @GetMapping("/api/rankings/{uuid}")
    public ResponseEntity<?> getMyRank(@PathVariable String uuid) {
        MyRankResponseDto myRank = rankingService.getMyRank(uuid);

        if (myRank == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(myRank);
    }

    // 이름 변경
    @PatchMapping("/api/rankings/{uuid}/name")
    public ResponseEntity<Void> updateNickname(
            @PathVariable String uuid,
            @RequestBody NicknameUpdateDto requestDto) {
        rankingService.updateNickname(uuid, requestDto.getName());

        return ResponseEntity.ok().build();
    }
}