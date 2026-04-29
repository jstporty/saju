# 사주 백엔드 + 모노레포 설계 문서 (2026-04-29 갱신)

> 신규 합류자가 1시간 내에 컨텍스트를 잡을 수 있는 단일 진입 문서.
> 작성: 2026-04-28 → 공동 의사결정 라운드: 2026-04-29 → UX 라운드 + 카테고리/오늘 운세 + Next.js 프론트 도입: 2026-04-29 (3차)
> MVP 출시 목표: T+약 7주 (YC 배치, ADR-22)

---

## 제품 개요

- **제품**: MZ 페르소나용 재미·실사용성 있는 사주 PWA + 친구·가족 차트 + 공유 viral loop
- **범위**: 모노레포 = `backend/`(Spring Boot) + `frontend/`(Next.js 14) + `.github/workflows/`
- **MVP 7대 기능**: 카카오 로그인 / 출생정보 입력 / 사주 계산 + **5에너지·성격카드 6장** / **카테고리 운세 6종** / **오늘의 운세** / **공유 링크** / 이력
- **Post-MVP**: 풀 AI 해석(긴 텍스트) / PII 익명화 / PWA 푸시(재고)

---

## MVP 스코프 (ADR-22 lock)

### IN

- 카카오 OAuth 로그인 (spring-session-jdbc, ADR-15, cross-subdomain `Domain=.saju.app`)
- 출생정보 입력 (본인/타인 토글, 시간 모름, 자시 강제 선택, 양/음력 윤달, ADR-21)
- 사주 계산 (lunar-java + 한국식 보정, ADR-13) + **`ElementsScore`/`TenGodsCount`** (ADR-22)
- **카테고리 운세 6종** (총운/금전/연애/건강/직업·학업/가족·인간관계, ADR-26) — 사전 생성 캐시 룩업
- **오늘의 운세** (한 줄 + 행운 컬러/시간/주의, ADR-26) — 24h 캐시
- **공유 링크** (POST /shares, GET /shares/{token} 비로그인 공개)
- 사주 이력 목록·단건·삭제 (본인·친구·가족, ADR-21)
- **프론트엔드 Next.js 14 (App Router) + PWA** (ADR-25)

### OUT (Post-MVP)

- 풀 AI 해석 (긴 텍스트) → V3
- PII 익명화 → V4 (ADR-17)
- PWA 푸시 → V5+ (ADR-23 재고)
- 신살·대운, 친구 비교, 다국어, 결제

---

## 기술 스택 요약

### 백엔드

| 항목 | 값 |
|---|---|
| 런타임 | Java 21 (Temurin LTS) |
| 프레임워크 | Spring Boot 3.5.x (ADR-18) |
| ORM | Spring Data JPA + Querydsl 5.1.0:jakarta |
| DB | MySQL 8.x |
| 인증 | 카카오 OAuth 2.0 + spring-session-jdbc (ADR-15) |
| 사주 엔진 | lunar-java 1.7.x + 한국식 어댑터 + `ElementAnalyzer`·`TenGodsService`·`DailyStemBranchProvider` (ADR-13) |
| 마이그레이션 | Flyway 10.x (`ddl-auto: validate`) |
| 시크릿 | LocalStack(로컬) + AWS Secrets Manager(운영) (ADR-16) |
| 배포 | AWS ECS Fargate (Express Mode) |
| 캐시 | Caffeine L1 (`key_message`, `daily_fortune_template` 룩업) |

### 프론트엔드 (신규, ADR-25)

| 항목 | 값 |
|---|---|
| 프레임워크 | Next.js 14+ (App Router) + TypeScript |
| UI | Tailwind CSS + shadcn/ui (Radix) |
| 상태 (서버) | TanStack Query v5 |
| 상태 (클라이언트) | Zustand |
| 차트 | Recharts |
| OG 이미지 | `@vercel/og` (Edge Runtime) |
| PWA | `@serwist/next` (manifest + SW, push 미사용 ADR-23) |
| 배포 | Vercel (GitHub 통합 자동 배포) |
| 도메인 | `saju.app` |

