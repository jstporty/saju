# ADR — Architecture Decision Records

각 결정의 **배경(왜) + 결정(무엇) + 트레이드오프**를 간결하게 기록.

---

## ADR-01: 언어 — Kotlin/Java 21

**배경**: 스타트업 속도와 트래픽 폭발 대비 안정성을 동시에 확보해야 함.  
**결정**: Kotlin + Java 21 (Virtual Thread 지원). Spring Boot 3.5.x.  
**트레이드오프**: Kotlin 러닝 커브 < JVM 생태계의 검증된 안정성.

---

## ADR-02: 사주 엔진 — lunar-java 오픈소스 채택

**배경**: 천문학 직접 계산 시 검증 비용이 개발 기간을 초과.  
**결정**: `cn.6tail:lunar:1.7.7` 연동. 커스텀 어댑터로 KST 보정·자시 처리 래핑.  
**트레이드오프**: 라이브러리 의존성 < 계산 오류 리스크 최소화. 교체 시 `SajuEnginePort`만 변경.

---

## ADR-03: 한국 시간 보정 — KST +30분 LMT

**배경**: 한국 표준시(KST, UTC+9)는 지리적 경도(동경 127.5°) 기반 LMT(UTC+8:30)와 30분 차이. 전통 명리학은 LMT 기준 계산.  
**결정**: 모든 사주 계산 전 입력 시간에서 30분 차감.  
**트레이드오프**: 일부 현대 명리학자는 KST 그대로 사용. MVP는 전통 방식 채택.

---

## ADR-04: 자시/야자시 — 사용자 선택 강제

**배경**: 00:00-01:00는 전날 자시(야자시)와 당일 자시 해석이 갈림. 임의 기본값 적용 시 오계산 리스크.  
**결정**: 해당 시간대 입력 시 야자시/자시 선택 모달 강제 표시. 선택 없이 제출 불가.  
**트레이드오프**: UX 마찰 추가 < 계산 신뢰도.

---

## ADR-05: 인증 — JWT 제거, JDBC Session 채택

**배경**: JWT Refresh Token 구현이 복잡도 대비 MVP에서 과도. 카카오 OAuth + 세션 쿠키가 단순하고 안전.  
**결정**: `spring-session-jdbc` + `SPRING_SESSION` 테이블, 14일 TTL, SameSite=None+Secure.  
**트레이드오프**: 수평 확장 시 DB 의존성 증가. ECS + RDS 환경에서 허용 가능한 수준.

---

## ADR-06: LLM 콘텐츠 — 오픈 전 사전 생성

**배경**: 실시간 LLM 호출 시 P95 응답이 500ms SLO를 초과. 사용자 경로에 LLM 없어야 함.  
**결정**: 오픈 전 Admin Seeder로 350행(key_message) + 600행(daily_fortune_template) 사전 생성 후 DB 적재. 사용자 요청은 DB 룩업만.  
**트레이드오프**: 콘텐츠 다양성 제한 (1:1 맞춤 불가) < 응답 속도 SLO 보장.

---

## ADR-07: Seeder 트리거 — Admin REST Endpoint

**배경**: 마이그레이션 시 자동 실행 시 LLM 비용·시간이 예측 불가. 수동 제어 필요.  
**결정**: `POST /admin/seed/key-messages`, `POST /admin/seed/daily-fortunes`. 인증 필수, idempotent(중복 row skip).  
**트레이드오프**: 수동 트리거 필요 < LLM 시드가 배포 파이프라인을 블로킹하는 위험 방지.

---

## ADR-08: 차트 조회 — 매 GET 엔진 재계산

**배경**: `raw_pillars` JSON을 그대로 반환하면 엔진 버전 업 시 구버전 데이터가 노출됨. 사주 계산은 CPU 연산으로 DB보다 빠름.  
**결정**: 매 GET 시 `LunarJavaSajuEngine.analyze()` 재계산. `raw_pillars`는 중복 방지(`calculation_key`)와 캐싱 참조용만 사용.  
**트레이드오프**: CPU 사용량 증가 < 항상 최신 엔진 결과 보장.

---

