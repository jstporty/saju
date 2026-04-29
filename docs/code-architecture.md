# Code Architecture

## 패키지 구조

```
com.company.saju
├── SajuBackendApplication.java
├── common/
│   ├── config/CacheConfig.java              — Caffeine 캐시 빈 설정
│   ├── domain/BaseEntity.java               — id 필드
│   ├── domain/BaseTimeEntity.java           — created_at, updated_at (JPA Auditing)
│   ├── dto/ApiResponse.java                 — 표준 응답 래퍼
│   ├── exception/BusinessException.java
│   ├── exception/ErrorCode.java             — 에러 코드 열거 + HTTP 상태 매핑
│   ├── exception/GlobalExceptionHandler.java — RFC 7807 ProblemDetail 반환
│   └── util/IdGenerator.java                — UUID v4
├── auth/
│   ├── adapter/in/web/AuthController.java   — GET /api/v1/auth/me, DELETE /api/v1/auth/me
│   ├── adapter/out/oauth2/KakaoOAuthSuccessHandler — OAuth 성공 → 세션 발급 → redirect
│   ├── adapter/out/persistence/entity/OAuthUserEntity
│   ├── adapter/out/persistence/repository/OAuthUserRepository
│   ├── application/service/AuthService.java — getCurrentUser, deleteAccount(연쇄 소프트 삭제)
│   └── infrastructure/config/
│       ├── SecurityConfig.java              — CORS, Session, OAuth2Login, Logout 설정
│       ├── SessionConfig.java               — @EnableJdbcHttpSession, 14일 TTL
│       └── JpaAuditingConfig.java
├── saju/
│   ├── adapter/in/web/
│   │   ├── ChartController.java             — POST/GET/DELETE /api/v1/charts
│   │   ├── FortuneController.java           — GET /api/v1/charts/{id}/categories, /today
│   │   ├── ShareController.java             — POST/GET /api/v1/shares
│   │   └── AdminSeederController.java       — POST /admin/seed/key-messages, /daily-fortunes
│   ├── adapter/out/engine/
│   │   ├── LunarJavaSajuEngine.java         — SajuEnginePort 구현체 (lunar-java 연동)
│   │   ├── KoreanLmtAdjuster.java           — KST +30분 LMT 보정
│   │   ├── HourPillarAdjuster.java          — 자시/야자시 처리
│   │   ├── ElementAnalyzer.java             — 오행 점수 + dominant() 계산
│   │   ├── TenGodsService.java              — 십신 카운트 + 6카드 선택
│   │   └── DailyStemBranchProvider.java     — 일진 천간/지지 계산
│   ├── adapter/out/persistence/
│   │   ├── entity/SajuChartEntity.java      — @SQLRestriction("deleted_at IS NULL")
│   │   ├── entity/SajuShareLinkEntity.java
│   │   ├── entity/KeyMessageEntity.java
│   │   ├── entity/DailyFortuneTemplateEntity.java
│   │   └── repository/ (각 Entity용 JPA Repository)
│   ├── application/
│   │   ├── port/in/CreateChartUseCase.java
│   │   ├── port/in/GetChartUseCase.java
│   │   ├── port/out/SajuEnginePort.java
│   │   ├── service/SajuAnalysisService.java — 차트 생성/조회/목록/삭제 (매 GET 엔진 재계산)
│   │   ├── service/FortuneService.java      — 카테고리 운세 + 오늘의 운세 (Caffeine 캐시)
│   │   ├── service/ShareService.java        — 공유링크 생성/공개 조회 (PII 마스킹)
│   │   ├── seeder/KeyMessageSeeder.java     — OVERALL 50행 LLM 시드
│   │   ├── seeder/CategoryFortuneSeeder.java — 카테고리 250행 LLM 시드
│   │   ├── seeder/DailyFortuneSeeder.java   — 일진 600행 LLM 시드
│   │   └── dto/ (BirthInfo, SajuAnalysis, ElementsScore, FourPillarsDto, TenGodsCount, DayPillar)
│   └── domain/model/ (Element, FourPillars, Pillar, GanJi)
├── ai/
│   ├── adapter/HuggingFaceClient.java       — RestClient, OpenAI 호환 응답 파싱
│   ├── application/HuggingFaceService.java  — generateText(seeder용), generateInterpretation
│   └── application/PromptBuilder.java       — 분석 프롬프트 + 시드용 프롬프트 빌더
└── user/domain/model/ (Gender, CalendarType, UserStatus)
```

---

## 레이어 의존 방향

