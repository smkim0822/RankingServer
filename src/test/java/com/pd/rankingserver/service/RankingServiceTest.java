package com.pd.rankingserver.service;

import com.pd.rankingserver.controller.ScoreRequestDto;
import com.pd.rankingserver.domain.Score;
import com.pd.rankingserver.repository.ScoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // 테스트 끝나면 데이터 롤백 (DB 오염 방지)
class RankingServiceTest {

    @Autowired
    RankingService rankingService;

    @Autowired
    ScoreRepository scoreRepository;

    @Value("${ranking.secret}")
    private String SECRET_KEY;

    @Test
    @DisplayName("1. 신규 유저 등록 -> 2. 더 높은 점수 갱신 -> 3. 낮은 점수 무시 확인")
    void testUpsertLogic() {
        // Given
        String username = "TestPlayer_Upsert";
        String testUuid = "test-uuid-1234";

        // --- Step 1: 신규 유저 등록 (100점) ---
        int firstScore = 100;
        String hash1 = generateHash(username, firstScore);

        ScoreRequestDto req1 = new ScoreRequestDto();
        req1.setUuid(testUuid);
        req1.setName(username);
        req1.setScore(firstScore);
        req1.setHash(hash1);

        rankingService.recordScore(req1);

        // 검증: 100점으로 저장되었는가?
        Score savedScore1 = scoreRepository.findByUuid(testUuid).orElseThrow();
        assertThat(savedScore1.getScore()).isEqualTo(100);
        System.out.println("✅ Step 1 통과: 신규 유저 등록 완료 (100점)");

        // --- Step 2: 더 높은 점수로 갱신 시도 (200점) ---
        int highScore = 200;
        String hash2 = generateHash(username, highScore);

        ScoreRequestDto req2 = new ScoreRequestDto();
        req2.setUuid(testUuid);
        req2.setName(username);
        req2.setScore(highScore);
        req2.setHash(hash2);

        rankingService.recordScore(req2);

        // 검증: 200점으로 변경되었는가? (여기 findByName 남아있던 거 수정!)
        Score savedScore2 = scoreRepository.findByUuid(testUuid).orElseThrow();
        assertThat(savedScore2.getScore()).isEqualTo(200);
        System.out.println("✅ Step 2 통과: 최고 점수 갱신 완료 (200점)");

        // --- Step 3: 낮은 점수로 요청 시도 (50점) ---
        int lowScore = 50;
        String hash3 = generateHash(username, lowScore);

        ScoreRequestDto req3 = new ScoreRequestDto();
        req3.setUuid(testUuid);
        req3.setName(username);
        req3.setScore(lowScore);
        req3.setHash(hash3);

        rankingService.recordScore(req3);

        // 검증: 여전히 200점인가?
        Score savedScore3 = scoreRepository.findByUuid(testUuid).orElseThrow();
        assertThat(savedScore3.getScore()).isEqualTo(200);
        System.out.println("✅ Step 3 통과: 낮은 점수 무시 완료 (200점 유지)");
    }

    // 테스트용 해시 생성기
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
            throw new RuntimeException(e);
        }
    }
}