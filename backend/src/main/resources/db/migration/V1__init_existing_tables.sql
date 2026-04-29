-- =============================================================================
-- V1__init_existing_tables.sql
-- Flyway baseline: 기존 ddl-auto:update로 생성된 테이블을 V1로 캡처.
-- 신규 환경(스테이징/프로덕션 reset)에서 처음부터 스키마를 만들기 위한 정본.
-- =============================================================================

CREATE TABLE IF NOT EXISTS oauth_user (
    id            VARCHAR(36)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    profile_image VARCHAR(500),
    provider      VARCHAR(20)  NOT NULL,
    provider_id   VARCHAR(100) NOT NULL,
    roles         VARCHAR(200) NOT NULL DEFAULT 'USER',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_oauth_user_email (email),
    KEY idx_oauth_user_provider (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_token (
    id          VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    device_info VARCHAR(200),
    ip_address  VARCHAR(45),
    expires_at  DATETIME     NOT NULL,
    revoked     TINYINT(1)   NOT NULL DEFAULT 0,
    revoked_at  DATETIME,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_refresh_token_user_id (user_id),
    KEY idx_refresh_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS saju_results (
    id                VARCHAR(36)  NOT NULL,
    user_id           VARCHAR(36)  NOT NULL,
    calculation_key   VARCHAR(100) NOT NULL,
    raw_pillars       JSON         NOT NULL,
    elements_score    JSON         NOT NULL,
    ten_gods          JSON,
    special_stars     JSON,
    ai_interpretation TEXT,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_saju_results_calc_key (calculation_key),
    KEY idx_saju_results_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS solar_terms (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    year          INT          NOT NULL,
    term_name     VARCHAR(20)  NOT NULL,
    term_datetime DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_solar_terms_year_name (year, term_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed: 2024~2026 주요 절기 (lunar-java 보정 검증용)
INSERT IGNORE INTO solar_terms (year, term_name, term_datetime) VALUES
(2024, '입춘', '2024-02-04 17:27:00'), (2024, '경칩', '2024-03-05 11:23:00'),
(2024, '청명', '2024-04-04 16:02:00'), (2024, '입하', '2024-05-05 09:10:00'),
(2024, '망종', '2024-06-05 13:10:00'), (2024, '소서', '2024-07-06 23:20:00'),
(2024, '입추', '2024-08-07 15:09:00'), (2024, '백로', '2024-09-07 12:11:00'),
(2024, '한로', '2024-10-08 09:00:00'), (2024, '입동', '2024-11-07 01:20:00'),
(2024, '대설', '2024-12-06 18:17:00'), (2024, '소한', '2025-01-05 11:14:00'),
(2025, '입춘', '2025-02-03 23:10:00'), (2025, '경칩', '2025-03-05 17:07:00'),
(2025, '청명', '2025-04-04 21:48:00'), (2025, '입하', '2025-05-05 14:56:00'),
(2025, '망종', '2025-06-05 18:57:00'), (2025, '소서', '2025-07-07 05:06:00'),
(2025, '입추', '2025-08-07 20:54:00'), (2025, '백로', '2025-09-07 17:52:00'),
(2025, '한로', '2025-10-08 14:41:00'), (2025, '입동', '2025-11-07 07:04:00'),
(2025, '대설', '2025-12-07 00:04:00'), (2025, '소한', '2026-01-05 17:02:00'),
(2026, '입춘', '2026-02-04 04:58:00'), (2026, '경칩', '2026-03-05 22:55:00'),
(2026, '청명', '2026-04-05 03:39:00'), (2026, '입하', '2026-05-05 20:49:00'),
(2026, '망종', '2026-06-06 00:52:00'), (2026, '소서', '2026-07-07 11:03:00'),
(2026, '입추', '2026-08-08 02:53:00'), (2026, '백로', '2026-09-07 23:53:00'),
(2026, '한로', '2026-10-08 20:44:00'), (2026, '입동', '2026-11-07 13:10:00'),
(2026, '대설', '2026-12-07 06:12:00');

CREATE TABLE IF NOT EXISTS notification (
    id             VARCHAR(36)  NOT NULL,
    user_id        VARCHAR(36)  NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    template_code  VARCHAR(50)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    content        TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    reference_type VARCHAR(50),
    reference_id   VARCHAR(36),
    sent_at        DATETIME,
    read_at        DATETIME,
    error_message  TEXT,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notification_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hanja (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    character_value VARCHAR(8)   NOT NULL,
    meaning         VARCHAR(200),
    sound_korean    VARCHAR(20),
    radical         VARCHAR(20),
    stroke_count    INT,
    PRIMARY KEY (id),
    KEY idx_hanja_character (character_value),
    KEY idx_hanja_sound (sound_korean)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
