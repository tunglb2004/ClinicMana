SET @services_is_deleted_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'services'
      AND COLUMN_NAME = 'is_deleted'
);

SET @services_is_deleted_ddl := IF(
    @services_is_deleted_exists = 0,
    'ALTER TABLE services ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER is_active',
    'SELECT 1'
);

PREPARE services_is_deleted_stmt FROM @services_is_deleted_ddl;
EXECUTE services_is_deleted_stmt;
DEALLOCATE PREPARE services_is_deleted_stmt;

UPDATE services
SET is_deleted = 0
WHERE is_deleted IS NULL;
