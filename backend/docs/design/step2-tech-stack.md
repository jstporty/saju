# Step 2: 기술 스택 — 라이브러리/버전 핀 (2026-04-29 갱신)

> 기준: MVP 약 7주 출시 (ADR-22로 갱신, 카테고리 운세·오늘의 운세·프론트엔드 포함). lunar-java + session-jdbc + Next.js 모노레포 전제.
> 변경 이력: 2026-04-28 초안 → 2026-04-29 SB 3.5.x 업그레이드 + jjwt 제거 + lunar-java/spring-session-jdbc + ai/ 유지 + 프론트엔드 Next.js 도입.

## 핀 확정 의존성 (MVP)

| 카테고리 | 라이브러리 | 버전 | 채택 사유 | 대안 / 비고 |
|---|---|---|---|---|
| **런타임** | Eclipse Temurin Java | 21 (LTS) | Virtual Threads, 장기 지원 | Java 25 LTS는 출시 후 검토 |
| **프레임워크** | Spring Boot | **3.5.x** | HikariCP 6.x, 최신 보안 패치, Java 21 통합 | 3.2.2에서 업그레이드 (ADR-18) |
| **의존성 관리** | io.spring.dependency-management | 1.1.x | Spring BOM 자동 정렬 | — |
| **ORM** | Spring Data JPA (Hibernate 6.x) | Boot BOM | Jakarta Persistence 3.1 | — |
| **커넥션풀** | HikariCP | Boot BOM (~6.x) | 최고 성능 JDBC 풀 | VT pinning 회피로 JPA 경로는 플랫폼 스레드 (ADR-02) |
| **QueryDSL** | querydsl-jpa | **5.1.0:jakarta** | 타입 안전 동적 쿼리 | KSP는 미채택 (Java 프로젝트 유지) |
| **DB 드라이버** | mysql-connector-j | Boot BOM | MySQL 8.x | — |
| **마이그레이션** | flyway-core + flyway-mysql | **10.x** | 운영 DDL 추적 (ADR-09) | Liquibase 미채택 |
| **인증** | Spring Security | Boot BOM | OAuth2 Client | — |
| **OAuth2** | spring-boot-starter-oauth2-client | Boot BOM | 카카오 OAuth2 처리 | — |
| **세션** | **spring-session-jdbc** | Boot BOM | 세션 저장소 (ADR-15) | jjwt 제거 |
| **사주 엔진** | **`cn.6tail:lunar-java`** | **1.7.x** | 음력·간지·24절기 (ADR-13) | 한국식 보정은 자체 어댑터 |
| **직렬화** | Jackson | Boot BOM | Spring MVC 기본 | — |
| **유틸** | uuid-creator | 5.3.x | UUID v7 시간 정렬 | java.util.UUID v4 |
| **매핑** | MapStruct | 1.6.x | 컴파일 타임 매핑 | — |
| **코드 생성** | Lombok + lombok-mapstruct-binding | 1.18.x + 0.2.0 | Boilerplate 제거 | — |
| **검증** | spring-boot-starter-validation | Boot BOM | Bean Validation 3.0 | — |
| **문서화** | springdoc-openapi-starter-webmvc-ui | **2.6.x** | OpenAPI 3.1 자동 생성 | SB 3.5 호환 버전 |
| **모니터링** | spring-boot-starter-actuator | Boot BOM | Health/Metrics | — |
| **로그** | logback-classic + logstash-logback-encoder | 8.x | CloudWatch JSON 로그 | — |
| **AWS SDK** | AWS SDK v2 (s3, secretsmanager) | 2.28.x | LocalStack 호환 (ADR-16) | v1 미채택 |
| **캐시** | Caffeine | Boot BOM (~3.1.x) | L1 인메모리 (ADR-05) | Redis는 ECS Task ≥ 3 시 |
| **테스트** | spring-boot-starter-test | Boot BOM | JUnit 5 + Mockito + AssertJ | — |
| **테스트** | spring-security-test | Boot BOM | Security Mock | — |
| **테스트 인프라** | testcontainers-mysql + testcontainers-localstack | 1.20.x | 통합 테스트 | — |

