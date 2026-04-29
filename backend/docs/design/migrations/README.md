# Flyway 마이그레이션 초안 (2026-04-29 갱신)

본 디렉터리는 Step 6 데이터 설계의 산출물 중 **DDL 마이그레이션 스크립트 초안**을 보관합니다.
실제 Flyway 적용 시 이 파일들을 `backend/src/main/resources/db/migration/`로 이동하면 됩니다.

## 파일 (MVP)

| 파일 | 역할 | 상태 | 도입 시점 |
|---|---|---|---|
| `V1__init_existing_tables.sql` | 기존 운영 테이블 베이스라인 (oauth_user, saju_results, solar_terms 등) | 초안 | W1 Pre-work |
| `V2__mvp_schema_alignment.sql` | MVP 정렬: saju_chart(+subject_kind) / SPRING_SESSION / saju_share_link / key_message / daily_fortune_template 추가, refresh_token/notification/hanja DROP | 초안 | W1\~W2 |

## V2에 포함된 테이블 (ADR-22)

| 테이블 | 역할 | 관련 ADR |
|---|---|---|
| `saju_chart` | 사용자 소유 사주 이력 (`subject_kind` SELF/OTHER 포함) | ADR-08, ADR-21 |
| `SPRING_SESSION` / `SPRING_SESSION_ATTRIBUTES` | session-jdbc 표준 세션 저장소 | ADR-15 |
| `saju_share_link` | 공유 링크 토큰 (8자 URL slug + 1년 만료 + revoke) | ADR-22 |
| `key_message` | 키 메시지 + 카테고리 운세 6종 사전 생성 (50 + 300조합) | ADR-24, ADR-26 |
| `daily_fortune_template` | 오늘의 운세 사전 생성 (~1000조합) | ADR-24, ADR-26 |

## V2 적용 후 시드 (별도 어드민 작업)

`key_message`, `daily_fortune_template`은 V2 직후에는 빈 테이블. 어드민 시드 실행 필요:

| Seeder | 호출 | 결과 |
|---|---|---|
| `KeyMessageSeeder.seedAll()` | 50조합 | `key_message` (category=OVERALL 50개) |
| `CategoryFortuneSeeder.seedAll()` | 300조합 | `key_message` 6 카테고리 × 50 = 300개 |
| `DailyFortuneSeeder.seedAll()` | ~1000조합 | `daily_fortune_template` |

> 시드는 1회성. LLM 모델/품질 변경 시 재시드. 비용 ≈ $20\~40 (HuggingFace Llama 3 70B 또는 GPT-4o-mini, ADR-24).

## Post-MVP (참고용, 본 스프린트 외)

| 파일 (예정) | 역할 |
|---|---|
| `V3__add_ai_interpretation.sql` | `saju_chart`에 풀 AI 해석 컬럼 (`ai_status`, `ai_*_at`, `ai_interpretation`) — 긴 텍스트 비동기 |
| `V4__add_pii_anonymization.sql` | `oauth_user.email_hash/name_hash`, `saju_chart.anonymized_at`/`anonymous_user_id` (ADR-17) |
| `V5__add_notification_subscription.sql` | PWA 푸시 옵트인 (ADR-23 재고 시) |

## 적용 절차 (Flyway 도입 시 — W1 Pre-work)

1. `backend/build.gradle`에 의존성 추가
   ```gradle
   implementation 'org.flywaydb:flyway-core'
   implementation 'org.flywaydb:flyway-mysql'
   implementation 'org.springframework.session:spring-session-jdbc'
   ```
2. `backend/src/main/resources/application.yml` 수정
   ```yaml
   spring:
     flyway:
       enabled: true
       locations: classpath:db/migration
       baseline-on-migrate: true
       baseline-version: 1
     jpa:
       hibernate:
         ddl-auto: validate
     session:
       store-type: jdbc
       jdbc:
         initialize-schema: never  # Flyway가 직접 관리
       timeout: 14d
   ```
3. SQL 파일 이동: `backend/docs/design/migrations/V*.sql` → `backend/src/main/resources/db/migration/V*.sql`
4. 스테이징에서 `./gradlew bootRun` → Flyway가 V1을 baseline으로 인식 후 V2 적용
5. Testcontainers 통합 테스트(빈 MySQL 컨테이너에서 V1→V2 순차 실행) 통과 확인
6. **시드 실행** (어드민): `KeyMessageSeeder`, `CategoryFortuneSeeder`, `DailyFortuneSeeder` 1회

## V2 적용 전 점검사항 (운영)

- `refresh_token`, `notification`, `hanja` 테이블 데이터 **백업** 필수
- `saju_chart.calculation_key` UNIQUE 키가 `(user_id, calculation_key)` 복합으로 적용됨 (ADR-08 메모)
- 기존 `saju_chart`가 있는 환경에서는 `subject_kind` 컬럼이 추가되어야 함 — 이 V2 SQL은 `CREATE TABLE IF NOT EXISTS`이므로 신규 환경에는 안전, 기존 환경에서는 별도 ALTER 필요 (운영 데이터 점검 후 결정)
- 운영 환경에서 V2 적용 시 점검 SQL:
  ```sql
  SELECT COUNT(*) FROM refresh_token;     -- 백업 후 0이어야 안전
  SELECT COUNT(*) FROM notification;      -- 동일
  SELECT COUNT(*) FROM hanja;             -- 동일 (또는 별도 archive 테이블로)
  SELECT COUNT(*) FROM SPRING_SESSION;    -- 0이면 정상 (V2가 만든 빈 테이블)
  SELECT COUNT(*) FROM saju_share_link;
  SELECT COUNT(*) FROM key_message;       -- 0 (Seeder 실행 전)
  SELECT COUNT(*) FROM daily_fortune_template; -- 0 (Seeder 실행 전)
  ```

## 주의사항

- V1은 `CREATE TABLE IF NOT EXISTS` → 기존 환경에서도 안전
- V2는 멱등 ALTER + DROP IF EXISTS → 재실행 안전
- FK 추가 시 `saju_chart.user_id` ↔ `oauth_user.id` 정합성 사전 점검 필요
- DROP 전 백업: 운영 데이터는 별도 archive DB로 export 권장
- `key_message`와 `daily_fortune_template`는 사용자 데이터 아님 — FK 없음. 어드민 Seeder만 채움.
