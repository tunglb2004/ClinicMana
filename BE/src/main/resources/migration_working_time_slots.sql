-- Migration script: Recreate working_time_slots table
-- Run this script in MySQL to fix the schema mismatch

-- Step 1: Drop old table (if exists)
DROP TABLE IF EXISTS working_time_slots;

-- Step 2: Create new table with correct schema
CREATE TABLE working_time_slots (
    slot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shift_name VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_patients INT NOT NULL DEFAULT 8,
    description TEXT,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_is_deleted (is_deleted),
    INDEX idx_is_active (is_active),
    INDEX idx_shift_name (shift_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 3: Insert sample data
INSERT INTO working_time_slots 
    (shift_name, start_time, end_time, max_patients, description, is_active, is_deleted, created_at, updated_at) 
VALUES
('Ca SÃ¡ng', '07:00:00', '12:00:00', 8, 'Ca lÃ m viá»‡c buá»•i sÃ¡ng tá»« 7h Ä‘áº¿n 12h', 1, 0, '2026-04-25 00:00:00', '2026-04-25 00:00:00'),
('Ca Chiá»u', '13:00:00', '17:00:00', 8, 'Ca lÃ m viá»‡c buá»•i chiá»u tá»« 13h Ä‘áº¿n 17h', 1, 0, '2026-04-25 00:00:00', '2026-04-25 00:00:00');

-- Verify
SELECT * FROM working_time_slots;
