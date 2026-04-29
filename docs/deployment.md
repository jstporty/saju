# 배포 가이드

## 1. 환경 변수 / Secrets 명세

### 백엔드 (ECS Fargate, `application-prod.yml`)

| 변수 | 출처 | 예시 / 설명 |
|---|---|---|
| `DB_URL` | Secrets Manager `saju/prod/db:url` | `jdbc:mysql://...rds.../saju_db?useSSL=true&serverTimezone=Asia/Seoul` |
| `DB_USERNAME` | Secrets Manager `saju/prod/db:username` | `saju` |
| `DB_PASSWORD` | Secrets Manager `saju/prod/db:password` | (RDS 마스터 비밀번호) |
| `KAKAO_CLIENT_ID` | Secrets Manager `saju/prod/kakao:client_id` | 카카오 디벨로퍼 콘솔 발급 |
| `KAKAO_CLIENT_SECRET` | Secrets Manager `saju/prod/kakao:client_secret` | 동일 |
| `KAKAO_REDIRECT_URI` | Secrets Manager `saju/prod/kakao:redirect_uri` | `https://api.saju.app/login/oauth2/code/kakao` |
| `HF_TOKEN` | Secrets Manager `saju/prod/hf:token` | HuggingFace Inference API 토큰 |
| `AWS_REGION` | Task Definition env | `ap-northeast-2` |
| `SPRING_PROFILES_ACTIVE` | Task Definition env | `prod` |

### 프론트엔드 (Vercel)

| 변수 | 값 |
|---|---|
| `NEXT_PUBLIC_API_BASE` | `https://api.saju.app` |
| `NEXT_PUBLIC_APP_URL` | `https://saju.app` |

### GitHub Actions Secrets (백엔드 배포용)

| 키 | 설명 |
|---|---|
| `AWS_DEPLOY_ROLE_ARN` | OIDC로 ECS/ECR에 push/deploy 권한이 있는 IAM Role ARN |

---

## 2. AWS 사전 작업

1. **ECR 리포지토리 생성**: `saju-backend`
2. **RDS MySQL 8.x 생성**: VPC 내, ECS와 동일 VPC, sg에서 ECS sg → RDS 3306 허용
3. **Secrets Manager 등록**:
   - `saju/prod/db` (json: `{username, password, url}`)
   - `saju/prod/kakao` (json: `{client_id, client_secret, redirect_uri}`)
   - `saju/prod/hf` (json: `{token}`)
4. **ECS Cluster 생성**: `saju-prod` (Fargate)
5. **ALB 생성**: `api.saju.app`로 매핑, target group은 ECS 서비스
6. **Route 53**:
   - `saju.app` → Vercel (CNAME 또는 Vercel 가이드 따름)
   - `api.saju.app` → ALB
7. **IAM Role**:
   - Task Execution Role: ECR pull + Secrets Manager read
   - Task Role: 추가 AWS 호출이 있다면 권한 부여
   - GitHub Actions OIDC Role: ECR push, ECS update-service, iam:PassRole

---

## 3. 카카오 디벨로퍼 콘솔 등록

1. https://developers.kakao.com 접속 → 앱 생성
2. **카카오 로그인** 활성화
3. **Redirect URI** 등록:
   - 운영: `https://api.saju.app/login/oauth2/code/kakao`
   - 로컬: `http://localhost:8080/login/oauth2/code/kakao`
4. **동의 항목**:
   - 닉네임 (필수)
   - 이메일 (필수, 비즈 앱 신청 필요할 수 있음)
5. **앱 키 → REST API 키** = `KAKAO_CLIENT_ID`
6. **보안 → Client Secret** 발급 → `KAKAO_CLIENT_SECRET`

---

## 4. Vercel 연결

1. Vercel 대시보드 → New Project → GitHub repo 선택
2. **Root Directory**: `frontend`
3. **Framework Preset**: Next.js (자동 감지)
4. **Environment Variables**:
   - `NEXT_PUBLIC_API_BASE=https://api.saju.app`
   - `NEXT_PUBLIC_APP_URL=https://saju.app`
5. 도메인: `saju.app` 연결
6. PR → Preview deployment 자동 생성

---

## 5. 첫 배포 체크리스트

- [ ] RDS 생성 + Secrets Manager 등록 완료
- [ ] Flyway가 `V1`, `V2`, `V3` 마이그레이션 자동 적용 확인 (ECS 첫 기동 시 로그 확인)
- [ ] ECS Task 정상 기동: `/actuator/health/liveness` 200
- [ ] ALB → ECS 헬스체크 PASS
- [ ] `https://api.saju.app/swagger-ui/index.html` 접근 가능
- [ ] Vercel `https://saju.app` 접근 가능
- [ ] 카카오 로그인 → 세션 쿠키 `Domain=.saju.app; SameSite=None; Secure` 발급 확인
- [ ] **Seeder 1회 실행** (관리자 카카오 로그인 후):
  ```
  curl -X POST https://api.saju.app/admin/seed/key-messages \
       -H "Cookie: SESSION=..."
  curl -X POST https://api.saju.app/admin/seed/daily-fortunes \
       -H "Cookie: SESSION=..."
  ```
  HF API 약 950회 호출, 30~60분 소요. idempotent (재실행 안전).
- [ ] 시드 검증 SQL: `SELECT COUNT(*) FROM key_message;`(=350), `SELECT COUNT(*) FROM daily_fortune_template;`(=600)

---

## 6. 운영 시 주의

- **세션 쿠키 SameSite=None**: cross-subdomain (`saju.app` ↔ `api.saju.app`) 동작에 필수. HTTPS 필수.
- **CORS**: `application-prod.yml`의 `cors.allowed-origins=https://saju.app`로 고정.
- **Admin 엔드포인트**: 운영에서는 ALB Path 룰 또는 IAM 인증으로 `/admin/**` 추가 차단 권장.
- **카카오 30일 유예**: 회원 탈퇴 시 데이터 즉시 삭제 금지. 30일 후 별도 batch로 물리 삭제 (post-MVP).
