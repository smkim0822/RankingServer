# Tap Blast - Ranking Server

모바일 하이퍼캐주얼 게임 **Tap Blast: Drop & Merge**의 글로벌 랭킹을 제공하는 Spring Boot REST API 서버입니다.  
Unity 클라이언트가 전송한 UUID, 닉네임, 점수를 검증하고 MySQL에 사용자별 최고 점수로 저장한 뒤 Top 50 및 개인 순위를 제공합니다.

---

## 기술 스택

| 구분 | 내용 |
|---|---|
| Framework | Spring Boot 4.0.2 |
| Language | Java 17 |
| Database | MySQL, Spring Data JPA |
| Deployment | OCI Ubuntu, systemd |
| API | Layered Architecture, Spring Web REST API |

---

## 데이터 흐름 (Data Flow)

### 1. 점수 등록 및 갱신 흐름 (POST /api/rankings)

```text
Unity 클라이언트
  -> POST /api/rankings
  -> ScoreRequestDto 매핑 및 @Valid 검증 (UUID 정규식, 닉네임, 음수 점수 방어)
  -> SHA-256 해시 검증 (위변조 차단)
  -> UUID 기반 기존 데이터 조회
  -> 신규 INSERT 또는 최고 점수만 UPDATE
  -> MySQL Score 테이블 적재
  -> ScoreResponseDto(uuid, name, score)로 등록 결과 반환
```

### 2. Top 50 랭킹 조회 흐름 (GET /api/rankings)

```text
Unity 클라이언트
  -> GET /api/rankings
  -> score 내림차순 Top 50 정렬 조회
  -> List<RankingResponseDto>로 반환 (개별 요소는 name, score만 포함해 내부 ID/UUID 은닉)
```

---

### 보안 검증 및 Upsert 로직

- **데이터 수집 및 입력 검증:** 클라이언트는 `uuid`, `name`, `score`, `hash`를 JSON으로 전송합니다. `@Valid`를 통해 UUID 형식(`@Pattern`), 닉네임(`@NotBlank`, `@Size`), 점수(`@NotNull`, `@Min(0)`), 해시(`@NotBlank`)를 검증하며, 위변조 방지를 위해 사전 정의된 서명 규칙(`{name}:{score}:{uuid}:{RANKING_SECRET}`)으로 SHA-256 해시 비교를 수행합니다.
- **Upsert 처리 규칙:**
  - 신규 UUID: 새 레코드 `INSERT`
  - 기존 UUID: 닉네임 최신화 반영, 기존 최고 점수보다 높은 경우에만 `score` `UPDATE` (낮거나 같으면 유지)
---

### 데이터 모델링 (MySQL Schema)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, Auto Increment | 레코드 고유 식별자 |
| `uuid` | VARCHAR(36) | Unique, Not Null | 클라이언트 디바이스 고유 ID |
| `name` | VARCHAR(50) | Not Null | 사용자 닉네임 |
| `score` | INT | Not Null, **Index** | 랭킹 점수 (`idx_score_score` 인덱스 적용) |

---

## API 명세서

Base path: `/api`

| Method | Path | 설명 | HTTP Status | 요청 DTO | 응답 DTO / Body |
|---|---|---|---|---|---|
| GET | `/health` | 클라이언트 헬스체크 응답 | `200 OK` | 없음 | `String` (`"ok"`) |
| GET | `/health/detailed` | 서버 및 DB 상태 상세 조회 | `200 OK` | 없음 | `HealthResponseDto` |
| POST | `/rankings` | 점수 등록 또는 최고 점수 갱신 | `200 OK`, `400 Bad Request` | `ScoreRequestDto` | `ScoreResponseDto` |
| GET | `/rankings` | 점수 내림차순 Top 50 | `200 OK` | 없음 | `List<RankingResponseDto>` |
| GET | `/rankings/{uuid}` | 닉네임·점수·개인 순위 | `200 OK`, `404 Not Found` | PathVariable `uuid` | `MyRankResponseDto` |
| PATCH | `/rankings/{uuid}/name` | 닉네임 변경 | `200 OK`, `400 Bad Request`, `404 Not Found` | `NicknameUpdateDto` | Void (Empty Body) |

---

## 주요 개선 및 트러블슈팅 (Troubleshooting)

- **DB 스키마와 Entity 제약조건 동기화:** DB 컬럼 제약조건과 JPA Entity 설정 간 불일치로 인한 데이터 오염 가능성을 방지하기 위해 `Score` 엔티티 필드에 길이 제약 및 `@Column(nullable = false)`을 명시하여 스키마 정합성을 강화했습니다.
- **API 입력 검증 및 예외 응답 표준화:** Controller 계층에 `@Valid`를 배치하고 DTO별 입력 검증(`@NotBlank`, `@Size`, `@Pattern`, `@Min`)을 강화하여 유효하지 않은 요청을 사전 차단했습니다. 또한 존재하지 않는 UUID 조회 시 `404 Not Found` 상태 코드가 반환되도록 예외 처리를 표준화했습니다.
- **DTO 패키지 구조 리팩토링 및 DTO 분리:** `controller` 패키지에 혼재하던 DTO들을 `com.pd.rankingserver.dto` 전용 패키지로 통합 이동하고, Top 50 랭킹 조회 시 외부 노출이 불필요한 내부 `id` 및 `uuid` 필드를 은닉한 `RankingResponseDto`를 응답 계약으로 분리했습니다.
- **조회 성능 최적화 구조 마련 (B-Tree 인덱스 도입):** 단순 `ORDER BY score DESC` 정렬 시 데이터 누적으로 인한 성능 저하를 방지하고자 MySQL의 기본 B-Tree 인덱스로 활용되는 `score` 인덱스 (`idx_score_score`)를 선언하여 Top 50 정렬 및 범위 조회의 인덱스 활용 가능성을 확보했습니다.

---

## 향후 개선 과제

- **Redis Sorted Set 기반 CQRS 검토:** 동시 접속 및 순위 계산 요청 증가 시 DB 정렬 연산 부담을 줄이기 위해 읽기 모델로 Redis Sorted Set(ZSET)을 도입하는 아키텍처 검토.
- **SQL 윈도우 함수 기반 순위 계산 및 EXPLAIN 검증:** 동점자 처리 및 효율적인 개인 순위 조회를 위해 MySQL 8.0 `RANK() OVER` 윈도우 함수 쿼리 적용 및 `EXPLAIN` 실행 계획을 통한 실제 인덱스 타겟팅 성능 검증.

---

## 로컬 실행 및 배포 환경

### 환경 변수

운영 비밀은 저장소에 커밋하지 않고 환경 변수로 주입합니다.

| 변수 | 설명 |
|---|---|
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `RANKING_SECRET` | 클라이언트와 공유하는 점수 서명 Secret |

Windows PowerShell 기준 실행 예시:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/ranking_game?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
$env:DB_USERNAME="ranking_app"
$env:DB_PASSWORD="your_password"
$env:RANKING_SECRET="your_secret"
.\gradlew.bat bootRun
```

### 운영 배포
systemd unit과 환경 변수 템플릿은 [`deploy/`](./deploy/)에 있습니다. 상세 설치·검증 절차는 [`deploy/README.md`](./deploy/README.md)를 참고하세요.

---

## 관련 프로젝트

- 게임 클라이언트:
  [Tap-Blast-Drop-Merge](https://github.com/romantic02/Tap-Blast-Drop-Merge)
