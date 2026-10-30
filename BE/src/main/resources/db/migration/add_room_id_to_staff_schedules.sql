-- 1. Thêm cột room_id vào staff_schedules (nullable để tương thích bản ghi cũ)
ALTER TABLE staff_schedules
ADD COLUMN room_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;

ALTER TABLE staff_schedules
ADD CONSTRAINT fk_staff_schedule_room FOREIGN KEY (room_id) REFERENCES rooms(room_id);

-- 2. Migrate dữ liệu cũ: parse note "Phòng: <tên phòng>" → tìm room_id theo room_name
UPDATE staff_schedules ss
JOIN rooms r ON TRIM(r.room_name) COLLATE utf8mb4_unicode_ci = TRIM(SUBSTRING(ss.note COLLATE utf8mb4_unicode_ci, LOCATE(':', ss.note COLLATE utf8mb4_unicode_ci) + 2))
SET ss.room_id = r.room_id
WHERE ss.note LIKE 'Phòng:%'
  AND ss.room_id IS NULL;
