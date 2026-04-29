# Step 9: 프론트엔드 + 모노레포 설계 (신규, 2026-04-29)

> Next.js 14 App Router 채택(ADR-25). PWA 단일 코드(ADR-23). 백엔드와 별도 도메인·CI/CD.
> 본 문서는 **청사진**까지만 다룹니다. 실제 코드(`frontend/` 내용물 + `.github/workflows/*.yml`) 생성은 별도 구현 plan.

---

## 1. 모노레포 디렉토리 (Pre-flight 완료 후)

```
saju-backend/                           # 워크스페이스 루트(이름 유지) = git repo 루트
├── .git/
├── .gitignore                          # Java + Node 통합
├── README.md                           # 모노레포 안내
├── backend/                            # Spring Boot (리네임됨)
│   ├── src/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew, gradlew.bat
│   ├── gradle/
│   ├── infra/
│   └── docs/design/                    # 설계 문서 본 위치
├── frontend/                           # Next.js 14 (별도 plan에서 생성)
│   ├── app/                            # App Router
│   ├── components/
│   ├── lib/
│   ├── public/
│   ├── package.json
│   ├── next.config.mjs
│   ├── tailwind.config.ts
│   └── tsconfig.json
└── .github/
    └── workflows/
        ├── backend.yml                 # paths: ["backend/**"]
        └── frontend.yml                # paths: ["frontend/**"] (선택, Vercel 자체 CI 위임 가능)
```

> Next.js Workspace(npm/pnpm/yarn) 도구 사용 안 함 — 백엔드(Gradle)와 프론트(Node)는 별도 의존성 관리. 단일 git repo에서 `frontend/`, `backend/` 두 모듈을 독립 빌드.

---

## 2. Next.js App Router — 페이지 라우팅 (S1\~S8 매핑)

| URL | 화면 (step3) | 라우트 파일 | 인증 |
|---|---|---|---|
| `/` | S1 Landing | `app/page.tsx` | 익명 (서버에서 세션 확인 후 redirect) |
| `/auth/kakao/callback` | S2 OAuth callback | (백엔드가 처리, 프론트는 redirect만 받음) | — |
| `/onboarding` | S3 출생정보 입력 폼 (본인 모드) | `app/onboarding/page.tsx` | 세션 |
| `/chart/new` | S3 (다른 사람 모드) | `app/chart/new/page.tsx` | 세션 |
| `/chart/[id]` | S4 first fold + S5 R2 | `app/chart/[id]/page.tsx` | 세션, 소유자 |
| `/chart/[id]/opengraph-image` | OG 이미지 동적 생성 | `app/chart/[id]/opengraph-image.tsx` | (Vercel Edge) |
| `/share/[token]` | S6 공유 페이지 (비로그인) | `app/share/[token]/page.tsx` | 익명 |
| `/share/[token]/opengraph-image` | OG 이미지 동적 생성 | `app/share/[token]/opengraph-image.tsx` | (Vercel Edge) |
| `/me` | S7 이력 목록 | `app/me/page.tsx` | 세션 |
| (단건은 `/chart/[id]` 재사용) | S8 | — | — |

### 2.1 디렉토리 상세

```
frontend/app/
├── layout.tsx                          # 루트 레이아웃 + PWA 메타 + Tailwind 글로벌
├── globals.css                         # Tailwind base
├── page.tsx                            # S1 Landing
├── auth/
│   └── kakao/
│       └── callback/
│           └── page.tsx                # 백엔드 redirect 받음 → 결과 화면 또는 /onboarding
├── onboarding/
│   └── page.tsx                        # S3 본인 입력
├── chart/
│   ├── new/
│   │   └── page.tsx                    # S3 다른 사람 입력
│   └── [id]/
│       ├── page.tsx                    # S4 + S5 결과 화면
│       └── opengraph-image.tsx         # @vercel/og
├── share/
│   └── [token]/
│       ├── page.tsx                    # S6 비로그인 공개
│       └── opengraph-image.tsx
├── me/
│   └── page.tsx                        # S7 이력
├── (api)/                              # 프론트 자체 API (없음, 백엔드로 직통)
└── error.tsx, not-found.tsx, loading.tsx
```

---

## 3. 컴포넌트 구조