전체 의존성 핀 → **[step2-tech-stack.md](./step2-tech-stack.md)** + **[step9-frontend.md](./step9-frontend.md)**

---

## 모노레포 구조

```
saju-backend/                       # 워크스페이스 루트 = git repo 루트
├── .git/
├── .gitignore                      # Java + Node 통합
├── README.md                       # 모노레포 안내
├── backend/                        # Spring Boot (Pre-flight에서 saju-backend/saju-backend → backend로 리네임)
│   ├── src/
│   ├── build.gradle
│   └── docs/design/                # 본 문서 위치
├── frontend/                       # Next.js (별도 plan에서 부트스트랩)
│   ├── app/
│   ├── components/
│   └── package.json
└── .github/workflows/
    ├── backend.yml                 # paths: ["backend/**"]
    └── frontend.yml                # Vercel 위임 시 lint/typecheck만
```

---

## 아키텍처 한 줄 요약

헥사고날(Ports & Adapters). 사주 엔진은 외부 라이브러리(lunar-java) 어댑터로 분리. 카테고리·오늘 운세는 사전 생성된 메시지 룩업으로 SLO 보호.

```
com.company.saju
 ├── auth/      카카오 OAuth + spring-session-jdbc
 ├── saju/      차트(=이력) + Fortune + Share + Engine 어댑터 + Seeder
 ├── ai/        LLM 시드 도구 전용 (사용자 요청 경로 미사용, ADR-19/24)
 ├── user/      Gender/CalendarType 등 enum 보존
 └── common/    ApiResponse, BaseEntity, IdGenerator, Exception
```

**제거 대상** (W1 Refactor): `engine/`, `notification/`, `hanja/`, `auth.infrastructure.jwt/`, `RefreshTokenEntity`, `history/`(saju로 흡수)

패키지 상세 → **[step7-architecture.md](./step7-architecture.md)**

---

## 단계별 설계 문서

| 단계 | 파일 | 핵심 내용 |
|---|---|---|
| 0 | (Pre-flight) | git init + `saju-backend/saju-backend/` → `backend/` 리네임 + .gitignore 통합 (완료) |
| 1 | [step1-engine-decision.md](./step1-engine-decision.md) | lunar-java 채택, `ElementAnalyzer`/`TenGodsService`/`DailyStemBranchProvider` 어댑터 내 재포함 |
| 2 | [step2-tech-stack.md](./step2-tech-stack.md) | SB 3.5.x 의존성 + **프론트엔드 스택 섹션 신규** |
| 3 | [step3-user-flow.md](./step3-user-flow.md) | **화면 단위 와이어 S1\~S8 + 에러 매트릭스 + 백엔드 시퀀스 부록** |
| 5 | [step5-api-design.md](./step5-api-design.md) | `/charts` 단일 자원 + `/shares` + `/charts/{id}/categories` + `/charts/{id}/today` |
| 6 | [step6-data-design.md](./step6-data-design.md) | `saju_share_link`/`key_message`/`daily_fortune_template` V2 ERD |
| 7 | [step7-architecture.md](./step7-architecture.md) | 헥사고날 패키지 + Fortune·Share 서비스 + Seeder 어드민 도구 |
| 8 | [step8-adr.md](./step8-adr.md) | ADR-01\~26 (12 Superseded by 21, 19/20 갱신) |
| 9 | [step9-frontend.md](./step9-frontend.md) | **Next.js 14 App Router 모노레포 설계 + GitHub Actions 분기** |
| 부록 | [migrations/](./migrations/README.md) | Flyway V1/V2 SQL — V2에 share_link/key_message/daily_fortune_template 포함 |

---

## 핵심 결정 요약 (ADR 26건)

### Locked (MVP 적용)

