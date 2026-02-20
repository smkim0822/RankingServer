package com.pd.rankingserver.service;

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
        // 1. 보안 검증 (name + score + uuid + SECRET_KEY 순서)
        String serverHash = generateHash(dto.getName(), dto.getScore(), dto.getUuid());
        if (!serverHash.equals(dto.getHash())){
            throw new IllegalArgumentException("보안 경고: 데이터 조작됨.");
        }

        // 2. UUID 기반 기존 유저 조회 (Upsert)
        Optional<Score> existingScore = scoreRepository.findByUuid(dto.getUuid());

        if (existingScore.isPresent()){
            Score userScore = existingScore.get();
            // 닉네임은 변경되었을 수 있으므로 항상 최신화
            userScore.setName(dto.getName());

            if (dto.getScore() > userScore.getScore()){
                userScore.setScore(dto.getScore());
                System.out.println("📈 [Update] 최고 기록 갱신: " + dto.getName() + " (" + dto.getScore() + "점)");
            }
            return userScore;

        } else {
            // 신규 유저 등록
            Score newScore = Score.builder()
                    .uuid(dto.getUuid())
                    .name(dto.getName())
                    .score(dto.getScore())
                    .build();
            System.out.println("🆕 [Insert] 신규 유저 등록: " + dto.getName());
            return scoreRepository.save(newScore);
        }
    }

    public List<Score> getTop10() {
        return scoreRepository.findAllByOrderByScoreDesc();
    }

    private String generateHash(String name, int score, String uuid) {
        // 유니티 RankingManager의 GenerateHash와 동일한 순서
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