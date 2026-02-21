package com.pd.rankingserver.service;

import com.pd.rankingserver.controller.MyRankResponseDto;
import com.pd.rankingserver.controller.ScoreRequestDto;
import com.pd.rankingserver.domain.Score;
import com.pd.rankingserver.repository.ScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Service
public class RankingService {

    private final ScoreRepository scoreRepository;
    private static final String SECRET_KEY = "UnityMergeGameSecretKey2026!";

    public RankingService(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    @Transactional
    public Score recordScore(ScoreRequestDto dto) {
        String serverHash = generateHash(dto.getName(), dto.getScore(), dto.getUuid());
        if (!serverHash.equals(dto.getHash())){
            throw new IllegalArgumentException("보안 경고: 데이터 조작됨.");
        }

        Optional<Score> existingScore = scoreRepository.findByUuid(dto.getUuid());

        if (existingScore.isPresent()){
            Score userScore = existingScore.get();
            userScore.setName(dto.getName());

            if (dto.getScore() > userScore.getScore()){
                userScore.setScore(dto.getScore());
            }
            return userScore;

        } else {
            Score newScore = Score.builder()
                    .uuid(dto.getUuid())
                    .name(dto.getName())
                    .score(dto.getScore())
                    .build();
            return scoreRepository.save(newScore);
        }
    }

    // [수정] Top 50만 반환하도록 변경
    public List<Score> getTop50() {
        return scoreRepository.findTop50ByOrderByScoreDesc();
    }

    // [추가] UUID 기반 내 랭킹 조회 로직
    public MyRankResponseDto getMyRank(String uuid) {
        Optional<Score> scoreOpt = scoreRepository.findByUuid(uuid);

        // 기록이 아예 없는 유저일 경우 null 반환 (컨트롤러에서 404 처리)
        if (scoreOpt.isEmpty()) {
            return null;
        }

        Score score = scoreOpt.get();
        // 등수 = 내 점수보다 높은 사람 수 + 1
        int rank = scoreRepository.countByScoreGreaterThan(score.getScore()) + 1;

        return new MyRankResponseDto(rank, score.getName(), score.getScore());
    }

    private String generateHash(String name, int score, String uuid) {
        String input = name + score + uuid + SECRET_KEY;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시 생성 오류", e);
        }
    }
}