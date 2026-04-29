-- saju_share_link.updated_at 추가 (BaseTimeEntity의 @LastModifiedDate 매핑용)
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'saju_share_link'
      AND COLUMN_NAME  = 'updated_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE saju_share_link ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at',
    'SELECT ''saju_share_link.updated_at already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
