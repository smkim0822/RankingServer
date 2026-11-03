package com.pd.rankingserver.controller;

import com.pd.rankingserver.domain.Score;
import com.pd.rankingserver.repository.ScoreRepository;
import com.pd.rankingserver.service.RankingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import com.pd.rankingserver.dto.NicknameUpdateDto;

@RestController
public class RankingController {

    private final RankingService rankingService;
    private final ScoreRepository scoreRepository;

    /** build.gradle의 version을 application.yml 등에서 주입받기 위한 키. 미주입 시 "unknown". */
    @Value("${spring.application.version:unknown}")
    private String appVersion;

    public RankingController(RankingService rankingService, ScoreRepository scoreRepository) {
        this.rankingService = rankingService;
        this.scoreRepository = scoreRepository;
    }

    @GetMapping("/api/health")
    public String healthCheck() {
        return "ok";
    }

    /**
     * 운영·모니터링용 상세 헬스체크.
     * 기존 /api/health 평문 "ok" 응답은 클라이언트/keep-alive 호환을 위해 그대로 두고,
     * DB 핑·버전 등을 포함한 JSON 응답은 이 엔드포인트에서만 제공한다.
     */
    @GetMapping("/api/health/detailed")
    public ResponseEntity<HealthResponseDto> healthDetailed() {
        String dbStatus;
        try {
            scoreRepository.count();
            dbStatus = "UP";
        } catch (Exception e) {
            dbStatus = "DOWN";
        }

        String overall = "UP".equals(dbStatus) ? "UP" : "DEGRADED";
        HealthResponseDto body = new HealthResponseDto(
                overall,
                dbStatus,
                Instant.now().toString(),
                appVersion
        );

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(body);
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
    public List<RankingResponseDto> getTopRanks() {
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