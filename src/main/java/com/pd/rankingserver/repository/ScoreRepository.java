package com.pd.rankingserver.repository;

import com.pd.rankingserver.domain.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    // 점수 높은 순으로 상위 10개 가져오기
    List<Score> findTop10ByOrderByScoreDesc();
}