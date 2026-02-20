package com.pd.rankingserver.repository;

import com.pd.rankingserver.domain.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findAllByOrderByScoreDesc();
    Optional<Score> findByUuid(String uuid);
}