```
Controller → UseCase(Port/In) → Service → Port/Out(Engine, Repository)
                                         ↑
                               Adapter/Out 구현체
```

- Controller는 Service를 직접 참조하지 않고 UseCase 인터페이스를 통해 호출.
- `SajuEnginePort` — Engine 구현체 교체를 위한 포트. 현재 구현체: `LunarJavaSajuEngine`.

---

## 핵심 흐름

### 차트 생성 (POST /api/v1/charts)
```
ChartController
  → SajuAnalysisService.createChart(req, userId)
    → SajuEnginePort.analyze(BirthInfo)         # lunar-java 계산
    → SHA-256 calculation_key 생성
    → SajuChartRepository.findByUserIdAndCalculationKey()  # 중복 체크
    → 중복: 기존 차트 반환 / 신규: save 후 반환
    → KeyMessageRepository.findBy...(OVERALL)   # key_message 룩업
```

### 카테고리 운세 (GET /api/v1/charts/{id}/categories)
```
FortuneController
  → FortuneService.getCategories(chartId, userId)
    → [Caffeine keyMessage 캐시 hit] → 즉시 반환
    → [miss] KeyMessageRepository 6회 룩업 → 캐시 적재
```

### LLM 시드 (POST /admin/seed/key-messages)
```
AdminSeederController
  → KeyMessageSeeder.seed()      # OVERALL 50행
  → CategoryFortuneSeeder.seed() # WEALTH/LOVE/HEALTH/CAREER/FAMILY 250행
    → HuggingFaceService.generateText(PromptBuilder.buildKeyMessagePrompt())
    → KeyMessageRepository.save() (skip if exists)
```

### 회원 탈퇴 (DELETE /api/v1/auth/me)
```
AuthController
  → AuthService.deleteAccount(userId)
    → SajuShareLinkRepository.revokeAllByUserId()
    → SajuChartRepository.findAllByUserId() → 각 chart.softDelete()
    → OAuthUserEntity.softDelete()
  → session.invalidate()
```

---

## API 엔드포인트 전체 목록

```
인증
  GET    /api/v1/auth/me              — 세션 사용자 정보
  DELETE /api/v1/auth/me              — 회원 탈퇴 (연쇄 소프트 삭제 + 세션 무효화)
  POST   /api/v1/auth/logout          — 세션 삭제 (Spring Security 처리)

차트
  POST   /api/v1/charts               — 사주 생성 (중복 시 기존 반환)
  GET    /api/v1/charts?page=0&size=20 — 이력 목록 (offset 페이지)
  GET    /api/v1/charts/{id}          — 차트 상세 (엔진 재계산)
  DELETE /api/v1/charts/{id}          — 소프트 삭제 + 공유링크 폐기

운세
  GET    /api/v1/charts/{id}/categories — 6개 카테고리 운세 (Caffeine 캐시)
  GET    /api/v1/charts/{id}/today      — 오늘의 운세 (SELF 전용, 24h 캐시)

공유
  POST   /api/v1/shares               — 공유링크 생성
  GET    /api/v1/shares/{token}        — 공개 차트 조회 (비로그인 허용, PII 마스킹)

어드민 (Seeder)
  POST   /admin/seed/key-messages     — key_message 350행 LLM 생성 (idempotent)
  POST   /admin/seed/daily-fortunes   — daily_fortune_template 600행 LLM 생성 (idempotent)
```

---

## 인프라

```
로컬:   Podman + podman-compose (MySQL + LocalStack)
프로덕: AWS ECS Fargate → ECR (GitHub Actions CI/CD)
도메인: saju.app (Vercel/프론트), api.saju.app (ECS/백엔드)
세크릿: AWS Secrets Manager (로컬: LocalStack saju/local/db)
```

---

## 기술 스택 요약

| 구분 | 선택 |
|---|---|
| 백엔드 | Kotlin/Java 21, Spring Boot 3.5.x, Gradle Kotlin DSL |
| ORM | Spring Data JPA + Querydsl 5.1.0 |
| 마이그레이션 | Flyway 10.x |
| 세션 | spring-session-jdbc (JDBC), 14일 TTL |
| 캐시 | Caffeine L1 |
| 인증 | Kakao OAuth 2.0 → JDBC Session (JWT 없음) |
| 사주 엔진 | cn.6tail:lunar:1.7.7 + KST 보정 어댑터 |
| LLM | HuggingFace Inference API (Llama-3.1-8B-Instruct) |
| 프론트 | Next.js 14+ App Router, TypeScript, Tailwind, shadcn/ui |
