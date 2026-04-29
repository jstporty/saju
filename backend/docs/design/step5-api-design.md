# Step 5: API 설계 (2026-04-29 갱신)

> MVP 스코프(ADR-22). 세션 쿠키 기반(ADR-15). Guest 차단(ADR-14). 친구·가족 차트 허용(ADR-21).
> 카테고리 운세·오늘의 운세·공유 기능 MVP 포함(ADR-22, ADR-26).
> Post-MVP 엔드포인트는 별도 섹션에 표시.

---

## 공통 규약

- Base URL: `/api/v1`
- 인증: 세션 쿠키 `SESSION`
  - **MVP**: `Domain=.saju.app; SameSite=None; Secure; HttpOnly; Path=/` (cross-subdomain, ADR-25)
  - **로컬 개발**: `Domain=localhost; SameSite=Lax; Path=/`
- 응답 포맷(과도기): 기존 `ApiResponse<T>` 호환 유지
  - 정상: `{ "success": true, "data": T, "error": null }`
  - 에러: `{ "success": false, "data": null, "error": { "code": "...", "message": "..." } }`
- 페이지네이션: 커서 기반 (`?cursor=&size=20`, max 50)
- Content-Type: `application/json`
- 시각: ISO-8601 UTC. 날짜 `LocalDate`(yyyy-MM-dd), 시간 `LocalTime`(HH:mm)
- ID: 신규 도메인은 UUID v7 (`uuid-creator`)

---

## MVP 엔드포인트 (약 7주 출시 범위)

### Auth (`/api/v1/auth`)

#### GET `/oauth2/authorization/kakao`

카카오 인가 엔드포인트로 리다이렉트.

- 인증: 익명
- 응답: 302 → kauth.kakao.com

#### GET `/login/oauth2/code/kakao`

Spring Security가 처리하는 콜백.

- 성공 시: `oauth_user` upsert + `HttpSession` 생성 → `Set-Cookie: SESSION=...`
- 후속 redirect: 신규/차트없음 → `https://saju.app/onboarding`, 기존+차트있음 → `https://saju.app/chart/{lastChartId}`
- 실패 시: 401 / 프론트에서 Landing fallback

#### POST `/auth/logout`

- 인증: 세션
- 처리: `HttpSession.invalidate()` + `SPRING_SESSION` DELETE
- 응답: 204 + `Set-Cookie: SESSION=; Max-Age=0; Domain=.saju.app`

#### GET `/auth/me`

- 인증: 세션
- 응답: `ApiResponse<UserMeResponse>`
  ```json
  { "id": "uuid", "email": "string", "name": "string", "profileImage": "string", "provider": "kakao" }
  ```

---

### Charts (`/api/v1/charts`)

> 단일 자원 경로로 통일 (기존 `/saju/charts`와 `/history`는 동일 자원이므로 `/charts` 단일화).

#### POST `/charts`

사주 계산 + 차트 저장. 본인·타인 차트 모두 허용 (`subjectName`으로 구분, ADR-21).

- 인증: 세션 필수 (Guest 차단)
- Idempotency: 헤더 `Idempotency-Key` 옵션 — 동일 요청 5분 내 재시도 보호
- Request:
  ```json
  {
    "subjectName": "홍길동",
    "subjectKind": "SELF",
    "birthDate": "1990-05-15",
    "birthTime": "14:30",
    "birthTimeUnknown": false,
    "calendarType": "SOLAR",
    "isLeapMonth": false,
    "gender": "MALE",
    "jasiPolicy": "NEXT_DAY",
    "birthLongitude": 127.5
  }
  ```
  - `subjectKind`: `SELF` | `OTHER` (본인/다른 사람 모드)
  - `birthTimeUnknown`: true면 `birthTime`은 무시되며 시주 미산정
  - `jasiPolicy`: 23:00\~23:59 입력 시만 의미. `NEXT_DAY` (다음 날 자시) | `YAJASI` (당일 야자시). 모호 시 422
  - `birthLongitude`: 디폴트 127.5(KST). 사용자에게 노출하지 않음