## ADR-09: 페이지네이션 — Offset (MVP), Cursor (Post-MVP)

**배경**: 사주 이력은 데이터가 적고(사용자당 수십 건), 빠른 구현이 우선.  
**결정**: `page`/`size` offset 방식. Cursor 전환은 Post-MVP.  
**트레이드오프**: 대용량에서 Deep Page 성능 저하 < MVP 구현 속도.

---

## ADR-10: 소프트 삭제 — @SQLRestriction 엔티티 레벨 필터

**배경**: 카카오 정책상 회원 탈퇴 후 30일 유예가 필요. 물리 삭제 불가.  
**결정**: `oauth_user`, `saju_chart`에 `@SQLRestriction("deleted_at IS NULL")` 적용. 모든 JPA 쿼리에 자동 필터.  
**트레이드오프**: `@SQLRestriction`이 네이티브 쿼리에는 적용 안 됨 → 네이티브 쿼리 사용 시 WHERE 조건 수동 추가 필요.

---

## ADR-11: 사용자 탈퇴 — 연쇄 소프트 삭제 (애플리케이션 레벨)

**배경**: DB FK CASCADE DELETE는 복구 불가. 애플리케이션 레벨에서 순서 보장.  
**결정**: `oauth_user.softDelete()` → `saju_chart.softDelete()` (전체) → `saju_share_link.revokeAllByUserId()` 순서.  
**트레이드오프**: 애플리케이션 코드 복잡성 < 데이터 복구 가능성.

---

## ADR-12: 캐시 — Caffeine L1 단일 계층

**배경**: MVP 스케일에서 Redis 운영 비용 과도.  
**결정**: `keyMessage` 영구 캐시, `todayFortune` 24h TTL. 인스턴스 재시작 시 warm-up 필요 (DB 룩업으로 자동).  
**트레이드오프**: 멀티 인스턴스 캐시 불일치 가능 < 운영 단순성. ECS Task 수가 적은 MVP에서 허용.

---

## ADR-13: 도메인 분리 — saju.app / api.saju.app

**배경**: 프론트(Vercel)와 백엔드(ECS)를 다른 인프라에 배포. CORS 설정 필요.  
**결정**: 프론트 `saju.app`, 백엔드 `api.saju.app`. 서버사이드 렌더링에서는 `INTERNAL_API_BASE` 환경변수로 직접 호출.  
**트레이드오프**: CORS + 쿠키 SameSite=None 설정 필요 < 프론트/백 독립 배포.

---

## ADR-14: 공유 — MVP 포함 결정

**배경**: 공유가 바이럴 루프의 핵심. 지연 시 초기 사용자 데이터 수집 기회 손실.  
**결정**: MVP에 공유 포함. 8자 slug, 1년 만료, PII 마스킹(이름 첫글자+○○).  
**트레이드오프**: 구현 복잡도 소폭 증가 < 바이럴 실험 기회.

---

## ADR-15: 로컬 개발 — Podman + LocalStack

**배경**: Docker Desktop 라이선스 이슈 우려. AWS 환경을 로컬에서 재현 필요.  
**결정**: Podman(컨테이너), LocalStack 3.8(S3, Secrets Manager), `start-infra.sh`로 자동 초기화.  
**트레이드오프**: LocalStack 제한(일부 AWS 기능 미지원) < 무료 + AWS 동등 환경.

---

## ADR-16: subject_kind — SELF/OTHER 구분

**배경**: 지인 사주 기능 포함. 오늘의 운세는 "나 자신"에게만 의미 있음.  
**결정**: `saju_chart.subject_kind = SELF | OTHER`. `GET /today`는 SELF 차트에서만 허용(OTHER는 403).  
**트레이드오프**: API 제한 추가 < 의미 없는 오늘의 운세 노출 방지.

---

## ADR-17: dominant_element 동점 처리 — 목>화>토>금>수 우선순위

**배경**: 오행 점수가 동점일 경우 일관된 결정이 필요. 목(木)이 생명·시작을 의미하므로 우선.  
**결정**: 동점 시 목>화>토>금>수 순서로 선택.  
**트레이드오프**: 명리학적 완전성 < 구현 단순성 + 결과 일관성.