```
frontend/components/
├── ui/                                 # shadcn/ui CLI로 생성 (Button, Card, Dialog, Toggle, Accordion, Toast, ...)
│   └── button.tsx, card.tsx, dialog.tsx, ...
├── chart/                              # 차트 결과 표시
│   ├── FourPillars.tsx                 # 사주 4기둥 그리드
│   ├── ElementsDonut.tsx               # 5가지 에너지 도넛 (Recharts)
│   ├── TenGodsCards.tsx                # 성격 카드 6장 (2×3 그리드)
│   └── KeyMessage.tsx                  # 한 줄 키 메시지
├── fortune/                            # 운세 컨텐츠
│   ├── CategoryAccordion.tsx           # 6 카테고리 아코디언
│   └── TodayFortuneCard.tsx            # 오늘의 운세 카드
├── form/                               # 입력 폼
│   ├── OnboardingForm.tsx              # 본인 모드
│   ├── OtherChartForm.tsx              # 다른 사람 모드 (재사용)
│   ├── JasiModal.tsx                   # 자시 강제 선택 모달
│   └── BirthDateInput.tsx              # 양/음력 토글, 윤달 체크
├── action/                             # 액션 컴포넌트
│   ├── BottomActionBar.tsx             # S5-5 하단 고정 액션바
│   ├── ShareButton.tsx                 # 네이티브 Share API + 폴백 (URL 복사)
│   └── DeleteChartDialog.tsx           # 삭제 확인 다이얼로그
└── layout/                             # 레이아웃 부속
    ├── Header.tsx
    └── Footer.tsx
```

---

## 4. 상태 관리

### 4.1 서버 상태 — TanStack Query v5

```typescript
// frontend/lib/queries.ts
export const useChart = (id: string) =>
  useQuery({
    queryKey: ['chart', id],
    queryFn: () => api.get<ChartResponse>(`/charts/${id}`),
    staleTime: 1000 * 60 * 60,         // 1시간 (계산 결과는 사실상 영구)
  });

export const useCategoryFortunes = (chartId: string) =>
  useQuery({
    queryKey: ['categories', chartId],
    queryFn: () => api.get<CategoryFortunesResponse>(`/charts/${chartId}/categories`),
    staleTime: Infinity,                // 동일 키 → 동일 결과 (영구)
  });

export const useTodayFortune = (chartId: string) =>
  useQuery({
    queryKey: ['today', chartId],
    queryFn: () => api.get<TodayFortuneResponse>(`/charts/${chartId}/today`),
    staleTime: 1000 * 60 * 60,         // 1시간 (24h 백엔드 캐시)
  });

export const useChartList = () =>
  useInfiniteQuery({
    queryKey: ['charts'],
    queryFn: ({ pageParam }) =>
      api.get<CursorPageResponse<ChartSummaryDto>>(`/charts?cursor=${pageParam ?? ''}&size=20`),
    initialPageParam: '',
    getNextPageParam: (last) => last.nextCursor ?? undefined,
  });

export const useCreateChart = () =>
  useMutation({
    mutationFn: (req: ChartCreateRequest) => api.post<ChartResponse>('/charts', req),
    onSuccess: (chart) => {
      queryClient.setQueryData(['chart', chart.id], chart);
      queryClient.invalidateQueries({ queryKey: ['charts'] });
    },
  });

export const useCreateShare = () =>
  useMutation({
    mutationFn: (chartId: string) =>
      api.post<ShareCreatedResponse>('/shares', { chartId }),
  });
```

### 4.2 클라이언트 상태 — Zustand

```typescript
// frontend/lib/store.ts
type FormStore = {
  subjectKind: 'SELF' | 'OTHER';
  birthTimeUnknown: boolean;
  jasiModalOpen: boolean;
  setSubjectKind: (kind: 'SELF' | 'OTHER') => void;
  setBirthTimeUnknown: (v: boolean) => void;
  openJasiModal: () => void;
  closeJasiModal: () => void;
};

export const useFormStore = create<FormStore>((set) => ({
  subjectKind: 'SELF',
  birthTimeUnknown: false,
  jasiModalOpen: false,
  setSubjectKind: (kind) => set({ subjectKind: kind }),
  setBirthTimeUnknown: (v) => set({ birthTimeUnknown: v }),
  openJasiModal: () => set({ jasiModalOpen: true }),
  closeJasiModal: () => set({ jasiModalOpen: false }),
}));
```

---

## 4.2 서버 컴포넌트 세션 확인 (`lib/session.ts`)

