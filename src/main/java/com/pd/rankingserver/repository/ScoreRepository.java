package com.pd.rankingserver.repository;

import com.pd.rankingserver.domain.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    // 1. 전체 랭킹 조회 (Top 50명 제한)
    List<Score> findTop50ByOrderByScoreDesc();

    // 2. uuid로 기존 데이터 찾기
    Optional<Score> findByUuid(String uuid);

    // 3. 내 점수보다 높은 사람의 수를 카운트 (내 등수 계산용)
    int countByScoreGreaterThan(int score);
}