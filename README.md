# 사주 (Saju) — Monorepo

> MZ 페르소나용 사주 PWA. YC 배치 출시 타깃 (T+약 7주, ADR-22).

## 모노레포 구조

```
saju-backend/                       # 워크스페이스 루트 = git repo 루트 (이름 유지)
├── backend/                        # Spring Boot 3.5.x (Java 21)
│   ├── src/
│   ├── build.gradle
│   └── docs/design/                # 설계 문서 (단일 진입: docs/design/README.md)
├── frontend/                       # Next.js 14 (App Router) — 별도 plan에서 부트스트랩
│   └── (pending)
├── .github/workflows/
│   ├── backend.yml                 # paths: ["backend/**"]
│   └── frontend.yml                # paths: ["frontend/**"] (Vercel 자체 CI 위임 가능)
├── .gitignore                      # Java + Node 통합
└── README.md                       # 본 파일
```

## 진입 문서

전체 설계 컨텍스트(MVP 스코프, 기술 스택, 데이터 모델, API, ADR 26건, 일정):
**[backend/docs/design/README.md](backend/docs/design/README.md)**

## MVP 7대 기능 (ADR-22)

1. 카카오 로그인 (cross-subdomain 세션, `saju.app`/`api.saju.app`)
2. 출생정보 입력 (본인/타인 토글, 자시 강제 선택, 양/음력 윤달)
3. 사주 계산 + 5가지 에너지 도넛 + 성격 카드 6장
4. 카테고리 운세 6종 (총운/금전/연애/건강/직업·학업/가족·인간관계)
5. 오늘의 운세 (한 줄 + 행운 컬러/시간 + 주의)
6. 공유 링크 (비로그인 공개 + 동적 OG 이미지)
7. 사주 이력 (본인·친구·가족 차트 관리)

## 기술 스택 한 줄 요약

- **Backend**: Java 21 + Spring Boot 3.5.x + JPA/QueryDSL + MySQL 8 + lunar-java 1.7.x + spring-session-jdbc + Flyway 10
- **Frontend**: Next.js 14 (App Router) + TypeScript + Tailwind + shadcn/ui + TanStack Query + Recharts + `@vercel/og` + `@serwist/next` (PWA)
- **Infra**: AWS ECS Fargate (Express Mode) + Vercel + AWS Secrets Manager + LocalStack(로컬)

## 개발 시작

### Backend

```bash
cd backend
./gradlew bootRun
# → http://localhost:8080
```

### Frontend (별도 plan에서 부트스트랩 후)

```bash
cd frontend
npm install
npm run dev
# → http://localhost:3000
```

### Local infra (Docker)

```bash
cd backend
./infra/start-infra.sh   # MySQL 8 + LocalStack(S3, Secrets Manager)
```

## CI/CD

- **백엔드**: GitHub Actions (`.github/workflows/backend.yml`) → ECR → ECS Fargate
- **프론트**: Vercel(GitHub 통합) — `main` 푸시 → 프로덕션, PR → preview

paths 필터로 모노레포 분기 — backend/ 변경만 push 시 backend.yml만 실행.

## 도메인 (운영)

| 영역 | 도메인 | 비고 |
|---|---|---|
| 프론트 | `https://saju.app` | Vercel |
| 백엔드 | `https://api.saju.app` | ECS Fargate |
| 세션 쿠키 | `Domain=.saju.app; SameSite=None; Secure; HttpOnly` | cross-subdomain |
| OAuth Redirect | `https://api.saju.app/login/oauth2/code/kakao` | 카카오 디벨로퍼 콘솔 등록 |

## 다음 단계 (별도 plan)

1. **Backend 구현**: lunar-java 어댑터 + ChartController + FortuneController + ShareController + Seeder
2. **LLM 시드 1회 실행**: 50조합 키 메시지 + 300조합 카테고리 운세 + 1000조합 오늘의 운세
3. **Frontend 부트스트랩**: `cd frontend && npx create-next-app@latest . ...`
4. **GitHub Actions YAML 파일 생성** (`.github/workflows/backend.yml`, `frontend.yml`)
5. **도메인·DNS 등록**, 카카오 Redirect URI 등록
6. **출시**: ALB + Route 53 + Vercel 도메인 연결

## 라이선스 / 약관

- 카카오 로그인 약관 동의 (앱 내 모달)
- 개인정보처리방침 (앱 내 모달)
- 미성년자(만 13세 미만) 차단 (ADR-22)

---

> 모든 기술 결정은 `backend/docs/design/step8-adr.md`에 ADR로 기록됨. 26건 (Locked + Superseded + Post-MVP).