- Response: `ApiResponse<ChartResponse>` (201)
  ```json
  {
    "id": "uuid-v7",
    "subjectName": "홍길동",
    "subjectKind": "SELF",
    "birth": { "...": "echo of request" },
    "fourPillars": {
      "year":  { "stem": "庚", "stemKo": "경", "branch": "午", "branchKo": "오", "stemElement": "METAL", "branchElement": "FIRE" },
      "month": { "...": "..." },
      "day":   { "...": "..." },
      "hour":  { "...": "..." }
    },
    "elementsScore": {
      "WOOD": 35,
      "FIRE": 20,
      "EARTH": 10,
      "METAL": 25,
      "WATER": 10
    },
    "tenGodsCount": [
      { "tenGod": "비견", "count": 2, "keyword": "자립" },
      { "tenGod": "식신", "count": 1, "keyword": "표현" },
      { "tenGod": "정재", "count": 1, "keyword": "안정" },
      { "tenGod": "정관", "count": 1, "keyword": "책임" },
      { "tenGod": "정인", "count": 2, "keyword": "지혜" },
      { "tenGod": "일주", "count": 1, "keyword": "핵심" }
    ],
    "keyMessage": "곧고 의지가 강한 큰 나무 같은 사람입니다",
    "warnings": [],
    "createdAt": "2026-04-29T08:50:00Z"
  }
  ```
  - `elementsScore`: 5가지 에너지 비율 (S5-1)
  - `tenGodsCount`: 성격 카드 6장 (S5-2). 선정 기준: 사주 8자(천간4+지지4)에서 일간 대비 십신 등장 횟수 카운트 → count 내림차순 상위 5개 + 일주 카드(항상 포함) = 6장. 동점 시 우선순위: 비견>겁재>식신>상관>편재>정재>편관>정관>편인>정인. count>0이 5개 미만이면 count=0 십신으로 채움
  - `keyMessage`: 일간 + 지배 오행 → `key_message` 캐시 룩업 (50조합 사전 생성, ADR-24). **시드 완료 전에는 `null` 반환 (에러 없음) — 프론트는 null이면 placeholder 표시. 출시 게이트: 시드 완료 확인 필수**

- 검증 규칙
  - `birthDate`: 1899\~현재 (lunar-java 지원 범위). 외부 시 422 `BIRTH_DATE_OUT_OF_RANGE`
  - `birthTimeUnknown=true`: `birthTime`/`jasiPolicy` 무시, 응답 `fourPillars.hour=null`, `warnings=["BIRTH_TIME_UNKNOWN"]`
  - `calendarType=LUNAR`인 경우만 `isLeapMonth` 의미. 양력은 강제 false
  - `birthTime` ∈ [23:00, 23:59) AND `jasiPolicy` 미설정: 422 `HOUR_AMBIGUOUS`
  - 만 13세 미만: 422 `MINOR_BLOCKED`

#### GET `/charts/{id}`

- 인증: 세션 + 소유자 (`chart.user_id == sessionUserId`)
- 응답: `ApiResponse<ChartResponse>` (POST와 동일 형태)
- 404: 미존재 / 삭제됨 / 타인 소유 (정보 노출 회피)

#### GET `/charts`

본인 차트 목록 (S7 이력, "내가 본 사주"). 본인 차트는 항상 최상단 정렬.

- 인증: 세션 필수
- Query: `cursor` (createdAt+id 인코딩), `size` (default 20, max 50)
- 응답: `ApiResponse<CursorPageResponse<ChartSummaryDto>>`
  ```json
  {
    "items": [
      {
        "id": "uuid",
        "subjectName": "홍길동",
        "subjectKind": "SELF",
        "birthDate": "1990-05-15",
        "keyMessagePreview": "곧고 의지가 강한 큰 나무 …",
        "createdAt": "..."
      }
    ],
    "nextCursor": "opaque-string",
    "hasMore": true
  }
  ```

