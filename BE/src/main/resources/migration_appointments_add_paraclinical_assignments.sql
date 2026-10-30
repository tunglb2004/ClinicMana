ALTER TABLE appointments
ADD COLUMN paraclinical_doctor_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
ADD COLUMN paraclinical_room_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;

ALTER TABLE appointments
ADD CONSTRAINT fk_appointments_paraclinical_doctor
FOREIGN KEY (paraclinical_doctor_id) REFERENCES doctors(doctor_id);

ALTER TABLE appointments
ADD CONSTRAINT fk_appointments_paraclinical_room
FOREIGN KEY (paraclinical_room_id) REFERENCES rooms(room_id);

UPDATE appointments a
JOIN doctors d ON d.doctor_id = a.doctor_id
JOIN departments dept ON dept.dept_id = d.dept_id
SET a.paraclinical_doctor_id = a.doctor_id,
    a.paraclinical_room_id = COALESCE(a.room_id, d.room_id)
WHERE dept.dept_type = 'PARACLINICAL'
  AND a.paraclinical_doctor_id IS NULL;