서버 컴포넌트는 브라우저 쿠키에 직접 접근할 수 없으므로, Next.js `cookies()` API로 `SESSION` 쿠키를 읽어 백엔드 `GET /api/v1/auth/me`에 전달한다.

```typescript
// frontend/lib/session.ts
import { cookies } from 'next/headers';

export interface SessionUser {
  id: string;
  name: string;
  email: string;
  profileImage: string | null;
  lastChartId?: string | null;
}

const INTERNAL_BASE = process.env.INTERNAL_API_BASE!;  // 서버 전용, 브라우저 노출 없음

export async function getSession(): Promise<SessionUser | null> {
  const cookieStore = await cookies();
  const sessionCookie = cookieStore.get('SESSION');
  if (!sessionCookie?.value) return null;

  try {
    const res = await fetch(`${INTERNAL_BASE}/api/v1/auth/me`, {
      headers: {
        Cookie: `SESSION=${sessionCookie.value}`,
      },
      cache: 'no-store',    // 세션은 항상 fresh 확인
    });
    if (!res.ok) return null;
    const json = await res.json();
    return json.data as SessionUser;
  } catch {
    return null;
  }
}
```

> - `INTERNAL_API_BASE`: 서버 사이드 전용. ECS/Docker 내부 네트워크 URL 설정 시 외부 홉 없이 직접 통신.
> - 401 또는 네트워크 오류 시 `null` 반환 → 각 페이지에서 `redirect('/')` 처리.
> - `cache: 'no-store'` 필수 — 세션 상태는 캐시해서는 안 됨.

---

## 5. 백엔드 API 클라이언트

```typescript
// frontend/lib/api.ts
const API_BASE = process.env.NEXT_PUBLIC_API_BASE!;   // https://api.saju.app

class ApiClient {
  private async request<T>(path: string, init?: RequestInit): Promise<T> {
    const res = await fetch(`${API_BASE}${path}`, {
      ...init,
      credentials: 'include',                          // 세션 쿠키 동봉
      headers: {
        'Content-Type': 'application/json',
        ...init?.headers,
      },
    });

    if (res.status === 401) {
      window.location.href = '/';                      // Landing redirect
      throw new ApiError('SESSION_EXPIRED', 401);
    }

    const json = await res.json();
    if (!json.success) throw new ApiError(json.error.code, res.status, json.error.message);
    return json.data as T;
  }

  get<T>(path: string)        { return this.request<T>(path); }
  post<T>(path: string, body: unknown) { return this.request<T>(path, { method: 'POST', body: JSON.stringify(body) }); }
  delete<T>(path: string)     { return this.request<T>(path, { method: 'DELETE' }); }
}

export const api = new ApiClient();
```

---

## 6. PWA 설정 (ADR-23)

### 6.1 `public/manifest.json`

```json
{
  "name": "사주",
  "short_name": "사주",
  "description": "당신의 사주를 한눈에",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#000000",
  "icons": [
    { "src": "/icons/icon-192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "/icons/icon-512.png", "sizes": "512x512", "type": "image/png" },
    { "src": "/icons/icon-512-maskable.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable" }
  ]
}
```

### 6.2 Service Worker (`@serwist/next` 또는 `next-pwa`)

```typescript
// frontend/next.config.mjs
import withSerwist from '@serwist/next';

export default withSerwist({
  swSrc: 'app/sw.ts',
  swDest: 'public/sw.js',
  cacheOnNavigation: true,
})({
  reactStrictMode: true,
});
```

- 정적 자산 (CSS, JS, 이미지) 오프라인 캐시
- API 응답은 캐시하지 않음 (사주 데이터는 항상 최신)
- 푸시 미사용 (ADR-23)

### 6.3 OG 이미지 (`@vercel/og`)

```typescript
// frontend/app/share/[token]/opengraph-image.tsx
import { ImageResponse } from 'next/og';

export const runtime = 'edge';
export const alt = '사주';
export const size = { width: 1200, height: 630 };
export const contentType = 'image/png';

export default async function Image({ params }: { params: { token: string } }) {
  const data = await fetchPublicChart(params.token);    // 백엔드 호출
  return new ImageResponse(
    (
      <div style={{ /* tailwind-like inline styles */ }}>
        <div>{data.fourPillars.day.stem}</div>           {/* 일간 한자 메인 */}
        <div>{data.keyMessage}</div>                     {/* 키 메시지 한 줄 */}
        <div>사주.app</div>                              {/* 로고 */}
      </div>
    ),
    size,
  );
}
```

