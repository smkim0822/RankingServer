# 🏆 Project Drop - Ranking Server

![Project Status](https://img.shields.io/badge/Status-Live-brightgreen?style=flat-square) 
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-green?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue?style=flat-square&logo=mysql)
![Oracle Cloud](https://img.shields.io/badge/Oracle_Cloud-Infra-red?style=flat-square&logo=oracle)
![Nginx](https://img.shields.io/badge/Nginx-Reverse_Proxy-009639?style=flat-square&logo=nginx)

## 📝 프로젝트 소개
모바일 하이퍼캐주얼 게임 **Project Drop**의 실시간 랭킹을 처리하기 위한 Spring Boot 기반의 라이브 백엔드 API 서버입니다.
오라클 클라우드(Oracle Cloud) 인스턴스 위에서 Nginx 리버스 프록시와 연동되어 24시간 무중단으로 구동되며, 클라이언트의 점수 데이터를 안전하게 검증, 저장 및 실시간으로 제공합니다.

## ✨ 핵심 기능 및 아키텍처 (Key Features & Architecture)
* **Nginx 리버스 프록시 (Reverse Proxy) 기반 통신:** Nginx를 웹 서버로 앞단에 배치하여 외부 요청(80, 443)을 내부 WAS(Spring Boot, 8080)로 포워딩. 내부 포트를 은닉하고 비즈니스 로직과 웹 서빙 역할을 분리.
* **HTTPS(SSL/TLS) 강력한 보안:** 안드로이드 OS의 통신 규격을 충족하기 위해 Certbot(Let's Encrypt)으로 무료 SSL 인증서를 발급받아 암호화 통신(SSL Termination) 구현.
* **메모리 최적화 및 안정성 (Swap Memory):** 1GB RAM 환경의 프리 티어 한계를 극복하기 위해 리눅스에 2GB의 Swap 메모리를 할당하여 OOM(Out of Memory) 현상 원천 차단.
* **보안 및 무결성 검증:** * UUID 기반 유저 식별 및 Hash 암호화 검증을 통한 비정상 점수 등록(조작) 차단.
  * DB 자격 증명(비밀번호 등)을 환경변수로 주입하여 소스코드 탈취 대비 방어.
  * 오라클 VCN 및 iptables 방화벽을 제어하여 외부의 직접적인 DB 포트 접근 차단.
* **최고점 갱신 (Upsert) 로직:** 기존 유저의 기록 경신 시에만 점수를 업데이트하고, 닉네임은 항상 최신 상태로 동기화.

## 🛠️ 기술 스택 (Tech Stack)
* **Framework:** Spring Boot 4.0.2
* **Language:** Java 17
* **Database:** MySQL, Spring Data JPA
* **Infrastructure & DevOps:** Oracle Cloud (Oci), Nginx, Linux (Ubuntu), DuckDNS, Let's Encrypt (Certbot)

## 🌐 API 명세서 (API Endpoints)
> **Base URL:** `https://pd-ranking.duckdns.org`

| Method | URL | Description |
|---|---|---|
| `GET` | `/api/rankings` | 점수 내림차순 기준 Top 50 랭킹 리스트 조회 |
| `GET` | `/api/rankings/{uuid}` | 특정 UUID를 가진 유저의 닉네임, 점수 및 현재 전체 등수 조회 |
| `POST` | `/api/rankings` | 게임 종료 및 닉네임 변경 시 유저 닉네임, UUID, 점수, Hash 데이터 등록/갱신 |
| `GET` | `/api/health` | 서버 상태 정상 확인 (Health Check) |

## 🔗 관련 프로젝트 (Related Project)
* [[게임 클라이언트 레포지토리 (Project-Drop)]](https://github.com/romantic02/Project-Drop)

## 📅 배포 히스토리 (Deploy Log)
* **v0.8.0:** 오라클 클라우드 정식 배포, Nginx 리버스 프록시 적용, HTTPS(SSL) 암호화 통신 구축, Swap 메모리 최적화 적용
* **v0.7.0:** 로컬 환경 랭킹 API 개발 (UUID 검증, Top 50 쿼리 최적화)
