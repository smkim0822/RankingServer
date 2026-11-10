package com.pd.rankingserver.service;

import com.pd.rankingserver.dto.MyRankResponseDto;
import com.pd.rankingserver.dto.RankingResponseDto;
import com.pd.rankingserver.dto.ScoreRequestDto;
import com.pd.rankingserver.dto.ScoreResponseDto;
import com.pd.rankingserver.domain.Score;
import com.pd.rankingserver.repository.ScoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingService.class);

    private final ScoreRepository scoreRepository;

    @Value("${ranking.secret}")
    private String secretKey;

    public RankingService(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    @Transactional
    public ScoreResponseDto recordScore(ScoreRequestDto dto) {
        String serverHash = generateHash(dto.getName(), dto.getScore(), dto.getUuid());
        if (!serverHash.equalsIgnoreCase(dto.getHash())){
            log.warn("Score hash verification failed for uuid={}", dto.getUuid());
            throw new IllegalArgumentException("보안 경고: 데이터 조작됨.");
        }

        Optional<Score> existingScore = scoreRepository.findByUuid(dto.getUuid());

        if (existingScore.isPresent()){
            Score userScore = existingScore.get();
            userScore.setName(dto.getName());

            if (dto.getScore() > userScore.getScore()){
                userScore.setScore(dto.getScore());
            }
            return toResponseDto(userScore);

        } else {
            Score newScore = Score.builder()
                    .uuid(dto.getUuid())
                    .name(dto.getName())
                    .score(dto.getScore())
                    .build();
            return toResponseDto(scoreRepository.save(newScore));
        }
    }

    private ScoreResponseDto toResponseDto(Score score) {
        return new ScoreResponseDto(score.getUuid(), score.getName(), score.getScore());
    }

    // [수정] Top 50만 반환하도록 변경
    public List<RankingResponseDto> getTop50() {
        return scoreRepository.findTop50ByOrderByScoreDesc().stream()
                .map(score -> new RankingResponseDto(score.getName(), score.getScore()))
                .toList();
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
        String input = name + ":" + score + ":" + uuid + ":" + secretKey;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
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

    @Transactional
    public void updateNickname(String uuid, String newName){
        Score score = scoreRepository.findByUuid(uuid)
                .orElseThrow(() -> new NoSuchElementException("랭킹 정보를 찾을 수 없습니다: " + uuid));
        score.updateName(newName);
    }
}