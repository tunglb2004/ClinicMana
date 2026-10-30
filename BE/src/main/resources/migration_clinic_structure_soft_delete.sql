SET @departments_is_active_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'departments'
      AND COLUMN_NAME = 'is_active'
);

SET @departments_is_active_ddl := IF(
    @departments_is_active_exists = 0,
    'ALTER TABLE departments ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER description',
    'SELECT 1'
);

PREPARE departments_is_active_stmt FROM @departments_is_active_ddl;
EXECUTE departments_is_active_stmt;
DEALLOCATE PREPARE departments_is_active_stmt;

SET @departments_is_deleted_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'departments'
      AND COLUMN_NAME = 'is_deleted'
);

SET @departments_is_deleted_ddl := IF(
    @departments_is_deleted_exists = 0,
    'ALTER TABLE departments ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER is_active',
    'SELECT 1'
);

PREPARE departments_is_deleted_stmt FROM @departments_is_deleted_ddl;
EXECUTE departments_is_deleted_stmt;
DEALLOCATE PREPARE departments_is_deleted_stmt;

UPDATE departments
SET is_active = 1
WHERE is_active IS NULL;

UPDATE departments
SET is_deleted = 0
WHERE is_deleted IS NULL;

SET @rooms_is_deleted_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'rooms'
      AND COLUMN_NAME = 'is_deleted'
);

SET @rooms_is_deleted_ddl := IF(
    @rooms_is_deleted_exists = 0,
    'ALTER TABLE rooms ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER status',
    'SELECT 1'
);

PREPARE rooms_is_deleted_stmt FROM @rooms_is_deleted_ddl;
EXECUTE rooms_is_deleted_stmt;
DEALLOCATE PREPARE rooms_is_deleted_stmt;

UPDATE rooms
SET is_deleted = 0
WHERE is_deleted IS NULL;
