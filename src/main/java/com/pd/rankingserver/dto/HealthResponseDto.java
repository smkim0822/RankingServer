package com.pd.rankingserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * /api/health/detailed 응답 DTO.
 * 기존 /api/health 평문 "ok" 엔드포인트와 별도로, 운영·모니터링 도구가
 * 서버/DB/버전 상태를 한눈에 파악할 수 있게 한다.
 */
@Getter
@AllArgsConstructor
public class HealthResponseDto {

    /** 전체 서비스 상태. "UP" (모두 정상) 또는 "DEGRADED" (앱은 떠 있으나 의존성 일부 실패). */
    private final String status;

    /** DB 연결 상태. "UP" 또는 "DOWN". */
    private final String db;

    /** 응답 생성 시각(ISO-8601, UTC). */
    private final String checkedAt;

    /** 빌드 버전(`spring.application.version`, 없으면 "unknown"). */
    private final String version;
}