| ADR | 제목 | 결론 |
|---|---|---|
| ADR-02 | Virtual Threads | JPA 비적용, `vtIoExecutor`만 |
| ADR-03 | 배포 | ECS Fargate Express Mode |
| ADR-04 | 사용자 주체 | `oauth_user` 단일 테이블 |
| ADR-07 | RDBMS | MySQL 8.x |
| ADR-08 | 차트 테이블 | `saju_chart` 분리 |
| ADR-09 | DDL | Flyway + `validate` |
| ADR-13 | **사주 엔진** | **lunar-java + 한국식 어댑터** (Supersedes ADR-01) |
| ADR-14 | **Guest** | **차단** (Supersedes ADR-06) |
| ADR-15 | **세션** | **spring-session-jdbc** (Supersedes ADR-11) |
| ADR-16 | 시크릿 | LocalStack + AWS Secrets Manager |
| ADR-18 | 프레임워크 | Spring Boot 3.5.x 업그레이드 |
| ADR-19 | 모듈 정리 | `notification`/`hanja` 제거, **`ai/` 유지** (Seeder 활용) |
| ADR-21 | **차트 스코프** | **친구·가족 차트 허용** (Supersedes ADR-12) |
| ADR-22 | **MVP 스코프** | **7대 기능 + 약 7주** (Supersedes ADR-20) |
| ADR-23 | **PWA** | 단일 코드 + 푸시 미사용 |
| ADR-24 | **LLM precompute** | 50/300/1000 사전 생성 + DB 시드 |
| ADR-25 | **모노레포 + 프론트** | Next.js 14 App Router + Vercel |
| ADR-26 | **결과 페이지 표준** | 6 카테고리 + 오늘의 운세 |

### Superseded

| ADR | 무효 사유 |
|---|---|
| ADR-01 | → ADR-13 (lunar-java 채택) |
| ADR-06 | → ADR-14 (Guest 차단) |
| ADR-11 | → ADR-15 (session-jdbc) |
| ADR-12 | → ADR-21 (친구·가족 허용) |
| ADR-20 | → ADR-22 (스코프 7주 갱신) |

### Post-MVP

| ADR | 제목 |
|---|---|
| ADR-05 | 캐시 (MVP 일부 적용) |
| ADR-10 | AI Fallback (V3+에서 풀 AI 해석으로 부활) |
| ADR-17 | PII Anonymize (V4) |

---

## 데이터 모델 요약 (MVP)

| 테이블 | 역할 |
|---|---|
| `oauth_user` | 카카오 로그인 사용자 |
| `SPRING_SESSION` / `SPRING_SESSION_ATTRIBUTES` | spring-session-jdbc 세션 |
| `saju_chart` | 본인·친구·가족 사주 이력 (`subject_kind` SELF/OTHER) |
| `saju_share_link` | 공유 링크 토큰 |
| `key_message` | 키 메시지 + 카테고리 운세 6종 사전 생성 (50 + 300조합) |
| `daily_fortune_template` | 오늘의 운세 사전 생성 (~1000조합) |
| `solar_terms` | (보존) 절기 데이터 — lunar-java 검증용 |

**제거 (V2 마이그레이션)**: `refresh_token`, `notification`, `hanja`

ERD + DDL → **[step6-data-design.md](./step6-data-design.md)**

---

## API 엔드포인트 요약 (MVP)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/oauth2/authorization/kakao` | 익명 | 카카오 인가 |
| GET | `/login/oauth2/code/kakao` | 익명 | OAuth 콜백 (Spring Security) |
| GET | `/api/v1/auth/me` | 세션 | 내 정보 |
| POST | `/api/v1/auth/logout` | 세션 | 로그아웃 |
| POST | `/api/v1/charts` | 세션 | 사주 계산 + 저장 (subjectKind 포함) |
| GET | `/api/v1/charts` | 세션 | 본인 이력 목록 (커서) |
| GET | `/api/v1/charts/{id}` | 세션·소유자 | 차트 단건 (elementsScore + tenGodsCount + keyMessage) |
| DELETE | `/api/v1/charts/{id}` | 세션·소유자 | 소프트 삭제 + share 폐기 |
| GET | `/api/v1/charts/{id}/categories` | 세션·소유자 | 카테고리 운세 6종 |
| GET | `/api/v1/charts/{id}/today` | 세션·소유자 | 오늘의 운세 (24h 캐시) |
| POST | `/api/v1/shares` | 세션·소유자 | 공유 토큰 생성 |
| GET | `/api/v1/shares/{token}` | **익명** | 공유 페이지 (PII 마스킹) |

