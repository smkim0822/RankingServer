package com.pd.rankingserver.repository;

import com.pd.rankingserver.domain.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    // 점수(score)를 기준으로 내림차순(Descending) 정렬해서 가져오기
    List<Score> findAllByOrderByScoreDesc();
}