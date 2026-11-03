# Tap Blast - Ranking Server

모바일 하이퍼캐주얼 게임 **Tap Blast: Drop & Merge**의 글로벌 랭킹을
제공하는 Spring Boot REST API 서버입니다. Unity 클라이언트가 전송한 UUID,
닉네임, 점수를 검증하고 MySQL에 사용자별 최고 점수로 저장한 뒤 Top 50과
개인 순위를 제공합니다.

## 기술 스택

| 구분 | 내용 |
|---|---|
| Framework | Spring Boot 4.0.2 |
| Language | Java 17 |
| Database | MySQL, Spring Data JPA |
| Deployment | OCI Ubuntu, systemd |
| API | Spring Web REST API |

## 데이터 흐름

```text
Unity 클라이언트
  -> POST /api/rankings
  -> 요청 DTO 매핑
  -> SHA-256 해시 검증
  -> UUID 기반 기존 데이터 조회
  -> 신규 INSERT 또는 최고 점수만 UPDATE
  -> MySQL Score 테이블
  -> score 내림차순 Top 50 조회
  -> RankingResponseDto로 공개 필드만 반환
```

### 1. 데이터 수집

클라이언트는 `uuid`, `name`, `score`, `hash`를 JSON으로 전송합니다.
해시는 다음 문자열을 UTF-8로 인코딩한 뒤 SHA-256으로 계산합니다.

```text
{name}:{score}:{uuid}:{RANKING_SECRET}
```

### 2. 검증 및 Upsert

서버가 동일한 규칙으로 해시를 계산해 요청값과 비교합니다. 검증에 실패한
요청은 저장하지 않습니다. 검증을 통과한 요청은 UUID로 기존 사용자를 찾고,
다음 규칙으로 저장합니다.

- 신규 UUID: 새 레코드 INSERT
- 기존 UUID: 닉네임은 최신 값으로 갱신
- 기존 점수보다 높은 점수: 점수 UPDATE
- 기존 점수보다 낮거나 같은 점수: 기존 최고 점수 유지

### 3. DB 저장

`Score` 엔티티는 `id`, `uuid`, `name`, `score` 컬럼을 사용합니다.
`uuid`에는 Unique 및 Not Null 제약을 적용해 사용자 중복을 방지하고,
`score`에는 Top-N 정렬과 점수 비교 조회를 위한 B-Tree 인덱스를 선언했습니다.

### 4. Top 50 집계 및 응답

Spring Data JPA의
`findTop50ByOrderByScoreDesc()`가 DB에서 점수 내림차순 정렬과 상위 50건
제한을 수행합니다. API는 JPA 엔티티를 직접 직렬화하지 않고
`RankingResponseDto(name, score)`로 변환해 내부 `id`와 `uuid`를 외부에
노출하지 않습니다.

## 이번 리팩토링에서 개선한 점

- **Entity 노출 차단:** `GET /api/rankings`가 `Score` 엔티티 대신
  `RankingResponseDto`를 반환하도록 변경했습니다. 이를 통해 DB 식별자와
  내부 UUID가 API 응답 계약에 섞이지 않습니다.
- **조회 성능 기반 마련:** `Score.score`에 `idx_score_score` B-Tree
  인덱스를 추가해 Top 50 정렬 및 `score > ?` 조건 조회를 고려했습니다.
- **안전한 로깅:** 점수 해시 생성 과정에서 Secret이 포함된 원본 문자열을
  표준 출력으로 남기던 코드를 제거했습니다. 해시 검증 실패만 SLF4J
  로그로 기록하며 Secret은 로그에 포함하지 않습니다.

## API

Base path: `/api`

| Method | Path | 설명 |
|---|---|---|
| GET | `/health` | 클라이언트 호환용 `ok` 응답 |
| GET | `/health/detailed` | 애플리케이션·DB 상태와 버전 |
| POST | `/rankings` | 점수 등록 또는 최고 점수 갱신 |
| GET | `/rankings` | 점수 내림차순 Top 50 |
| GET | `/rankings/{uuid}` | 닉네임·점수·개인 순위 |
| PATCH | `/rankings/{uuid}/name` | 닉네임 변경 |

### Top 50 응답 예시

```json
[
  {
    "name": "PlayerName",
    "score": 12345
  }
]
```

## 데이터 엔지니어링 관점의 향후 개선 과제

### 데이터 규모 증가 시 Redis Sorted Set 검토

현재는 MySQL의 `ORDER BY`와 `COUNT`로 랭킹을 계산합니다. 사용자 수가 수만
명 이상으로 증가하거나 개인 순위 조회 요청이 많아지면 매 요청마다 DB에서
정렬·COUNT를 수행하는 비용이 커질 수 있습니다. 이 경우 Redis Sorted Set을
랭킹 읽기 모델로 도입하고, MySQL을 영속 원장으로 유지하는 CQRS형 구조를
검토할 수 있습니다.

### SQL 윈도우 함수 기반 순위 계산

개인 순위 조회가 증가하거나 한 번에 순위가 필요한 사용자가 많아지면,
단순 `COUNT(score > ?)` 대신 MySQL 8의 `RANK() OVER` 또는 `DENSE_RANK() OVER`
윈도우 함수를 사용하는 조회로 전환할 계획입니다. 동점자 처리 정책을
명확히 정하고, `EXPLAIN`으로 인덱스 사용 여부와 실행 시간을 검증해야 합니다.

### 추가 개선 항목

- 점수 원천 이벤트와 현재 최고 점수 테이블을 분리해 이력·부정행위 분석 지원
- `ddl-auto: update` 대신 Flyway/Liquibase 기반 버전 관리형 스키마 도입
- Bean Validation으로 UUID, 닉네임, 음수 점수 등 입력값 검증
- 단순 SHA-256 조합 대신 HMAC, nonce, 만료 시간 등 재전송 방지 강화
- 대량 데이터 기준으로 `score` 인덱스와 복합 정렬 조건의 실행 계획 측정

## 로컬 실행

### 환경 변수

운영 비밀은 저장소에 커밋하지 않고 환경 변수로 주입합니다.

| 변수 | 설명 |
|---|---|
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `RANKING_SECRET` | 클라이언트와 공유하는 점수 서명 Secret |

Windows PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/ranking_game?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
$env:DB_USERNAME="ranking_app"
$env:DB_PASSWORD="your_password"
$env:RANKING_SECRET="your_secret"
.\gradlew.bat bootRun
```

빌드:

```powershell
.\gradlew.bat build
```

## 운영 배포

systemd unit과 환경 변수 템플릿은 [`deploy/`](./deploy/)에 있습니다.
상세 설치·검증 절차는 [`deploy/README.md`](./deploy/README.md)를 참고하세요.

운영 비밀 파일은 `/etc/ranking-server.env`에 두고 권한을 `600`으로
설정해야 합니다. 비밀번호와 Secret을 Git에 커밋하지 마세요.

## 관련 프로젝트

- 게임 클라이언트:
  [Tap-Blast-Drop-Merge](https://github.com/romantic02/Tap-Blast-Drop-Merge)
