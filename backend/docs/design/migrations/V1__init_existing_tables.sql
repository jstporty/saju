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
