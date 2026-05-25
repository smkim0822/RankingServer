# Tap Blast — Ranking Server

![Status](https://img.shields.io/badge/Status-Production-brightgreen?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-green?style=flat-square&logo=springboot)
![Java](https://img.shields.io/badge/Java-17-blue?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue?style=flat-square&logo=mysql)
![OCI](https://img.shields.io/badge/Infrastructure-Oracle_Cloud-F80000?style=flat-square&logo=oracle)

## 프로젝트 소개

모바일 하이퍼캐주얼 게임 **[Tap Blast: Drop & Merge](https://github.com/romantic02/Tap-Blast-Drop-Merge)** 의 글로벌 랭킹을 처리하는 Spring Boot REST API 서버입니다.

클라이언트(Unity)에서 UUID·닉네임·점수·해시 서명을 받아 무결성을 검증한 뒤 MySQL에 저장하고, Top 50 랭킹과 개인 순위를 제공합니다.

## 핵심 기능

- **REST API:** 점수 등록·전체 랭킹·개인 순위·닉네임 변경·헬스체크
- **무결성 검증:** `SHA-256(name:score:uuid:secret)` 해시 — 클라이언트 `RankingRequestSigner`와 동일 규칙
- **Upsert:** 동일 UUID는 **점수가 더 높을 때만** 갱신, 닉네임은 요청 시마다 최신값으로 반영
- **성능:** Top 50만 반환, 개인 순위는 `countByScoreGreaterThan` 기반 계산
- **운영:** systemd 서비스 + `/etc/ranking-server.env` 비밀 분리 ([`deploy/`](./deploy/README.md))
- **OCI idle 대비:** GitHub Actions 외부 핑(클라이언트 레포) + 서버 내 `@Scheduled` DB ping 백업

## 기술 스택

| 구분 | 내용 |
|------|------|
| Framework | Spring Boot 4.0.2 |
| Language | Java 17 |
| DB | MySQL, Spring Data JPA (`ddl-auto: update`) |
| 배포 | OCI Always Free (Ubuntu), **systemd** `ranking-server` |
| JAR | `build/libs/RankingServer-0.0.1-SNAPSHOT.jar` |

## 배포·접속 (운영)

| 항목 | 값 |
|------|-----|
| **권장 Base URL** | `https://pd-ranking.duckdns.org/api` |
| **헬스체크 (클라이언트용)** | `GET /api/health` → 본문 `ok` (정확히 일치) |
| **상세 헬스체크 (운영·모니터링)** | `GET /api/health/detailed` → JSON (DB·버전) |
| **포트** | 호스트 `8080` (역방향 프록시로 HTTPS 종단) |

> 예전 문서의 `http://158.179.164.153:8080` 직접 접속은 **레거시**입니다. Unity `ServerConfig` 기본 폴백도 `https://pd-ranking.duckdns.org/api` 입니다.

### 검증 예시

```bash
curl -sS https://pd-ranking.duckdns.org/api/health
# ok

curl -sS https://pd-ranking.duckdns.org/api/health/detailed
```

서버 SSH 접속 후 systemd 상태·로그는 [`deploy/README.md`](./deploy/README.md) 참고.

## API 명세

Base path: `/api` (아래 URL은 `/api` 이후 경로)

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/health` | 서버 정상 여부. 응답 본문 **`ok`** (Unity `RankingManager.CheckServerStatus` 호환) |
| `GET` | `/health/detailed` | JSON: overall, db, timestamp, appVersion |
| `GET` | `/rankings` | 점수 내림차순 **Top 50** |
| `GET` | `/rankings/{uuid}` | 해당 UUID의 닉네임·점수·전체 등수. 없으면 **404** |
| `POST` | `/rankings` | 점수 등록/갱신. Body: `uuid`, `name`, `score`, `hash` |
| `PATCH` | `/rankings/{uuid}/name` | 닉네임만 변경. Body: `{ "name": "..." }` |

### POST `/api/rankings` Body 예시

```json
{
  "uuid": "device-uuid-string",
  "name": "PlayerName",
  "score": 12345,
  "hash": "hex-sha256..."
}
```

해시 입력 문자열: `{name}:{score}:{uuid}:{RANKING_SECRET}` (UTF-8) → SHA-256 hex 소문자.

### GET `/api/rankings/{uuid}` 응답 예시

```json
{
  "rank": 3,
  "name": "PlayerName",
  "score": 12345
}
```

## 로컬 실행

### 사전 요구

- JDK 17
- MySQL (로컬 DB `ranking_game` 등)

### 환경 변수

`application.yml`은 아래 값을 **환경 변수**로 받습니다 (커밋 금지).

| 변수 | 설명 |
|------|------|
| `DB_URL` | 예: `jdbc:mysql://localhost:3306/ranking_game?serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `DB_USERNAME` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `RANKING_SECRET` | 점수 해시용 시크릿 — **클라이언트 `ServerConfig.secretKey`와 동일**해야 함 |

Windows PowerShell 예:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/ranking_game?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
$env:DB_USERNAME="ranking_app"
$env:DB_PASSWORD="your_password"
$env:RANKING_SECRET="your_secret"
.\gradlew.bat bootRun
```

### 빌드

```bash
./gradlew build
# 산출물: build/libs/RankingServer-0.0.1-SNAPSHOT.jar
```

## 클라이언트 연동 (Unity)

저장소: [Tap-Blast-Drop-Merge](https://github.com/romantic02/Tap-Blast-Drop-Merge)

| 클라이언트 | 서버 |
|------------|------|
| `Resources/ServerConfig.asset` (`secretKey`, `oracleIp`, `GetBaseUrl()`) | `RANKING_SECRET`, HTTPS Base URL |
| `RankingManager` | 본 README API |
| `RankingRequestSigner` | `RankingService.generateHash` |
| `RankingPlayerIdentityStore` (UUID) | `uuid` 컬럼 |

`ServerConfig.asset`은 gitignore 대상입니다. 팀원은 `ServerConfig.example` 등 팀 내부 가이드로 동일 secret·URL을 맞춥니다.

## OCI Keep-Alive

- **외부 핑:** 게임 레포 `.github/workflows/keep-oci-alive.yml` (10분 간격, Variables `OCI_HEALTH_URL` 권장: `https://pd-ranking.duckdns.org/api/health`)
- **내부 백업:** `KeepAliveScheduler` — 기본 매일 KST 04:00 DB `count()` (`ranking.keepalive.*` 설정)
- 상세 운영 메모: 게임 레포 [`docs/OCI_KEEPALIVE.md`](https://github.com/romantic02/Tap-Blast-Drop-Merge/blob/main/docs/OCI_KEEPALIVE.md)

## 저장소 구조

```
src/main/java/com/pd/rankingserver/
  controller/   RankingController, DTO
  service/      RankingService (해시·Upsert·Top50·순위)
  repository/   ScoreRepository
  scheduler/    KeepAliveScheduler
deploy/         systemd unit, env 예시, 운영 가이드
```

## 관련 프로젝트

- **게임 클라이언트:** [romantic02/Tap-Blast-Drop-Merge](https://github.com/romantic02/Tap-Blast-Drop-Merge)

## 변경 이력 (요약)

- **systemd 운영:** `nohup` → `ranking-server.service`, 비밀은 `/etc/ranking-server.env`
- **헬스체크:** `/api/health/detailed` 추가 (기존 `/api/health` `ok` 유지)
- **닉네임:** `PATCH /api/rankings/{uuid}/name`
- **도메인:** `https://pd-ranking.duckdns.org` (HTTPS)
- **게임명:** Project Drop → **Tap Blast: Drop & Merge**