## 제거 대상 (Engine·Auth·Module 마이그레이션)

| 라이브러리 / 모듈 | 사유 |
|---|---|
| `io.jsonwebtoken:jjwt-*` 0.12.x | session-jdbc 전환 (ADR-15) |
| `engine/` 전 패키지 | lunar-java로 대체 (ADR-13) |
| `notification/` 전 패키지 | 모듈 제거 (ADR-19) |
| `hanja/` 전 패키지 | 모듈 제거 (ADR-19) |

> **`ai/` 패키지 유지 (ADR-19 부분 변경)**: 키 메시지 LLM precompute(50조합) + 카테고리 운세 LLM precompute(300조합) + 오늘의 운세 LLM precompute(1000조합) 시드 도구로 활용. HuggingFaceClient·PromptBuilder는 어드민 전용 시드 작성 시에만 호출. 사용자 요청 경로에는 LLM 호출 없음(SLO 보장).

## 단계별 추가 시점

| 라이브러리 | 추가 시점 | 이유 |
|---|---|---|
| `flyway-core`, `flyway-mysql` | W1 Refactor 스프린트 | DDL 마이그레이션 (ADR-09) |
| `spring-session-jdbc` | W2 Auth 스프린트 | JWT 제거와 동시 |
| `cn.6tail:lunar-java` | W2~W3 Engine 스프린트 | 어댑터 작성 시점 |
| `springdoc-openapi-starter-webmvc-ui:2.6.x` | W4 Domain 스프린트 | API 문서화 |
| `aws-sdk-java-v2:2.28.x` (s3, secretsmanager) | W1 Pre-work | Secrets Manager 통합 (ADR-16) |
| `testcontainers-mysql`, `testcontainers-localstack` | W2~W3 Engine 스프린트 | 골든 테스트셋 + 통합 테스트 |
| `spring-boot-starter-cache` + caffeine | Post-MVP | AI 해석 캐싱 시 |

## 버전 제약 메모

- **Spring Boot 3.5.x → 6.x HikariCP**: 5.1.0+에서 synchronized → ReentrantLock 전환 완료. yield-spin 이슈는 잔존 → ADR-02 유지.
- **QueryDSL 5.1.0:jakarta**: APT는 `querydsl-apt:5.1.0:jakarta` 페어. SB 3.5 호환 검증됨.
- **lunar-java 1.7.x**: Apache 2.0. JDK 8+ 호환. Java 21에서 동작 확인 필요(W2~W3 스프린트 게이트).
- **spring-session-jdbc**: SB 3.5.x BOM 버전 사용. SPRING_SESSION/SPRING_SESSION_ATTRIBUTES 테이블은 V2 마이그레이션에 포함.
- **MapStruct + Lombok**: `lombok-mapstruct-binding:0.2.0` 필수. 어노테이션 프로세서 순서 `lombok` → `mapstruct-processor`.

---

## 프론트엔드 스택 (frontend/, Next.js 모노레포 신규, ADR-25)

> 워크스페이스 루트에 `frontend/` 디렉토리 추가. 별도 `package.json` 관리. Vercel 배포로 백엔드와 독립 CI/CD.