---

## 7. 도메인 / 인증 / CORS (백엔드와 합의된 설정)

| 항목 | 값 |
|---|---|
| 프론트 도메인 | `https://saju.app` (Vercel) |
| 백엔드 도메인 | `https://api.saju.app` (ECS Fargate) |
| 세션 쿠키 | `Domain=.saju.app; SameSite=None; Secure; HttpOnly` (ADR-25) |
| 카카오 OAuth Redirect URI | `https://api.saju.app/login/oauth2/code/kakao` |
| OAuth 성공 후 redirect | `https://saju.app/onboarding` 또는 `https://saju.app/chart/{id}` |
| CORS | 백엔드 `WebMvcConfigurer`에 `https://saju.app` allowOrigin + `allowCredentials=true` |

### 환경 변수

```
# frontend/.env.local (개발)
NEXT_PUBLIC_API_BASE=http://localhost:8080
INTERNAL_API_BASE=http://localhost:8080      # 서버 컴포넌트 세션 확인용 (백엔드 직접 호출)

# frontend/.env.production (Vercel 환경변수)
NEXT_PUBLIC_API_BASE=https://api.saju.app
INTERNAL_API_BASE=https://api.saju.app       # ECS 내부 URL 또는 ALB DNS 사용 가능 (예: http://backend-alb.internal:8080)
```

> `INTERNAL_API_BASE`: 서버 컴포넌트에서 `GET /api/v1/auth/me` 호출에 사용. `NEXT_PUBLIC_` 접두사 없이 서버 전용으로 유지 (브라우저에 노출 불필요). ECS/Docker 환경에서는 내부 네트워크 URL로 설정하면 외부 홉을 줄일 수 있음.

---

## 8. 배포

### 8.1 Vercel (프론트)

- GitHub repo 연결 → `frontend/` 디렉토리를 root directory로 지정
- 자동 배포: `main` 브랜치 push → 프로덕션, PR → preview 배포
- 환경 변수: `NEXT_PUBLIC_API_BASE`, `INTERNAL_API_BASE`, OAuth/분석 키 등 Vercel 콘솔에 등록
- 도메인: `saju.app` 등록 + DNS A/CNAME 설정

### 8.2 ECS Fargate (백엔드 — 변경 없음)

- 기존 GitHub Actions → ECR → ECS Express Mode
- `api.saju.app` 도메인 + ALB SSL 인증서 등록

---

## 9. CI/CD — GitHub Actions 모노레포 분기

### 9.1 `.github/workflows/backend.yml` (청사진)

```yaml
name: Backend CI/CD

on:
  push:
    branches: [main]
    paths: ["backend/**", ".github/workflows/backend.yml"]
  pull_request:
    paths: ["backend/**", ".github/workflows/backend.yml"]

defaults:
  run:
    working-directory: backend

jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew clean build test --no-daemon

  deploy:
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    needs: build-test
    runs-on: ubuntu-latest
    permissions:
      id-token: write    # OIDC for AWS ECR
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: ap-northeast-2
      - uses: aws-actions/amazon-ecr-login@v2
        id: ecr
      - name: Build & push image
        run: |
          docker build -t $ECR_REGISTRY/saju-backend:${{ github.sha }} .
          docker push $ECR_REGISTRY/saju-backend:${{ github.sha }}
        env:
          ECR_REGISTRY: ${{ steps.ecr.outputs.registry }}
      - name: Update ECS service
        run: |
          aws ecs update-service --cluster saju-prod --service saju-backend \
            --force-new-deployment
```

### 9.2 `.github/workflows/frontend.yml` (선택, 청사진)

> Vercel 자체 CI에 위임하면 이 파일은 생략 가능. 보조 lint/typecheck만 하려면 다음:

```yaml
name: Frontend CI

on:
  push:
    branches: [main]
    paths: ["frontend/**", ".github/workflows/frontend.yml"]
  pull_request:
    paths: ["frontend/**", ".github/workflows/frontend.yml"]

defaults:
  run:
    working-directory: frontend

jobs:
  lint-typecheck:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run lint
      - run: npm run typecheck
      - run: npm run test --if-present
```

> 실제 배포는 Vercel(GitHub 통합)이 자동 처리 — 이 워크플로는 PR 게이트 보조용.

