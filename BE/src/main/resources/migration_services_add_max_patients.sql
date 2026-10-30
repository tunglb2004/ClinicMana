SET @services_max_patients_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'services'
      AND COLUMN_NAME = 'max_patients'
);

SET @services_max_patients_ddl := IF(
    @services_max_patients_exists = 0,
    'ALTER TABLE services ADD COLUMN max_patients INT NOT NULL DEFAULT 8 AFTER service_category',
    'SELECT 1'
);

PREPARE services_max_patients_stmt FROM @services_max_patients_ddl;
EXECUTE services_max_patients_stmt;
DEALLOCATE PREPARE services_max_patients_stmt;

UPDATE services
SET max_patients = 8
WHERE max_patients IS NULL OR max_patients < 1;