| 카테고리 | 라이브러리 | 버전 | 채택 사유 | 대안 / 비고 |
|---|---|---|---|---|
| **프레임워크** | next | **14.2.x** (App Router) | RSC + 동적 OG 이미지 + Vercel 친화 | Vite 기각 (OG·SEO 추가 부담) |
| **언어** | typescript | 5.4.x | 타입 안전, App Router 전제 | — |
| **런타임** | Node.js | 20 LTS | Next.js 14 권장 | — |
| **UI 프레임워크** | tailwindcss | 3.4.x | 유틸리티 CSS, MZ 페르소나 친화 | — |
| **컴포넌트** | shadcn/ui (Radix UI primitives) | 최신 | 접근성 + 커스터마이징 | Material UI 기각 (디자인 잠금) |
| **아이콘** | lucide-react | 0.4xx | shadcn 기본 | — |
| **차트** | recharts | 2.12.x | 오행 도넛, React 친화 | Chart.js 기각 (React 통합 약함) |
| **상태 관리 (서버)** | @tanstack/react-query | **5.x** | 서버 상태 캐시·재검증 | SWR 가능하나 ecosystem TS-Q 우세 |
| **상태 관리 (클라이언트)** | zustand | 4.5.x | 폼·UI 토글 가벼움 | Redux 기각 (오버킬) |
| **폼** | react-hook-form + zod | 7.5x + 3.23.x | 검증 통합 | Formik 기각 |
| **OG 이미지** | @vercel/og | 0.6.x | Edge Runtime 동적 OG | Java 백엔드 이미지 라이브러리 기각 |
| **PWA** | @serwist/next | 9.x | manifest + service worker (push 미사용, ADR-23) | next-pwa 가능 |
| **HTTP** | 표준 fetch (App Router) | — | `credentials: 'include'` (세션 쿠키) | axios 미사용 |
| **테스트** | vitest + @testing-library/react | 1.6.x + 16.x | Vite 호환 빠른 단위 테스트 | Jest 가능 |
| **E2E** | playwright | 1.4x | Post-MVP | — |
| **린팅** | eslint + eslint-config-next + prettier | 8.x + 14.x + 3.x | Next 표준 | — |
| **분석** | @vercel/analytics | 1.x | 무료, 즉시 | PostHog Post-MVP |

### 프론트엔드 단계별 추가

| 라이브러리 | 추가 시점 | 이유 |
|---|---|---|
| next, react, typescript, tailwindcss | W4 Frontend Setup | 초기 프로젝트 부트스트랩 |
| shadcn/ui (CLI) | W4 | UI 컴포넌트 첫 화면 (Landing) |
| @tanstack/react-query | W5 | 결과 화면·이력 데이터 페칭 |
| recharts | W5 | 오행 도넛 |
| @vercel/og | W6 | 공유 페이지 OG 이미지 |
| @serwist/next | W7 | PWA 마무리 |

### PWA 정책 (ADR-23)

- manifest.json + service worker (오프라인 캐시는 정적 자산만 — 사주 데이터는 항상 최신)
- 푸시 알림 **미사용** (사용자 자발적 재방문 = "오늘의 운세" 자체가 retention 훅)
- 설치 가능 웹앱 (Add to Home Screen 활성)

### 도메인·CORS·세션 (백엔드 영향)

- 도메인: `saju.app`(프론트, Vercel) / `api.saju.app`(백엔드, ECS)
- 세션 쿠키: `Domain=.saju.app; SameSite=None; Secure; HttpOnly` (cross-subdomain)
- 백엔드 `WebMvcConfigurer`에 `https://saju.app` allowOrigin + `allowCredentials=true`
- 카카오 OAuth Redirect URI: `https://api.saju.app/login/oauth2/code/kakao` → 성공 시 프론트 `https://saju.app/`로 302 redirect

### 백엔드 영향 (어드민 시드 도구 추가)

| 컴포넌트 | 위치 | 사용 시점 |
|---|---|---|
| `KeyMessageSeeder` | `saju.application.fortune` (어드민) | 50조합 키 메시지 LLM 호출 → `key_message` 테이블 INSERT |
| `CategoryFortuneSeeder` | `saju.application.fortune` (어드민) | 300조합 카테고리 운세 LLM 호출 → `key_message`(category) INSERT |
| `DailyFortuneSeeder` | `saju.application.fortune` (어드민) | 1000조합 오늘의 운세 LLM 호출 → `daily_fortune_template` INSERT |
| `DailyStemBranchProvider` | `saju.adapter.out.engine` | 임의 날짜 → 일진 천간/지지 (lunar-java 어댑터) |
