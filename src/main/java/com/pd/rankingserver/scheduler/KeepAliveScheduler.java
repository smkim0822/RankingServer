package com.pd.rankingserver.scheduler;

import com.pd.rankingserver.repository.ScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * OCI Always Free 회수 정책(7일 누적 idle) 대비 백업 keep-alive.
 *
 * <p>기본 트리거는 {@code Tap-Blast-Drop-Merge} 저장소의 GitHub Actions 워크플로
 * ({@code keep-oci-alive.yml})가 10분 간격으로 외부에서 /api/health를 ping하는 것이지만,
 * Actions가 멈출 가능성(예: 토큰 만료, 사용자 의도로 비활성화, 외부 장애)에 대비해
 * 서버 자체에서도 1일 1회 가벼운 자가 워크로드를 돌린다.
 *
 * <p>토글: {@code ranking.keepalive.enabled} (기본 true)
 * <p>주기:  {@code ranking.keepalive.cron} / {@code ranking.keepalive.zone}
 */
@Component
@ConditionalOnProperty(
        name = "ranking.keepalive.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KeepAliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveScheduler.class);

    private final ScoreRepository scoreRepository;

    public KeepAliveScheduler(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    /**
     * 기본값: 매일 KST 04:00 (게임 트래픽 최저 시간대).
     * cron/zone 모두 외부 설정으로 덮어쓸 수 있어, 임시 검증 시에는
     * {@code ranking.keepalive.cron=0/30 * * * * *} 로 30초마다 발화 가능.
     */
    @Scheduled(
            cron = "${ranking.keepalive.cron:0 0 4 * * *}",
            zone = "${ranking.keepalive.zone:Asia/Seoul}"
    )
    public void ping() {
        // 스케줄러는 절대 죽지 않도록 모든 예외를 잡아 로그만 남긴다.
        try {
            long count = scoreRepository.count();
            log.info("keep-alive ping: scoreCount={}", count);
        } catch (Exception e) {
            log.warn("keep-alive ping failed: {}", e.toString());
        }
    }
}