### 9.3 paths 필터 효과

- backend/ 변경만 push → `backend.yml` 실행, `frontend.yml` 건너뜀
- frontend/ 변경만 push → `frontend.yml`(있으면) + Vercel 빌드, `backend.yml` 건너뜀
- 동시 변경 → 양쪽 모두 실행

---

## 10. 부트스트랩 절차 (별도 plan에서 실행)

> 본 plan은 청사진까지. 실제 부트스트랩은 다음 절차로 별도 plan에서 진행.

```bash
# 워크스페이스 루트에서
cd frontend
npx create-next-app@latest . --typescript --tailwind --app --no-src-dir --import-alias "@/*"

# shadcn/ui 초기화
npx shadcn@latest init
npx shadcn@latest add button card dialog input toggle accordion toast

# 라이브러리 설치
npm i @tanstack/react-query zustand recharts lucide-react @vercel/og
npm i react-hook-form zod @hookform/resolvers
npm i -D @serwist/next vitest @testing-library/react @testing-library/jest-dom

# Vercel 연결
npx vercel link
```

---

## 11. 주요 페이지 청사진 (참조용)

### 11.1 `app/page.tsx` (S1 Landing)

```typescript
import { redirect } from 'next/navigation';
import { getSession } from '@/lib/session';        // 서버 컴포넌트에서 쿠키 검사

export default async function Landing() {
  const session = await getSession();
  if (session?.lastChartId) redirect(`/chart/${session.lastChartId}`);
  if (session?.user) redirect('/onboarding');
  return <LandingHero />;
}
```

### 11.2 `app/chart/[id]/page.tsx` (S4 + S5)

```typescript
export default async function ChartPage({ params }: { params: { id: string } }) {
  const chart = await fetchChart(params.id);            // 서버에서 SSR
  return (
    <>
      <FirstFold chart={chart} />                       {/* S4 */}
      <ElementsDonut score={chart.elementsScore} />     {/* S5-1 */}
      <TenGodsCards counts={chart.tenGodsCount} />      {/* S5-2 */}
      <CategoryAccordion chartId={chart.id} />          {/* S5-3 (TanStack Query 클라이언트) */}
      <TodayFortuneCard chartId={chart.id} />           {/* S5-4 */}
      <BottomActionBar chartId={chart.id} />            {/* S5-5 */}
    </>
  );
}
```

### 11.3 `app/share/[token]/page.tsx` (S6)

```typescript
export const revalidate = 300;   // ISR 5분 (백엔드 Cache-Control: max-age=300)

export default async function SharePage({ params }: { params: { token: string } }) {
  const data = await fetchPublicChart(params.token);    // 익명 호출
  return (
    <>
      <PublicFirstFold data={data} />                   {/* PII 마스킹된 결과 */}
      <ElementsDonut score={data.elementsScore} />
      <CategoryAccordion data={data.categories} />      {/* 본인용과 다른 props */}
      <CtaNoticeForGuest />                             {/* "나도 사주 보기" CTA */}
    </>
  );
}
```

---

## 12. 보안·성능 메모

- **CSRF**: SameSite=None + Secure 조합. MVP는 GET 비변경성과 율 제한·Origin 검증으로 보강. Post-MVP에서 `XSRF-TOKEN` 패턴 도입.
- **XSS**: React 자동 이스케이프 + `dangerouslySetInnerHTML` 사용 금지. 키 메시지·LLM 결과는 텍스트로만 렌더.
- **이미지 최적화**: `next/image`로 `public/` 정적 자산 자동 최적화. OG 이미지는 Edge에서 SVG 또는 Tailwind-like JSX로 렌더.
- **Lighthouse**: Performance ≥ 90, Accessibility ≥ 95, SEO ≥ 100 (출시 게이트).
- **번들 크기**: 초기 JS payload < 200KB (gzip). Tree-shaking된 shadcn + Recharts.

---

## 13. Post-MVP 메모

- 풀 AI 해석 페이지 (`/chart/[id]/interpretation`) — 비동기 폴링 UX
- 다국어 (next-intl 또는 i18next-app-router)
- 친구 비교 페이지 (`/compare?a=...&b=...`)
- PostHog 분석 통합 (퍼널·heatmap)
- PWA 푸시 (ADR-23 재고 시 — `notification_subscription` 테이블 V5)
