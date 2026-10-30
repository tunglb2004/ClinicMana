ALTER TABLE appointments
ADD COLUMN room_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;

ALTER TABLE appointments
ADD CONSTRAINT fk_appointments_room
FOREIGN KEY (room_id) REFERENCES rooms(room_id);

UPDATE appointments a
JOIN doctors d ON d.doctor_id = a.doctor_id
SET a.room_id = d.room_id
WHERE a.room_id IS NULL
  AND d.room_id IS NOT NULL;