#### DELETE `/charts/{id}`

- 인증: 세션 + 소유자
- 처리: 소프트 삭제 (`deleted_at = NOW()`) + 관련 `saju_share_link` 모두 `revoked_at = NOW()`
- 응답: 204

---

### Fortune (카테고리 운세 / 오늘의 운세, ADR-26)

#### GET `/charts/{id}/categories`

카테고리 운세 6종 (S5-3).

- 인증: 세션 + 소유자
- 응답: `ApiResponse<CategoryFortunesResponse>`
  ```json
  {
    "categories": [
      { "category": "OVERALL", "label": "총운", "icon": "✨", "message": "당신의 일생을 관통하는 흐름은 …" },
      { "category": "WEALTH", "label": "금전운", "icon": "💰", "message": "재물의 흐름은 …" },
      { "category": "LOVE", "label": "연애운", "icon": "💕", "message": "사랑과 인연의 결은 …" },
      { "category": "HEALTH", "label": "건강운", "icon": "🩺", "message": "몸과 마음의 균형은 …" },
      { "category": "CAREER", "label": "직업·학업운", "icon": "🎯", "message": "일과 성취의 방향은 …" },
      { "category": "FAMILY", "label": "가족·인간관계운", "icon": "👨‍👩‍👧", "message": "가족·동료와의 관계는 …" }
    ]
  }
  ```
- 데이터 출처: `key_message` 테이블 (`day_stem`, `dominant_element`, `category` 키) — 사전 생성 300조합 (50×6)
- 캐시: Caffeine L1 (TTL=24h, 동일 키는 영구 동일 결과이므로 사실상 영구)
- SLO: p95 ≤ 100ms (캐시 히트율 95%+)

#### GET `/charts/{id}/today`

오늘의 운세 (S5-4). **`subject_kind = SELF`인 본인 차트만 허용** (친구·가족 차트의 오늘의 운세는 사적 영역 침해 우려로 제외).

- 인증: 세션 + 소유자 + `subject_kind=SELF`
- `subject_kind=OTHER`인 차트에 호출 시: 422 `TODAY_NOT_ALLOWED_FOR_OTHER`
- 응답: `ApiResponse<TodayFortuneResponse>`
  ```json
  {
    "date": "2026-04-29",
    "dayLabel": "2026년 4월 29일 (수)",
    "message": "오늘은 새로운 시도가 잘 풀리는 날입니다.",
    "luckyColor": "파란색",
    "luckyHour": "오후 2시\\~4시",
    "caution": "서두르지 말기"
  }
  ```
- 데이터: 사용자 일간 + `DailyStemBranchProvider.of(LocalDate.now(KST))` → `daily_fortune_template` 룩업 (1000조합)
- 캐시: 키 = `(chartId, date)`, TTL 24h
- SLO: p95 ≤ 200ms

---

### Share (공유 링크, ADR-22)

#### POST `/shares`

본인의 차트로부터 공유 토큰 생성.

- 인증: 세션 + 소유자
- 율 제한: 분당 10건/사용자
- Request:
  ```json
  { "chartId": "uuid" }
  ```
- 응답: `ApiResponse<ShareCreatedResponse>` (201)
  ```json
  {
    "token": "abc12xyz",
    "shareUrl": "https://saju.app/s/abc12xyz",
    "expiresAt": "2027-04-29T08:50:00Z"
  }
  ```
- 토큰: 8자 슬러그 (URL-safe), 1년 만료, 차트 삭제 시 `revoked_at` 셋 (사용 불가)

#### GET `/shares/{token}`

비로그인 공개 조회 (S6).

- 인증: **익명 허용**
- 응답: `ApiResponse<PublicChartDto>`
  ```json
  {
    "subjectNameMasked": "홍○○",
    "fourPillars": { "...": "..." },
    "elementsScore": { "...": "..." },
    "keyMessage": "...",
    "categories": [ "...": "6종" ]
  }
  ```
