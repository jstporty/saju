# Step 8: Architecture Decision Records (ADR) — 2026-04-29 갱신

> 형식: [MADR](https://adr.github.io/madr/) 경량 버전
> 상태: `Accepted` | `Superseded` | `Deprecated`
> 본 갱신: 공동 의사결정 라운드 + UX 라운드 + 카테고리·오늘의 운세·프론트 도입 반영.
> 변경 요약: ADR-01/06/11 Superseded, ADR-12 Superseded(by ADR-21), ADR-13\~20 신규, ADR-19/20 부분 변경/Supersede, ADR-21\~26 신규.

---

## ADR-01: 사주 엔진 — 직접 구현 유지 (Superseded by ADR-13)

- **날짜**: 2026-04-28
- **상태**: ~~Accepted~~ → **Superseded by ADR-13** (2026-04-29)

### 컨텍스트

당시 코드 재마운트 시 `engine/` 패키지가 이미 완성되어 있어 자체 구현 유지로 결정.

### 무효 사유

공동 의사결정 라운드에서 외부 라이브러리(lunar-java) + 한국식 보정 어댑터로 가는 것이 장기 유지보수에 유리하다고 합의.

---

## ADR-02: Virtual Threads — JPA 경로 비활성화

- **날짜**: 2026-04-28
- **상태**: Accepted (변경 없음)

### 결정 (요약)

JPA/`@Transactional` 경로 VT 비적용. 외부 IO 전용 `vtIoExecutor` 빈으로만 VT 사용.

### 근거

HikariCP `ConcurrentBag` yield-spin (≤ 7.0.2). Spring Boot 3.5.x도 6.x 번들. JEP 491(Java 24+)에서 해소되나 본 MVP는 Java 21.

---

## ADR-03: 배포 — ECS Fargate Express Mode

- **날짜**: 2026-04-28
- **상태**: Accepted (변경 없음)

App Runner 2026-04-30 신규 가입 종료 → ECS Express Mode.

---

## ADR-04: 사용자 주체 — `oauth_user` 단일 테이블

- **날짜**: 2026-04-28
- **상태**: Accepted (변경 없음)

기존 `users` 테이블 미사용. `oauth_user`가 메인 사용자 테이블. `saju_chart.user_id` → `oauth_user.id` FK.

---

## ADR-05: 캐시 — Caffeine L1 단일 레이어

- **날짜**: 2026-04-28
- **상태**: Accepted (MVP에서는 미적용)

MVP는 캐시 도입 보류. Post-MVP에서 공유 차트·AI 해석 결과 도입 시 Caffeine L1 적용.

---

## ADR-06: 비인증 사용자 사주 계산 정책 (Superseded by ADR-14)

- **날짜**: 2026-04-28
- **상태**: ~~Accepted~~ → **Superseded by ADR-14** (2026-04-29)

### 무효 사유

Guest 허용 정책은 AI 비용·전환율 트레이드오프 분석 후 차단으로 변경.

---

## ADR-07: MySQL 8.x 유지

- **날짜**: 2026-04-28
- **상태**: Accepted

---

## ADR-08: `saju_results` vs `saju_chart` 역할 분리

- **날짜**: 2026-04-28
- **상태**: Accepted (보강)

### 추가 메모 (2026-04-29)

MVP에서는 `saju_results` 테이블의 캐시 의미가 약함(공유·AI 외). 보존하되 신규 INSERT/SELECT 차단. `saju_chart.calculation_key`는 `(user_id, calculation_key)` 복합 UNIQUE로 변경 권장.

---

## ADR-09: Flyway 도입 — `ddl-auto: validate`

- **날짜**: 2026-04-28
- **상태**: Accepted

W1 Pre-work 스프린트에 적용.

---

## ADR-10: AI 해석 Fallback 정책 (Deprecated for MVP)

- **날짜**: 2026-04-28
- **상태**: ~~Accepted~~ → **MVP 외 (ADR-20)**

### 무효 사유

AI 해석 자체가 MVP 외 기능. Post-MVP 부활 시 ADR-22로 재합의.

---

## ADR-11: 세션 저장 — JWT 유지 (Superseded by ADR-15)

- **날짜**: 2026-04-29 오전
- **상태**: ~~Accepted~~ → **Superseded by ADR-15** (2026-04-29 오후)

### 무효 사유

기존 JWT 자산 보존 vs 사용자가 원래 선택한 spring-session-jdbc 사이에서, 표준 세션 모델·CSRF 자연스러운 적용·다중 인스턴스 호환·OAuth2 표준 핸들러 사용을 우선해 session-jdbc로 전환.

---

## ADR-12: 차트 스코프 — 본인 차트만(MVP) (Superseded by ADR-21)

- **날짜**: 2026-04-29
- **상태**: ~~Accepted~~ → **Superseded by ADR-21** (2026-04-29 UX 라운드)

### 무효 사유

UX 라운드 결과, "친구·가족 차트도 보고 싶다"는 핵심 retention/공유 훅으로 확인. `subject_name` 컬럼은 유지하되, 클라이언트 UX에 본인/타인 토글을 노출하는 ADR-21로 대체.

---

## ADR-13: 사주 엔진 — `lunar-java` + 한국식 보정 어댑터 (Supersedes ADR-01)

- **날짜**: 2026-04-29
- **상태**: Accepted

### 컨텍스트

MVP 출시까지 검증된 외부 만세력 라이브러리를 활용해 자체 천문 계산 부담을 덜어야 한다. 기존 `engine/` 패키지가 있으나 유지보수 비용·정확도 검증 부담을 외부 라이브러리로 이전하기로 결정.

### 결정

**`cn.6tail:lunar-java` 1.7.x 채택. 한국식 보정(KST +30분 LMT, 자시/야자시)은 자체 어댑터로 래핑.** 기존 `engine/` 패키지 제거.

### 근거

- 검증된 음력·간지·24절기 데이터 (lunar-java)
- 한국 명리 특화 부분만 어댑터로 분리 → 책임 명확
- 자체 구현 유지 비용(테스트·버그 추적) > 라이브러리 의존 비용

### 트레이드오프

- 외부 의존성 추가, 라이브러리 결과와 KASI 데이터 1분 이내 일치 검증 필요
- 골든 테스트셋 50건이 출시 게이트

### 결과

- `saju.adapter.out.engine` 하위에 `LunarJavaSajuEngine`, `KoreanLmtAdjuster`, `HourPillarAdjuster` 작성
- 기존 `engine/` 전체 제거
- 골든 테스트셋 50건 작성 (자시·야자시·절입 시각·일반 케이스 균형)

---

## ADR-14: Guest 사주 계산 — 차단 (Supersedes ADR-06)

- **날짜**: 2026-04-29
- **상태**: Accepted

### 컨텍스트

비로그인 Guest 허용 시 AI 비용·데이터 누적 통제가 어렵다. MVP에서는 AI 해석 자체가 빠지므로(ADR-20) Guest 허용의 핵심 전환율 인센티브가 사라짐.

### 결정

**`/api/v1/saju/**` 전체 인증 필수.** Guest 호출은 401 반환.

### 근거

- 데이터·보안 거버넌스 단순화
- 사용자 식별 가능한 경우만 차트 저장 → 이력 기능 일관성
- AI 비용 통제 (Post-MVP에서 부활 시에도 인증된 사용자에게만 호출)

### 결과

- `SecurityConfig`에서 `/api/v1/saju/**` `authenticated()`
- `SajuController.analyzeSaju()`의 `guest_{timestamp}` 폴백 로직 제거

---

## ADR-15: 세션 저장 — `spring-session-jdbc` (Supersedes ADR-11)

- **날짜**: 2026-04-29
- **상태**: Accepted

### 컨텍스트

JWT 유지 vs spring-session-jdbc 전환을 재검토한 결과, 표준 세션 모델과 OAuth2 success handler 자연스러운 통합·CSRF 적용·다중 인스턴스 즉시 폐기·refresh rotation 복잡성 제거 등의 장점으로 session-jdbc 선택.

### 결정

**`spring-session-jdbc` 채택.** Spring Session이 `SPRING_SESSION`/`SPRING_SESSION_ATTRIBUTES` 테이블에 세션 저장. 세션 TTL 14일 (rolling).

### 근거

- 카카오 OAuth2 success handler가 `HttpSession`을 직접 채우므로 코드 단순
- Refresh Token 회전·재사용 탐지 등의 추가 보안 코드 불필요
- 즉시 폐기: `HttpSession.invalidate()`로 끝남
- 다중 인스턴스: 모든 노드가 동일 DB 세션 참조

### 트레이드오프

- 매 요청마다 DB 조회 (캐시는 spring-session 자체 메모리 캐시로 완화)
- 쿠키 기반이므로 CORS `withCredentials` + 도메인 화이트리스트 필요

### 결과

- `JwtTokenProvider`, `JwtAuthenticationFilter`, `RefreshTokenEntity`/`RefreshTokenRepository` 제거
- `SecurityConfig` stateful로 재작성
- V2 마이그레이션에 `SPRING_SESSION`/`SPRING_SESSION_ATTRIBUTES` 추가, `refresh_token` DROP

---

## ADR-16: Secrets 통합 — LocalStack + AWS Secrets Manager

- **날짜**: 2026-04-29
- **상태**: Accepted

### 컨텍스트

`application.yml`에 JWT secret, 카카오 client_secret, HuggingFace 토큰이 평문 노출. git 이력 점검 + 즉시 회전 + Secrets Manager 통합이 필요.

### 결정

**로컬: LocalStack Secrets Manager. 운영: AWS Secrets Manager.** Spring Cloud AWS Secrets Manager로 부트 타임에 시크릿을 환경변수처럼 주입. `application.yml`에는 시크릿 키 이름만 남김.

### 근거

- 운영 환경과 로컬 환경 동일 인터페이스
- 시크릿 회전·감사·접근 통제 표준 도구 활용
- 배포 자동화 시 ECS Task IAM 역할로 권한 부여

### 결과

- W1 Pre-work: 노출된 시크릿 즉시 회전, git history 점검(필요 시 BFG/filter-repo)
- `infra/podman-compose.yml`에 LocalStack 추가
- `aws-sdk-java-v2` (s3, secretsmanager) 의존성 추가
- 배포 준비 단계에서 ECS Task Role + Secrets Manager ARN 매핑

---

## ADR-17: PII Anonymize (Post-MVP)

- **날짜**: 2026-04-29
- **상태**: Accepted (Post-MVP)

### 결정

회원 탈퇴 시 hard delete 대신 익명화. `oauth_user.email/name`은 SHA-256 해시 컬럼으로 분리 저장 후 원본 삭제. `saju_chart.user_id` NULL + `anonymous_user_id`(해시 키) 보존.

### 근거

- 통계·분석 데이터 보존 (가설 검증용)
- 국내 개인정보보호법: 즉시 파기 의무는 없음. 분리 보관 후 30일 이내 파기 관행 적용 가능
- GDPR right-to-be-forgotten: 식별 불가 상태로 변환

### 결과

- V5 마이그레이션 (Post-MVP): `oauth_user`에 `email_hash`, `name_hash` 추가, 원본 컬럼 NULL 허용
- V5 마이그레이션: `saju_chart`에 `anonymized_at`, `anonymous_user_id` 추가
- `DELETE /api/v1/users/me` 엔드포인트 (V4+)

---

## ADR-18: Spring Boot 3.5.x 업그레이드

- **날짜**: 2026-04-29
- **상태**: Accepted

### 컨텍스트

기존 3.2.2는 EOL이 가까워졌고, lunar-java/spring-session-jdbc 도입이 의존성 재구성 시점이라 동시에 진행하는 것이 효율적.

### 결정

**Spring Boot 3.5.x로 업그레이드.** Querydsl `5.1.0:jakarta`, Springdoc `2.6.x` 등 페어링 버전 조정.

### 근거

- HikariCP 6.x 번들로 보안 패치
- Spring Security 6.3+ 기능
- 출시 전에 마이그레이션을 끝내야 출시 후 부담 없음

### 트레이드오프

- 마이너 호환 이슈 가능 (로그·설정 키)
- 일정 +1\~2일

### 결과

- W1 Refactor 스프린트에 포함
- `gradle.properties`의 `springBootVersion` 갱신
- 빌드/테스트 통과 확인 후 다음 스프린트 진입

---

## ADR-19: 모듈 제거 — `notification`, `hanja` (부분 변경: `ai/` 유지)

- **날짜**: 2026-04-29 (초안) / 2026-04-29 부분 변경
- **상태**: Accepted (부분 변경)

### 결정 (변경된 형태)

- `notification/`, `hanja/` 패키지는 코드베이스에서 **제거** (V2 마이그레이션에서 테이블도 DROP)
- **`ai/` 패키지는 유지**. 단, 사용자 요청 경로(컨트롤러)에서는 호출하지 않고, 어드민 시드 도구(`KeyMessageSeeder`, `CategoryFortuneSeeder`, `DailyFortuneSeeder`)에서만 호출 (ADR-24)

### 근거

- MVP 단순화. 빌드/테스트 시간 절감 (`notification`, `hanja`)
- `ai/` 유지 사유: 키 메시지 + 카테고리 운세 + 오늘의 운세 LLM precompute의 시드 도구로 즉시 사용 가능 (ADR-24). 사용자 요청 경로에서는 호출 안 됨 → SLO 영향 없음.

### 결과

- W1 Refactor 스프린트에서 `notification/`, `hanja/` 패키지 삭제 + V2 마이그레이션에서 테이블 DROP
- `ai/HuggingFaceService`, `ai/PromptBuilder` 등은 코드 유지 + Seeder에서 사용
- `application.yml`의 huggingface 설정 키 유지(어드민 호출용)

---

## ADR-20: MVP 최소 스코프 lock (Superseded by ADR-22)

- **날짜**: 2026-04-29
- **상태**: ~~Accepted~~ → **Superseded by ADR-22** (2026-04-29 UX·기능 확장 라운드)

### 무효 사유

UX 라운드 + 사용자의 카테고리 운세·오늘의 운세·React 프론트 추가 결정으로 MVP 범위 확장. ADR-22로 갱신. 결정 자체는 보존하되 일정·범위 모두 재조정.

---

## ADR-21: 차트 스코프 확장 — 친구·가족 허용 (Supersedes ADR-12)

- **날짜**: 2026-04-29 (UX 라운드)
- **상태**: Accepted

### 컨텍스트

UX 리서치에서 "친구·가족 사주도 보고 싶다"는 1차 페르소나 동기 확인. 본인만 허용 시 retention과 공유 훅이 약함.

### 결정

`POST /charts` 요청에 `subjectKind = SELF | OTHER` + `subjectName` 필드. 본인 차트는 항상 1개만 (수정 가능), 타인 차트는 다수 허용. UX는 본인/다른 사람 토글 + 본인 차트 항상 최상단 정렬.

### 근거

- 친구·가족 분석 자체가 공유의 자연스러운 트리거
- DB 스키마는 이미 `subject_name` 컬럼 보유 → 추가 비용 없음
- PII 부담 한정적 (사용자 본인이 자발적 입력, 본인 계정에 귀속)

### 결과

- `saju_chart.subject_kind` 컬럼 추가 (V2)
- `ChartCreateRequest.subjectKind` 필드 추가
- 클라이언트 토글 UX (S3) + 이력 정렬 규칙

---

## ADR-22: MVP 스코프 갱신 — 약 7주 (Supersedes ADR-20)

- **날짜**: 2026-04-29
- **상태**: Accepted

### 컨텍스트

MVP에 다음 기능 포함을 결정:
- 공유 링크 (viral loop)
- 카테고리 운세 6종 (총운/금전/연애/건강/직업·학업/가족·인간관계)
- 오늘의 운세 (재방문 훅)
- 5가지 에너지 도넛 + 성격 카드 6장 (R2 정보 위계)
- 프론트엔드 (Next.js 14+, ADR-25)

### 결정

**MVP 7주 스코프 (수정 7개 항목):**

1. 카카오 OAuth 로그인 (session-jdbc)
2. 출생정보 입력 (본인/타인 토글, 자시 모달, 시간 모름)
3. 사주 계산 (동기, lunar-java + 한국식 어댑터) + **오행 점수** + **십신 카운트**
4. **키 메시지 + 카테고리 운세 6종** (사전 생성 캐시 룩업)
5. **오늘의 운세** (사전 생성 + 24h 캐시)
6. **공유 링크** (POST /shares, GET /shares/{token} 비로그인)
7. 사주 이력 목록·단건·삭제 (본인·친구·가족)

**프론트엔드 신규:**
- Next.js 14 (App Router) + TypeScript
- shadcn/ui + Tailwind + Recharts + TanStack Query
- @vercel/og (동적 OG)
- next-pwa (manifest, push 미사용)

**MVP 외 (Post-MVP):**
- 풀 AI 해석 (긴 텍스트, V3)
- PII 익명화 (V4)
- PWA 푸시 (V5+, ADR-23 재고)
- 신살·대운, 친구 비교, 다국어, 결제

### 근거

- 카테고리 운세·오늘의 운세 = 핵심 가치 명제(P2)의 데이터 위계 구현
- 공유는 viral loop의 전제 (Vercel + @vercel/og)
- 프론트가 백엔드에 묶인 OG·SEO 부담을 가져가 SLO 보호
- LLM 사전 생성(ADR-24)으로 사용자 요청 경로 SLO 영향 0

### 트레이드오프

- 일정 4주 → 약 7주
- 어드민 시드 작업 (50/300/1000 LLM 호출) 1회성 비용 — 모델 비용 약 $20\~40 1회

### 결과

- step3/5/6/7/8 모두 갱신
- step9-frontend.md 신규 (Next.js 모노레포 설계)
- 마이그레이션 V2 확장 (saju_share_link, key_message, daily_fortune_template)

---

## ADR-23: PWA 단일 코드 + 푸시 알림 미사용

- **날짜**: 2026-04-29
- **상태**: Accepted

### 컨텍스트

네이티브 앱 / RN / 웹 / PWA 중 결정 필요. 푸시 알림은 retention 훅 후보지만 OS 권한·iOS 제약·알림 피로도 등 비용 큼.

### 결정

- **PWA 단일 코드** (Next.js + `next-pwa` 또는 `@serwist/next`)
- manifest + service worker (정적 자산 오프라인 캐시)
- **푸시 알림 미사용**. 재방문 훅은 "오늘의 운세" 자체로 — 매일 다른 메시지가 자발적 재방문 유도

### 근거

- 단일 코드베이스로 출시 속도 확보
- iOS 16.4+ PWA 푸시 가능하나 권한 UX 거부율 높음
- 카카오톡 공유 → PWA 설치 유도 → 홈화면 아이콘 = 푸시 대체 retention

### 트레이드오프

- 푸시 없이 retention 검증 필요. 수치 확인 후 Post-MVP에서 재검토 (V5+)

### 결과

- frontend/`@serwist/next` 또는 `next-pwa` 통합
- manifest.json + 192/512 아이콘 + theme_color
- Post-MVP에서 재검토 시 ADR-23 갱신

---

## ADR-24: LLM precompute — 50 + 300 + 1000 사전 생성

- **날짜**: 2026-04-29
- **상태**: Accepted

### 컨텍스트

키 메시지·카테고리 운세·오늘의 운세를 LLM으로 생성하면 사용자 요청 경로에 1\~3초 지연. SLO(p95 ≤ 500ms) 위배.

### 결정

**사용자 요청 경로에서는 LLM 호출 없음. 모든 메시지를 사전 생성 후 DB 시드.**

| 항목 | 조합 | 키 | 테이블 |
|---|---|---|---|
| 키 메시지 (S4 first fold) | 50 = 일간 10 × 지배 오행 5 | (day_stem, dominant_element, OVERALL) | `key_message` |
| 카테고리 운세 6종 (S5-3) | 300 = 50 × 6 카테고리 | (day_stem, dominant_element, category) | `key_message` |
| 오늘의 운세 (S5-4) | ~1000 = 일간 10 × 일진 천간 10 × 일진 지지 12 (룰 압축) | (day_stem, daily_stem, daily_branch) | `daily_fortune_template` |

### 근거

- 사용자 입력 공간(일간 10 + 지배 오행 5)이 좁으므로 사전 생성 가능
- 한 번 생성하면 동일 키 → 동일 결과 (캐시 의미 최대)
- LLM 호출은 어드민 1회성 → SLO 영향 0
- 모델 비용 ≈ $20\~40 (HuggingFace Llama 3 70B 또는 GPT-4o-mini 1회 시드)

### 트레이드오프

- 메시지 다양성 한정 (동일 일간 사용자는 동일 메시지). UX 영향 작음 — 일간 자체가 사용자 식별 지표
- 모델 변경/품질 개선 시 재시드 필요 (어드민 토글로 zero-downtime 가능)

### 결과

- `KeyMessageSeeder`, `CategoryFortuneSeeder`, `DailyFortuneSeeder` 작성 (W4\~W5)
- 어드민 트리거 엔드포인트 또는 CLI (별도 plan)
- LLM 호출은 `vtIoExecutor`에서 동시성 5 정도로 제한

---

## ADR-25: 모노레포 + 프론트엔드 Next.js 14 (App Router)

- **날짜**: 2026-04-29
- **상태**: Accepted

### 컨텍스트

프론트엔드 도입 결정. 후보: (a) Vite + React, (b) Next.js (Pages), (c) Next.js (App Router). 현재 디렉토리에 모노레포로 구성 요구.

### 결정

- **모노레포**: 워크스페이스 루트 = git repo 루트. `backend/`(Spring Boot, 리네임됨) + `frontend/`(Next.js) + `.github/workflows/`
- **프론트**: **Next.js 14+ (App Router) + TypeScript + Tailwind + shadcn/ui**
- OG 이미지: `@vercel/og` (Edge Runtime)
- PWA: `@serwist/next` 또는 `next-pwa` (push 미사용, ADR-23)
- 배포: Vercel (프론트), ECS Fargate 유지 (백엔드)
- 도메인: `saju.app`(프론트) + `api.saju.app`(백엔드) — cross-subdomain 세션 쿠키

### 근거

- Next.js App Router: SEO·OG 동적 이미지·RSC가 viral loop의 전제
- Vite 기각: OG 이미지·SEO를 백엔드에 추가 부담 (Java 이미지 라이브러리 비용)
- Pages Router 기각: App Router가 신규 표준, 학습 곡선 한 번이면 충분
- 모노레포: 코드 이동 비용 절감, 백엔드/프론트 동시 변경 시 단일 PR

### 트레이드오프

- 첫 셋업 일정 +1주 (W4 부트스트랩, W5\~W7 화면 구현)
- Vercel 무료 티어 한도 모니터링 필요 (Edge function 100k/월)

### 결과

- Pre-flight: git init + `saju-backend/saju-backend/` → `backend/` 리네임 (완료)
- step9-frontend.md 신규 작성 (디렉토리·페이지·컴포넌트·CI/CD)
- `.github/workflows/backend.yml` paths: `["backend/**"]`, `frontend.yml` paths: `["frontend/**"]`

---

## ADR-26: 결과 페이지 컨텐츠 표준 — 6 카테고리 + 오늘의 운세

- **날짜**: 2026-04-29
- **상태**: Accepted

### 컨텍스트

결과 페이지의 정보 위계를 어떻게 잡을지 결정 필요. 사용자가 카테고리별·오늘의 운세 모두 요구.

### 결정

**결과 페이지 R2의 정보 위계 lock:**

1. 5-1. 5가지 에너지 (오행 도넛, Recharts)
2. 5-2. 성격 카드 6장 (십신, 강도 순 6장)
3. 5-3. **카테고리 운세 6종** (아코디언):
   - OVERALL (총운)
   - WEALTH (금전)
   - LOVE (연애)
   - HEALTH (건강)
   - CAREER (직업·학업)
   - FAMILY (가족·인간관계)
4. 5-4. **오늘의 운세** (한 줄 메시지 + 행운 컬러 + 행운 시간대 + 주의 키워드 + 날짜 라벨)
5. 5-5. 하단 고정 액션바 (공유 / 다른 분석 / 내 이력)

### 근거

- 6 카테고리는 보편적 사주 수요 매핑 (재물·연애·건강·직장·가족·총운)
- 오늘의 운세는 명리 "일진" 개념을 MZ 친화 카드로 변환
- 정보 위계: 가벼움 → 깊이 (오행 → 성격 → 카테고리 → 오늘) — 사용자 페이지 끝까지 도달 시 다음 행동(공유) 유도

### 트레이드오프

- 카테고리당 메시지 1줄 → 깊이는 Post-MVP 풀 AI 해석으로 보강
- 오늘의 운세는 본인 차트만 노출 (PII·사적 영역) — 공유 페이지(S6)에서는 노출 안 함

### 결과

- `FortuneCategory` enum: OVERALL, WEALTH, LOVE, HEALTH, CAREER, FAMILY
- `key_message.category` 컬럼 (V2)
- `daily_fortune_template` 테이블 (V2)
- step3 S5-3, S5-4 정의
- step5 GET /charts/{id}/categories, GET /charts/{id}/today 엔드포인트

---

## 리스크 레지스터 (최신, 2026-04-29)

| ID | 설명 | 대응 ADR | 현재 상태 |
|---|---|---|---|
| R1 | 사주 정확도 — 한국식 보정 정확도 | ADR-13 | 골든 테스트셋 50건 게이트 (W2\~W3) |
| R2 | VT pinning/spin | ADR-02 | 적용 |
| R3 | App Runner 단종 | ADR-03 | 적용 |
| R4 | LocalStack Community 한계 | ADR-16 | LocalStack(S3, Secrets Manager) Community 티어 사용 |
| R5 | 기존 자산 통합 | ADR-04, 13, 15 | 진행 중 |
| R6 | Guest 데이터 누적 | ADR-14 | 차단 |
| R7 | ddl-auto:update 위험 | ADR-09 | Flyway 도입 |
| R8 | 시크릿 평문 노출 | ADR-16 | W1 Pre-work에서 회전 |
| R9 | PII 처리 | ADR-17 | Post-MVP |
| R10 | MVP 일정 — 4주 → 약 7주 | ADR-22 | 스코프 갱신 + 잠금 |
| R11 | 모듈 제거 (`notification`/`hanja`) | ADR-19 (부분 변경) | `ai/`는 유지 (Seeder 활용) |
| R12 | SB 3.5.x 마이너 호환 이슈 | ADR-18 | W1에서 빌드 통과 확인 |
| R13 | 카카오 redirect URI 운영 도메인 | ADR-25 | `api.saju.app/login/oauth2/code/kakao` 표준화 |
| R14 | LLM 비용·품질 (50/300/1000 시드) | ADR-24 | 1회성 비용 ≈ $20\~40, 모델 변경 시 재시드 |
| R15 | 친구·가족 차트 PII 부담 | ADR-21 | 사용자 자발적 입력, 본인 계정 귀속 |
| R16 | PWA 푸시 없는 retention | ADR-23 | "오늘의 운세"가 자체 retention 훅, 수치 검증 후 V5+에서 재고 |
| R17 | 모노레포·프론트 추가 작업 | ADR-25 | Vercel 자체 CI 위임으로 백엔드 영향 최소 |
| R18 | Vercel 무료 티어 한도 (Edge function 100k/월, OG 100k/월) | ADR-25 | 모니터링 후 Pro 전환 결정 |
