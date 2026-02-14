package com.pd.rankingserver.service;

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
    // 유니티와 똑같이 맞춰야 하는 비밀키
    private static final String SECRET_KEY = "UnityMergeGameSecretKey2026!";

    public RankingService(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    @Transactional
    public Score recordScore(String name, int score, String clientHash) {
        // 1. 보안 검증
        String serverHash = generateHash(name, score);
        if (!serverHash.equals(clientHash)){
            throw new IllegalArgumentException("보안 경고: 데이터 조작됨.");
        }

        // 2. 기존 유저 조회
        Optional<Score> existingScore = scoreRepository.findByName(name);

        if (existingScore.isPresent()){
            // 이미 등록된 유저가 있는 경우
            Score userScore = existingScore.get();

            if (score > userScore.getScore()){
                // 신기록인 경우에만 점수 갱신
                userScore.setScore(score);
                System.out.println("📈 [Update] 최고 기록 갱신: " + name + " (" + score + "점)");
            } else {
                // 기존 점수가 더 높거나 같으면 무시
                System.out.println("🛡️ [Skip] 기존 기록이 더 높음: " + name);
            }

            return userScore;

        } else {
            // 신규 유저인 경우
            Score newScore = new Score(name, score);
            System.out.println("🆕 [Insert] 신규 유저 등록: " + name);
            return scoreRepository.save(newScore);
        }
    }

    public List<Score> getTop10() {
        return scoreRepository.findAllByOrderByScoreDesc();
    }

    private String generateHash(String name, int score) {
        String input = name + score + SECRET_KEY;
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