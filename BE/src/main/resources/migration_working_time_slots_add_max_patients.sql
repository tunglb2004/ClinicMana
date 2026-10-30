SET @slots_max_patients_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'working_time_slots'
      AND COLUMN_NAME = 'max_patients'
);

SET @slots_max_patients_ddl := IF(
    @slots_max_patients_exists = 0,
    'ALTER TABLE working_time_slots ADD COLUMN max_patients INT NOT NULL DEFAULT 8 AFTER end_time',
    'SELECT 1'
);

PREPARE slots_max_patients_stmt FROM @slots_max_patients_ddl;
EXECUTE slots_max_patients_stmt;
DEALLOCATE PREPARE slots_max_patients_stmt;

UPDATE working_time_slots
SET max_patients = 8
WHERE max_patients IS NULL OR max_patients < 1;