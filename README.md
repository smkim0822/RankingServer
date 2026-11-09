# Tap Blast - Ranking Server

모바일 하이퍼캐주얼 게임 **Tap Blast: Drop & Merge**의 글로벌 랭킹을 제공하는 Spring Boot REST API 서버입니다. 
Unity 클라이언트가 전송한 UUID, 닉네임, 점수를 검증하고 MySQL에 사용자별 최고 점수로 저장한 뒤 Top 50과 개인 순위를 제공합니다.

## 기술 스택

| 구분 | 내용 |
|---|---|
| Framework | Spring Boot 4.0.2 |
| Language | Java 17 |
| Database | MySQL, Spring Data JPA |
| Deployment | OCI Ubuntu, systemd |
| API | Spring Web REST API |

## 데이터 흐름 (Data Flow)

```text
Unity 클라이언트
  -> POST /api/rankings
  -> 요청 DTO 매핑
  -> SHA-256 해시 검증
  -> UUID 기반 기존 데이터 조회
  -> 신규 INSERT 또는 최고 점수만 UPDATE
  -> MySQL Score 테이블 적재
  -> score 내림차순 Top 50 조회
  -> RankingResponseDto로 공개 필드만 반환
```

### 1. 데이터 수집 (Ingestion)

클라이언트는 `uuid`, `name`, `score`, `hash`를 JSON으로 전송합니다.
해시는 다음 문자열을 UTF-8로 인코딩한 뒤 SHA-256으로 계산합니다.

```text
{name}:{score}:{uuid}:{RANKING_SECRET}
```

### 2. 검증 및 Upsert

서버가 동일한 규칙으로 해시를 계산해 요청값과 비교합니다. 검증에 실패한 요청은 저장하지 않습니다. 
검증을 통과한 요청은 UUID로 기존 사용자를 찾고, 다음 규칙으로 분기하여 데이터를 처리합니다.

- 신규 UUID: 새 레코드 INSERT
- 기존 UUID: 닉네임은 최신 값으로 갱신
- 기존 점수보다 높은 점수: 점수 UPDATE
- 기존 점수보다 낮거나 같은 점수: 기존 최고 점수 유지

### 3. 데이터 모델링 (MySQL Schema)

`Score` 테이블은 사용자별 최고 점수를 관리하며, 빠르고 정확한 랭킹 조회를 위해 다음과 같이 설계했습니다.

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, Auto Increment | 레코드 고유 식별자 |
| `uuid` | VARCHAR(36) | Unique, Not Null | 클라이언트 디바이스 고유 ID |
| `name` | VARCHAR(50) | Not Null | 사용자 닉네임 (최신화 반영) |
| `score` | INT | Not Null, **Index** | 랭킹 점수 (B-Tree 인덱스 적용) |

### 4. Top 50 집계 및 응답

Spring Data JPA의 `findTop50ByOrderByScoreDesc()`를 통해 DB에서 점수 내림차순 정렬과 상위 50건 제한 조회를 수행합니다. 
API는 JPA 엔티티를 직접 직렬화하지 않고 `RankingResponseDto(name, score)`로 변환해 내부 `id`와 `uuid`를 외부에 노출하지 않습니다.

---

## 주요 문제 해결 및 데이터 아키텍처 고민

- **조회 성능 최적화 (B-Tree 인덱스 도입):** 단순 `ORDER BY score DESC`를 사용할 경우 데이터가 누적됨에 따라 풀 테이블 스캔(Full Table Scan)이 발생하여 성능이 크게 저하될 것을 우려했습니다. 이를 해결하기 위해 `score` 컬럼에 B-Tree 인덱스(`idx_score_score`)를 선언하여 Top 50 정렬 및 `score > ?` 조건 조회의 속도를 최적화했습니다.
- **Entity 노출 차단으로 안전한 데이터 응답 설계:** `GET /api/rankings` 호출 시 DB의 `Score` 엔티티를 직접 반환하지 않도록 변경했습니다. DTO를 분리하여 응답함으로써 DB 내부 식별자(`id`)와 클라이언트 고유 식별자(`uuid`)가 API 응답 계약에 섞이는 보안 리스크를 차단했습니다.
- **보안을 고려한 안전한 로깅:** 점수 해시 생성 과정에서 Secret이 포함된 원본 문자열을 표준 출력으로 남기던 코드를 제거했습니다. 해시 검증 실패만 SLF4J 로그로 기록하며, 민감한 Secret 정보는 로그 파이프라인에 포함되지 않도록 개선했습니다.

---

## API 명세서

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

---

## 데이터 엔지니어링 관점의 향후 개선 과제

### 1. 데이터 규모 증가 시 Redis Sorted Set 검토
현재는 MySQL의 `ORDER BY`와 `COUNT`로 랭킹을 계산합니다. 사용자 수가 수만 명 이상으로 증가하거나 개인 순위 조회 요청이 많아지면 매 요청마다 DB에서 정렬·COUNT를 수행하는 비용이 커질 수 있습니다. 향후 Redis Sorted Set을 랭킹 읽기 모델로 도입하고, MySQL을 영속 원장으로 유지하는 CQRS(명령과 조회의 분리)형 구조를 검토할 예정입니다.

### 2. SQL 윈도우 함수 기반 순위 계산
개인 순위 조회가 증가하거나 한 번에 순위가 필요한 사용자가 많아지면, 단순 `COUNT(score > ?)` 대신 MySQL 8.0 이상에서 지원하는 `RANK() OVER` 또는 `DENSE_RANK() OVER` 윈도우 함수를 사용하는 조회로 전환할 계획입니다. 동점자 처리 정책을 명확히 정하고, `EXPLAIN`으로 인덱스 사용 여부와 실행 시간을 검증할 것입니다.

### 3. 추가 개선 항목
- 점수 원천 이벤트와 현재 최고 점수 테이블을 분리해 유저 이력 및 부정행위 분석 지원
- `ddl-auto: update` 대신 Flyway/Liquibase 기반 버전 관리형 데이터베이스 스키마 도입
- 단순 SHA-256 조합 대신 HMAC, nonce, 만료 시간 등 재전송 방지 강화

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
systemd unit과 환경 변수 템플릿은 [`deploy/`](./deploy/)에 있습니다. 상세 설치·검증 절차는 [`deploy/README.md`](./deploy/README.md)를 참고하세요. (운영 비밀 파일은 `/etc/ranking-server.env`에 두고 권한을 `600`으로 설정해야 합니다.)

---

## 관련 프로젝트

- 게임 클라이언트:
  [Tap-Blast-Drop-Merge](https://github.com/romantic02/Tap-Blast-Drop-Merge)
