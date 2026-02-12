package com.pd.rankingserver.service;

import com.pd.rankingserver.domain.Score;
import com.pd.rankingserver.repository.ScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

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
        // 서버에서 직접 해시를 만들어보고, 클라이언트 값과 비교
        String serverHash = generateHash(name, score);
        if (!serverHash.equals(clientHash)) {
            throw new IllegalArgumentException("보안 경고: 데이터가 조작되었습니다.");
        }
        return scoreRepository.save(new Score(name, score));
    }

    public List<Score> getTop10() {
        return scoreRepository.findTop10ByOrderByScoreDesc();
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