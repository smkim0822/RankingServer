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

```text
Unity 클라이언트
  -> POST /api/rankings
  -> 요청 DTO 매핑 및 @Valid 검증
  -> SHA-256 해시 검증 (위변조 차단)
  -> UUID 기반 기존 데이터 조회
  -> 신규 INSERT 또는 최고 점수만 UPDATE
  -> MySQL Score 테이블 적재
  -> score 내림차순 Top 50 조회
  -> RankingResponseDto로 공개 필드만 반환
```

### 1. 데이터 수집 및 보안 검증 (Ingestion & Validation)

클라이언트는 `uuid`, `name`, `score`, `hash`를 JSON으로 전송합니다.
해시는 다음 문자열을 UTF-8로 인코딩한 뒤 SHA-256으로 계산하며, 검증 실패 요청은 저장하지 않습니다.

```text
{name}:{score}:{uuid}:{RANKING_SECRET}
```

### 2. 비즈니스 로직 (Upsert)

검증을 통과한 요청은 UUID로 기존 사용자를 찾고, 다음 규칙으로 분기하여 데이터를 처리합니다.

- 신규 UUID: 새 레코드 INSERT
- 기존 UUID: 닉네임은 최신 값으로 갱신
- 기존 점수보다 높은 점수: 점수 UPDATE
- 기존 점수보다 낮거나 같은 점수: 기존 최고 점수 유지

### 3. 데이터 모델링 (MySQL Schema)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, Auto Increment | 레코드 고유 식별자 |
| `uuid` | VARCHAR(36) | Unique, Not Null | 클라이언트 디바이스 고유 ID |
| `name` | VARCHAR(50) | Not Null | 사용자 닉네임 |
| `score` | INT | Not Null, **Index** | 랭킹 점수 (B-Tree 인덱스 적용) |

---

## API 명세서

Base path: `/api`

| Method | Path | 설명 | HTTP Status |
|---|---|---|---|
| GET | `/health` | 클라이언트 호환용 `ok` 응답 | `200 OK` |
| GET | `/health/detailed` | 애플리케이션·DB 상태 및 버전 상세 조회 | `200 OK` |
| POST | `/rankings` | 점수 등록 또는 최고 점수 갱신 | `200 OK`, `400 Bad Request` |
| GET | `/rankings` | 점수 내림차순 Top 50 | `200 OK` |
| GET | `/rankings/{uuid}` | 닉네임·점수·개인 순위 | `200 OK`, `404 Not Found` |
| PATCH | `/rankings/{uuid}/name` | 닉네임 변경 | `200 OK`, `400 Bad Request`, `404 Not Found` |

---

## 주요 개선 및 트러블슈팅 (Troubleshooting)

- **DB 스키마와 Entity 제약조건 동기화:** DB 컬럼 제약조건과 JPA Entity 설정 간 불일치로 인한 데이터 오염 가능성을 방지하기 위해 `Score` 엔티티 필드에 길이 제약 및 `@Column(nullable = false)`을 명시하여 스키마 정합성을 강화했습니다.
- **API 입력 검증 및 예외 응답 표준화:** PATCH 닉네임 변경 API에 `@Valid`, `@NotBlank`, `@Size(max = 50)`을 선언해 무효 요청을 사전 차단하고, 존재하지 않는 UUID 조회 시 `404 Not Found` 상태 코드를 반환하도록 표준화했습니다. (`curl` 클라이언트 테스트 완료)
- **DTO 패키지 구조 리팩토링:** `controller` 패키지에 포함되어 있던 DTO 클래스들을 `com.pd.rankingserver.dto` 전용 패키지로 통합 이동하여 Layered Architecture 계층 응집도를 높였습니다.
- **조회 성능 최적화 (B-Tree 인덱스 도입):** 단순 `ORDER BY score DESC` 정렬 시 데이터 누적으로 인한 Full Table Scan 발생을 방지하고자 `score` 컬럼에 B-Tree 인덱스 (`idx_score_score`)를 적용해 Top 50 조회 속도를 최적화했습니다.

---

## 향후 개선 과제

- **Redis Sorted Set 기반 CQRS 검토:** 동시 접속 및 순위 계산 요청 증가 시 DB 정렬 연산 부담을 줄이기 위해 읽기 모델로 Redis Sorted Set(ZSET)을 도입하는 아키텍처 검토.
- **SQL 윈도우 함수 기반 순위 계산:** 동점자 처리 및 효율적인 개인 순위 조회를 위해 MySQL 8.0 `RANK() OVER` 윈도우 함수 쿼리 적용 및 `EXPLAIN` 실행 계획 분석.

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
