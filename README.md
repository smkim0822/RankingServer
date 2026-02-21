# 🏆 Project Drop - Ranking Server

![Project Status](https://img.shields.io/badge/Status-Developing-yellow?style=flat-square) 
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-green?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue?style=flat-square&logo=mysql)

## 📝 프로젝트 소개
모바일 하이퍼캐주얼 게임 **'Project Drop'**의 실시간 글로벌 랭킹을 처리하기 위한 Spring Boot 기반의 백엔드 API 서버입니다.
클라이언트로부터 유저의 점수 데이터를 받아 무결성을 검증하고 저장하며, 전체 Top 50 랭킹 및 특정 유저의 개인 등수를 계산하여 제공합니다.

## ✨ 핵심 기능 (Key Features)
* **REST API 기반 랭킹 서비스:** 클라이언트의 점수 등록(POST) 및 순위 조회(GET) 요청 처리.
* **데이터 무결성 및 보안 검증:** UUID 기반 유저 식별 및 Hash 암호화 검증을 통한 비정상 점수 등록(조작) 차단.
* **최고점 갱신 (Upsert) 로직:** 기존 유저의 기록 경신 시에만 점수를 업데이트하고, 닉네임은 항상 최신 상태로 동기화.
* **트래픽 및 성능 최적화:** * 전체 랭킹 조회 시 상위 50위 데이터만 반환.
  * 개인 등수 조회 시 전체 DB 정렬 대신 `countByScoreGreaterThan` 쿼리를 사용하여 서버 부하 최소화.

## 🛠️ 기술 스택 (Tech Stack)
* **Framework:** Spring Boot 4.0.2
* **Language:** Java 17
* **Database:** MySQL, Spring Data JPA

## 🌐 API 명세서 (API Endpoints)
| Method | URL | Description |
|---|---|---|
| `GET` | `/api/rankings` | 점수 내림차순 기준 Top 50 랭킹 리스트 조회 |
| `GET` | `/api/rankings/{uuid}` | 특정 UUID를 가진 유저의 닉네임, 점수 및 현재 전체 등수 조회 |
| `POST` | `/api/rankings` | 게임 종료 시 유저 닉네임, UUID, 점수, Hash 서명 데이터 등록 및 갱신 |
| `GET` | `/api/health` | 서버 상태 정상 확인 (Health Check) |

## 🔗 관련 프로젝트 (Related Project)
* [[게임 클라이언트 레포지토리 (Project-Drop)]](https://github.com/romantic02/Project-Drop)