- 노출 안 함: 출생연월일·시간 (PII), `subjectKind`, `tenGodsCount` (성격 카드는 본인 로그인 화면 전용 — 공유 페이지 제외), 오늘의 운세 (본인 영역)
- 만료/폐기 시: 404 `SHARE_NOT_FOUND`
- SLO: p95 ≤ 800ms (Next.js RSC + 백엔드 1회 호출)

---

## 인증/인가 매트릭스 (MVP)

| 엔드포인트 | 메서드 | 인증 | 인가 |
|---|---|---|---|
| `/oauth2/authorization/kakao` | GET | 익명 | — |
| `/login/oauth2/code/kakao` | GET | 익명 | Spring Security 처리 |
| `/auth/logout` | POST | 세션 | self |
| `/auth/me` | GET | 세션 | self |
| `/charts` | POST | 세션 | self |
| `/charts` | GET | 세션 | self |
| `/charts/{id}` | GET / DELETE | 세션 | 소유자 |
| `/charts/{id}/categories` | GET | 세션 | 소유자 |
| `/charts/{id}/today` | GET | 세션 | 소유자 |
| `/shares` | POST | 세션 | 소유자 |
| `/shares/{token}` | GET | **익명 허용** | — |
| `/actuator/health` | GET | 익명 | ECS health check |

## 인증 미들웨어 동작 (세션 기반)

1. Spring Security `SessionManagementFilter`가 SESSION 쿠키 검증 → `SecurityContext` 세팅
2. `SecurityConfig`: 익명 경로 `permitAll()`, 그 외 `authenticated()`
3. 소유자 확인은 Service 레이어에서 `chart.userId.equals(currentUserId)`. 불일치 시 `404` (정보 노출 회피)
4. CSRF: 세션 + 쿠키 기반이므로 활성화. SPA에서는 `XSRF-TOKEN` 쿠키 + `X-XSRF-TOKEN` 헤더 패턴
   - 단, MVP에서는 SameSite=None + Secure 조합과 GET 비변경성으로 우선 비활성 + 율 제한·Origin 검증으로 보강

## CORS 정책

- 운영
  - allowedOrigins: `https://saju.app`
  - allowCredentials: `true`
  - allowedMethods: `GET, POST, DELETE, OPTIONS`
  - allowedHeaders: `Content-Type, X-XSRF-TOKEN, Idempotency-Key`
  - maxAge: 3600
- 카카오 OAuth Redirect URI: `https://api.saju.app/login/oauth2/code/kakao` (카카오 디벨로퍼 콘솔 등록)
- 로컬: `http://localhost:3000` 추가 허용, `withCredentials=true`
- 비-CORS 경로: `/oauth2/authorization/kakao` (302 redirect 자체)는 OPTIONS preflight 불필요

---

## Post-MVP 엔드포인트 (참고용 — 미구현)

| 메서드 | 경로 | 설명 | 도입 시점 |
|---|---|---|---|
| POST | `/api/v1/charts/{id}/interpretation` | 풀 AI 해석 비동기 트리거 | V3+ (긴 텍스트 + 카테고리당 5\~10문단) |
| GET | `/api/v1/charts/{id}/interpretation` | AI 해석 폴링 (status: PENDING/PROCESSING/DONE/FAILED) | V3+ |
| GET | `/api/v1/charts/{id}/special-stars` | 신살·대운 (전문가 모드) | V3+ |
| POST | `/api/v1/charts/compare` | 친구 비교 모드 (두 차트 궁합) | V3+ |
| DELETE | `/api/v1/users/me` | 회원 탈퇴 + 익명화 | V4+ (ADR-17) |
| GET | `/api/v1/notifications/subscribe` | PWA 푸시 옵트인 | V3+ (ADR-23 재고) |

---

## Springdoc OpenAPI 적용 (W4 도입)

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true   # 운영에서는 false 또는 인증 보호
```

```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
```
