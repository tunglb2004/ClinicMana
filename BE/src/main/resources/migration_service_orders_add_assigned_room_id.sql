ALTER TABLE service_orders
ADD COLUMN assigned_room_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL AFTER technician_id;

ALTER TABLE service_orders
ADD CONSTRAINT fk_service_orders_assigned_room
FOREIGN KEY (assigned_room_id) REFERENCES rooms(room_id);

UPDATE service_orders so
LEFT JOIN doctors technician ON technician.doctor_id = so.technician_id
LEFT JOIN service_room technician_sr
  ON technician_sr.room_id = technician.room_id
 AND technician_sr.service_id = so.service_id
LEFT JOIN medical_records mr ON mr.record_id = so.record_id
LEFT JOIN appointments a ON a.appointment_id = mr.appointment_id
LEFT JOIN service_room paraclinical_sr
  ON paraclinical_sr.room_id = a.paraclinical_room_id
 AND paraclinical_sr.service_id = so.service_id
LEFT JOIN service_room appointment_sr
  ON appointment_sr.room_id = a.room_id
 AND appointment_sr.service_id = so.service_id
SET so.assigned_room_id = COALESCE(
    CASE WHEN technician_sr.room_id IS NOT NULL THEN technician.room_id END,
    CASE WHEN paraclinical_sr.room_id IS NOT NULL THEN a.paraclinical_room_id END,
    CASE WHEN appointment_sr.room_id IS NOT NULL THEN a.room_id END
)
WHERE so.assigned_room_id IS NULL;