전체 스펙 → **[step5-api-design.md](./step5-api-design.md)**

---

## 약 7주 출시 일정 (T+0 = 2026-04-29)

| 주차 | 스프린트 | 산출물 |
|---|---|---|
| W1 | **Pre-work + Refactor** | 시크릿 회전, .env 정리, LocalStack Secrets Manager, SB 3.5.x 업그레이드, Querydsl 5.1.0, Flyway 도입(V1 baseline), `notification`/`hanja` 제거 |
| W2 | **Auth** | JWT 제거, spring-session-jdbc, `SecurityConfig` 재작성, V2 마이그레이션 |
| W2\~W3 | **Engine** | lunar-java 도입, `KoreanLmtAdjuster`/`HourPillarAdjuster`/`ElementAnalyzer`/`TenGodsService`/`DailyStemBranchProvider`, **골든 테스트셋 50건** |
| W3\~W4 | **Domain (Backend)** | `ChartController`/`FortuneController`/`ShareController`, `SajuAnalysisService`/`FortuneService`/`ShareService`, Springdoc, Caffeine L1 |
| W4 | **LLM Seed** | `KeyMessageSeeder`/`CategoryFortuneSeeder`/`DailyFortuneSeeder` 작성 + 1회 시드 (50+300+1000) |
| W4\~W5 | **Frontend Bootstrap** | `frontend/` Next.js 14 부트스트랩, shadcn/ui, S1\~S3 화면 |
| W5\~W6 | **Frontend Result** | S4·S5 결과 화면 (도넛/카드/아코디언/오늘 카드/액션바) |
| W6 | **Share + OG** | S6 공유 페이지 + `@vercel/og` 동적 이미지 + 도메인 분리 + CORS |
| W6\~W7 | **Infra + 출시** | Dockerfile, ECS Task Definition, Route 53, ALB, 카카오 redirect URI 등록, Vercel 도메인 연결 |
| **W7 종료** | **MVP 출시** | YC 배치 출시 |

---

## 출시 게이트 체크리스트

### 백엔드
- [ ] V1+V2 Flyway 마이그레이션이 빈 MySQL 컨테이너에서 통과 (Testcontainers)
- [ ] **골든 테스트셋 50건** lunar-java 어댑터 통과 (KASI 기준 1분 이내 일치, R1)
- [ ] `key_message` 350개·`daily_fortune_template` ~1000개 시드 완료 (R14)
- [ ] AWS Secrets Manager에서 시크릿 정상 주입 (R8)
- [ ] ECS Task Definition + ALB Health Check 200
- [ ] CloudWatch JSON 로그 + Actuator 메트릭 노출 정상

### 프론트엔드
- [ ] Lighthouse Performance ≥ 90, Accessibility ≥ 95, SEO ≥ 100
- [ ] `@vercel/og` 동적 OG 이미지 생성 정상 (S4 + S6)
- [ ] PWA manifest + service worker (offline 정적 자산만)
- [ ] Vercel 무료 티어 한도 모니터링 (R18)

### 통합
- [ ] 카카오 로그인 → 출생정보 입력 → 결과 → 카테고리·오늘 운세 → 공유 e2e 시나리오 통과
- [ ] CORS `withCredentials` 동작 확인 (cross-subdomain `saju.app` ↔ `api.saju.app`)
- [ ] 공유 링크 비로그인 진입 → "나도 보기" → 카카오 로그인 → 결과 화면 e2e

---

## 변경 이력

| 일자 | 변경 |
|---|---|
| 2026-04-28 | 9단계 설계 초안 (자체 엔진 유지, JWT 유지, MVP 4주) |
| 2026-04-29 (1차) | 공동 의사결정 라운드 11건 — lunar-java/세션-jdbc 전환, MVP 스코프 잠금, 모듈 제거 |
| 2026-04-29 (2차) | UX 라운드 17건 — 친구·가족, MZ 페르소나, SNS 명함, 자시 강제, 카테고리·오늘 운세, 공유 일급 |
| 2026-04-29 (3차) | 프론트엔드 Next.js 14 + 모노레포 도입, ai/ 유지, ADR-21\~26 신규, MVP 7주, step9 신규 |
