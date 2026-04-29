-- =============================================================================
-- V2__mvp_schema_alignment.sql
-- MVP 스코프(ADR-22)에 맞춘 스키마 정렬.
--   - saju_chart 신규 (ADR-08, subject_kind 포함 - ADR-21)
--   - SPRING_SESSION / SPRING_SESSION_ATTRIBUTES 추가 (ADR-15)
--   - saju_share_link 신규 (ADR-22, 공유 MVP 포함)
--   - key_message 신규 (ADR-24, 키 메시지 + 카테고리 운세 6종 사전 생성)
--   - daily_fortune_template 신규 (ADR-24, 오늘의 운세 사전 생성)
--   - oauth_user.deleted_at 추가
--   - refresh_token, notification, hanja 테이블 DROP (ADR-15, ADR-19)
-- 멱등성: 컬럼/제약/테이블 존재 여부를 information_schema로 점검 후 조건부 실행.
-- =============================================================================

-- 1) saju_chart 신규 테이블 (ADR-08, ADR-21)
CREATE TABLE IF NOT EXISTS saju_chart (
    id                 VARCHAR(36)  NOT NULL,
    user_id            VARCHAR(36)  NOT NULL,
    subject_name       VARCHAR(100) NOT NULL COMMENT '분석 대상 이름',
    subject_name_hanja VARCHAR(100),
    subject_kind       VARCHAR(10)  NOT NULL DEFAULT 'SELF' COMMENT 'SELF | OTHER (ADR-21)',
    birth_date         DATE         NOT NULL,
    birth_time         TIME,
    calendar_type      VARCHAR(10)  NOT NULL DEFAULT 'SOLAR',
    is_leap_month      TINYINT(1)   NOT NULL DEFAULT 0,
    gender             VARCHAR(10)  NOT NULL,
    birth_longitude    DOUBLE       NOT NULL DEFAULT 127.5,
    dominant_element   VARCHAR(10)  NOT NULL COMMENT 'WOOD/FIRE/EARTH/METAL/WATER — key_message 룩업 키, 동점 시 목>화>토>금>수',
    calculation_key    VARCHAR(64)  NOT NULL COMMENT 'SHA-256(birth_date||birth_time||gender||calendar_type||is_leap_month||subject_kind||subject_name)[:64]',
    raw_pillars        JSON         NOT NULL COMMENT 'FourPillars + elementsScore + tenGodsCount 직렬화',
    warnings           JSON,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    deleted_at         DATETIME     NULL COMMENT '소프트 삭제',
    PRIMARY KEY (id),
    UNIQUE KEY uq_saju_chart_user_calc (user_id, calculation_key),
    KEY idx_saju_chart_user_created (user_id, created_at DESC, id),
    CONSTRAINT fk_saju_chart_user FOREIGN KEY (user_id) REFERENCES oauth_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) Spring Session JDBC 표준 스키마 (ADR-15)
CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID            CHAR(36)     NOT NULL,
    SESSION_ID            CHAR(36)     NOT NULL,
    CREATION_TIME         BIGINT       NOT NULL,
    LAST_ACCESS_TIME      BIGINT       NOT NULL,
    MAX_INACTIVE_INTERVAL INT          NOT NULL,
    EXPIRY_TIME           BIGINT       NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    PRIMARY KEY (PRIMARY_ID),
    UNIQUE KEY uq_spring_session_id (SESSION_ID),
    KEY idx_spring_session_expiry (EXPIRY_TIME),
    KEY idx_spring_session_principal (PRINCIPAL_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BLOB         NOT NULL,
    PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT fk_spring_session_attr FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) oauth_user.deleted_at 멱등 추가
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'oauth_user'
      AND COLUMN_NAME  = 'deleted_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE oauth_user ADD COLUMN deleted_at DATETIME NULL COMMENT ''소프트 삭제'' AFTER updated_at',
    'SELECT ''oauth_user.deleted_at already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) DROP refresh_token (ADR-15: session-jdbc로 대체)
DROP TABLE IF EXISTS refresh_token;

-- 5) DROP notification (ADR-19: 모듈 제거)
DROP TABLE IF EXISTS notification;

-- 6) DROP hanja (ADR-19: 모듈 제거)
-- 주의: 운영 환경에 hanja 데이터가 있을 경우 사전 백업 권장.
DROP TABLE IF EXISTS hanja;

-- 7) saju_results는 KEEP (deprecated). 신규 INSERT/SELECT는 애플리케이션에서 차단.
--    Post-MVP V3 시점에 archive 후 삭제 검토.

-- 8) saju_share_link 신규 테이블 (ADR-22, 공유 MVP 포함)
CREATE TABLE IF NOT EXISTS saju_share_link (
    id          VARCHAR(36)  NOT NULL,
    chart_id    VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    token       VARCHAR(16)  NOT NULL COMMENT 'URL slug, 8자 권장',
    expires_at  DATETIME     NOT NULL COMMENT '1년 만료',
    revoked_at  DATETIME     NULL COMMENT '차트 삭제 시 set',
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_share_link_token (token),
    KEY idx_share_link_chart (chart_id),
    KEY idx_share_link_user_created (user_id, created_at DESC),
    CONSTRAINT fk_share_link_chart FOREIGN KEY (chart_id) REFERENCES saju_chart (id),
    CONSTRAINT fk_share_link_user  FOREIGN KEY (user_id)  REFERENCES oauth_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9) key_message 신규 테이블 (ADR-24, 50조합 키메시지 + 300조합 카테고리 운세)
--    카테고리: OVERALL, WEALTH, LOVE, HEALTH, CAREER, FAMILY (ADR-26)
CREATE TABLE IF NOT EXISTS key_message (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    day_stem           CHAR(2)      NOT NULL COMMENT '일간 천간(한자) 갑..계',
    dominant_element   VARCHAR(10)  NOT NULL COMMENT 'WOOD/FIRE/EARTH/METAL/WATER',
    category           VARCHAR(20)  NOT NULL COMMENT 'OVERALL/WEALTH/LOVE/HEALTH/CAREER/FAMILY',
    message            TEXT         NOT NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_key_message_lookup (day_stem, dominant_element, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10) daily_fortune_template 신규 테이블 (ADR-24, ~1000조합 오늘의 운세)
--     룩업 키: 사용자 일간 + 일진(천간/지지)
CREATE TABLE IF NOT EXISTS daily_fortune_template (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    day_stem      CHAR(2)      NOT NULL COMMENT '사용자 일간 천간',
    daily_stem    CHAR(2)      NOT NULL COMMENT '일진 천간',
    daily_branch  CHAR(2)      NOT NULL COMMENT '일진 지지',
    message       TEXT         NOT NULL,
    lucky_color   VARCHAR(50)  NOT NULL,
    lucky_hour    VARCHAR(50)  NOT NULL,
    caution       VARCHAR(100) NOT NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_daily_fortune_lookup (day_stem, daily_stem, daily_branch)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
