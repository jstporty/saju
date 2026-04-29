# Step 1: 사주 엔진 & 인프라 결정 (Locked, 2026-04-29 갱신)

> 상태: **확정(Locked)** — 이후 단계에서 번복 시 Step 8 ADR-01/13 이력 참조.
> 이전 결정(자체 엔진 유지)는 ADR-01에 보존, 본 결정은 ADR-13으로 갱신됨.

## 1-1. 사주 엔진 = 옵션 A (lunar-java + 한국식 보정 어댑터)

### 결정

**`6tail/lunar-java`(Maven, Apache 2.0) 채택. 한국식 보정은 자체 어댑터로 래핑.**
기존 `engine/` 패키지(GanjiCalculator, SolarTermService, ElementAnalyzer, TenGodsService, SpecialStarService, TimeService)는 **제거**되고 lunar-java 기반 신규 구현으로 대체.

### 근거 (vs 기존 자체 구현)

- 외부 검증된 라이브러리로 음력 변환·간지·24절기 시각의 **유지보수 부담 이전**
- lunar-java는 한국 사주 명리에 직접 대응하지 않으나, 음력·간지·절기 핵심 로직은 동일 → 보정 레이어만 한국화
- 자체 구현 유지 시 골든 테스트셋 검증 부담이 동일하게 발생함을 확인 → 외부 라이브러리 + 어댑터 조합이 장기적으로 더 안전

### 한국식 보정 어댑터 (자체 구현 항목)

| 항목 | 처리 방식 |
|---|---|
| KST +30분 LMT 보정 | 동경 127.5° 기준. `KoreanLmtAdjuster` 클래스로 입력 시각 보정 후 lunar-java에 전달 |
| 자시/야자시 분기 | 23:00\~00:59 구간을 입력 시점 기준으로 일주 결정. lunar-java 결과를 `HourPillarAdjuster`로 후처리 |
| 절입 시각 매핑 | lunar-java의 `JieQi` 데이터를 그대로 활용 (KASI 데이터와 1분 이내 차이 확인 필요) |
| 1908년 이전 LMT | **MVP 스코프 외**. 운영 도메인 1900\~현재 안에서 1908 이전은 1\~2% 수준이므로 출시 게이트 아님 |
| 시간 미상(birthTime null) | 시주 계산 생략, 응답에 `warnings: ["BIRTH_TIME_UNKNOWN"]` 추가 |

### 출시 게이트 (R1 해소 조건)

- KASI 데이터 기준 골든 테스트셋 **50건** 통과 (자시/야자시/절입 시각/일반 케이스 균형)
- lunar-java 결과 vs 한국 만세력 사이트(예: 정확한 만세력) 결과 1분 이내 일치
- 시간 미상 케이스 회귀 테스트

### 어댑터 컴포넌트 / Engine 마이그레이션

| 컴포넌트 | 처리 | MVP 노출 |
|---|---|---|
| `engine.ganji.GanjiCalculator` | 제거 → lunar-java `Lunar.getDayInGanZhi()` 등으로 대체 | (내부) |
| `engine.ganji.GanjiConstants` | 제거 (lunar-java 상수로 흡수) | (내부) |
| `engine.solarterm.SolarTermService` | 제거 → lunar-java `JieQi` 활용 | (내부) |
| `engine.solarterm.entity.SolarTermEntity` + `solar_terms` 테이블 | **유지** (lunar-java 결과 캐시 / 검증용) | (내부) |
| `engine.element.ElementAnalyzer` | **어댑터 내 재구현 (MVP 필수)** — 결과 화면 5가지 에너지 도넛 데이터 (`elementsScore`) 산출 | **MVP 포함** (ADR-22) |
| `engine.tengods.TenGodsService` | **어댑터 내 재구현 (MVP 필수)** — 성격 카드 6장 데이터 (`tenGodsCount`) 산출 | **MVP 포함** (ADR-22) |
| `engine.star.SpecialStarService` | 어댑터 내 재구현 (Post-MVP) | Post-MVP |
| `engine.time.TimeService` | 제거 → `KoreanLmtAdjuster`로 흡수 | (내부) |
| **`adapter.engine.DailyStemBranchProvider`** (신규) | **lunar-java로 임의 날짜의 천간·지지(일진) 계산. "오늘의 운세"용 (`daily_fortune_template` 룩업 키)** | **MVP 포함** (ADR-22, ADR-26) |

> **MVP 노출 lock (ADR-22)**: 오행 분석·십신 분석은 결과 화면 R2에 직접 노출. 신살·대운은 Post-MVP.
> **DailyStemBranchProvider 사용처**: `FortuneService.getTodayFortune(chartId)` → 사용자 일간 + `DailyStemBranchProvider.of(LocalDate.now(KST))` 조합으로 `daily_fortune_template` 테이블 룩업. 24h 캐시.

---

## 1-2. 인프라 = Java 21 유지 + ECS Fargate

### 결정

| 항목 | 결정값 |
|---|---|
| 런타임 | Java 21 (LTS), Eclipse Temurin |
| 프레임워크 | **Spring Boot 3.5.x** (3.2.2에서 업그레이드, ADR-18) |
| 배포 | AWS ECS Fargate (Express Mode) |
| 로컬 | Docker Compose + LocalStack (S3, **Secrets Manager**, Community 티어) |
| Virtual Threads | JPA/트랜잭션 경로 **비활성**, LLM·HTTP·S3 외부 IO 전용 Executor에만 적용 (※ AI는 Post-MVP) |

### Virtual Threads 제한 근거

HikariCP `ConcurrentBag` yield-spin 이슈. SB 3.5.x는 HikariCP 6.x 번들이지만 동일 패턴 보고됨 (JEP 491은 Java 24+). MVP는 보수적으로 JPA 경로 비적용 유지.

### App Runner 미채택 근거

AWS App Runner 신규 가입 2026-04-30 종료 → ECS Express Mode로 전환 (ADR-03).

---

## 리스크 레지스터 현황 (Step 8과 동기)

| ID | 설명 | 대응 | 현재 상태 |
|---|---|---|---|
| R1 | 사주 정확도 — lunar-java + 한국식 보정 정확도 | 어댑터 + 골든 테스트셋 50건 | 미구축 → Engine 스프린트 게이트 |
| R2 | VT pinning/spin | JPA 경로 비적용 | ADR-02 유지 |
| R3 | App Runner 단종 | ECS Express Mode | ADR-03 확정 |
| R4 | LocalStack Community 한계 | Compose + LocalStack(S3, Secrets Manager) | ADR-16 |
| R5 | 기존 DB·OAuth 자산 | 재마운트 + 부분 교체 | 진행 중 (Engine, JWT 제거) |
| R6 | Guest 데이터 누적 | 차단 | ADR-14로 해결 |
| R7 | ddl-auto:update 운영 위험 | Flyway 도입 | ADR-09 유지 |
| R8 | 시크릿 평문 노출 | 즉시 회전 + Secrets Manager | ADR-16으로 해결 |
| R9 | PII 보존/삭제 | 익명화 정책 | ADR-17 (Post-MVP) |
| R10 | MVP 일정 4주 → 약 7주 (프론트 + 카테고리·오늘의 운세 추가) | 스코프 lock + 카테고리 LLM precompute로 SLO 보장 | ADR-22로 갱신 |
| R11 | LLM 호출 응답시간 vs SLO | 50/300/1000 사전 생성 + DB 시드 | ADR-24로 해결 |
| R12 | 모노레포·프론트 추가 작업 | Vercel 배포 + Next.js, 백엔드는 ECS 유지 | ADR-25로 잠금 |
