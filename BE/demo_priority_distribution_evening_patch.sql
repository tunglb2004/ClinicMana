-- Evening sample for room load balancing and priority queue.
-- Run BE/demo_priority_distribution_seed.sql first, then run this patch.
-- This patch creates a separate "toi" demo set so the morning sample is not overwritten.

SET @demo_date = DATE('2026-04-27');
SET @demo_now = TIMESTAMP('2026-04-27 18:00:00');
SET @dept_clinical = 11;
SET @slot_afternoon = 6;
-- Demo account password: Demo@123
SET @demo_password_hash = '$2b$10$PxV20z.J6iiHrGGM5S1d.ecTg5j1UHDtvnalTMVtfUaG9TX/xJ2oy';

DROP TEMPORARY TABLE IF EXISTS tmp_demo_priority_toi_records;

CREATE TEMPORARY TABLE tmp_demo_priority_toi_records AS
SELECT record_id
FROM medical_records
WHERE record_id LIKE 'D-REC-TOI-%'
   OR appointment_id LIKE 'D-APT-TOI-%';

DELETE pd
FROM prescription_details pd
JOIN prescriptions pr ON pr.prescription_id = pd.prescription_id
LEFT JOIN tmp_demo_priority_toi_records dr ON dr.record_id = pr.record_id
WHERE dr.record_id IS NOT NULL
   OR pr.record_id LIKE 'D-REC-TOI-%';

DELETE pr
FROM prescriptions pr
LEFT JOIN tmp_demo_priority_toi_records dr ON dr.record_id = pr.record_id
WHERE dr.record_id IS NOT NULL
   OR pr.record_id LIKE 'D-REC-TOI-%';

DELETE so
FROM service_orders so
LEFT JOIN tmp_demo_priority_toi_records dr ON dr.record_id = so.record_id
WHERE dr.record_id IS NOT NULL
   OR so.record_id LIKE 'D-REC-TOI-%'
   OR so.invoice_id LIKE 'D-INV-TOI-%'
   OR so.order_id LIKE 'D-ORD-TOI-%';

DELETE inv
FROM invoices inv
LEFT JOIN tmp_demo_priority_toi_records dr ON dr.record_id = inv.record_id
WHERE dr.record_id IS NOT NULL
   OR inv.record_id LIKE 'D-REC-TOI-%'
   OR inv.invoice_id LIKE 'D-INV-TOI-%';

UPDATE medical_records mr
SET mr.parent_record_id = NULL,
    mr.root_record_id = NULL
WHERE mr.record_id LIKE 'D-REC-TOI-%'
   OR mr.parent_record_id LIKE 'D-REC-TOI-%'
   OR mr.root_record_id LIKE 'D-REC-TOI-%';

UPDATE appointments a
SET a.follow_up_source_record_id = NULL
WHERE a.appointment_id LIKE 'D-APT-TOI-%'
   OR a.follow_up_source_record_id LIKE 'D-REC-TOI-%';

DELETE FROM medical_records
WHERE record_id IN (SELECT record_id FROM tmp_demo_priority_toi_records)
   OR record_id LIKE 'D-REC-TOI-%';

DELETE FROM appointment_services
WHERE appointment_id LIKE 'D-APT-TOI-%';

DELETE FROM appointments
WHERE appointment_id LIKE 'D-APT-TOI-%';

DROP TEMPORARY TABLE IF EXISTS tmp_demo_priority_toi_records;

DELETE FROM staff_schedules
WHERE work_date = @demo_date
  AND user_id IN (
    SELECT user_id FROM users
    WHERE username IN (
      'demo.toi.doctor1',
      'demo.toi.doctor2',
      'demo.toi.doctor3',
      'demo.toi.nurse',
      'demo.toi.reception',
      'demo.toi.cashier'
    )
  );

INSERT INTO users (username, email, password_hash, full_name, gender, phone_number, staff_id, is_active, is_deleted, must_change_password)
VALUES
  ('demo.toi.reception', 'demo.toi.reception@clinicpro.local', @demo_password_hash, 'Le tan Demo Toi', 'FEMALE', '0909010001', 'D-TOI-ST-REC', 1, 0, 0),
  ('demo.toi.cashier', 'demo.toi.cashier@clinicpro.local', @demo_password_hash, 'Thu ngan Demo Toi', 'FEMALE', '0909010002', 'D-TOI-ST-CASH', 1, 0, 0),
  ('demo.toi.doctor1', 'demo.toi.doctor1@clinicpro.local', @demo_password_hash, 'Bac si Demo Toi Noi 1', 'MALE', '0909010011', 'D-TOI-ST-DOC1', 1, 0, 0),
  ('demo.toi.doctor2', 'demo.toi.doctor2@clinicpro.local', @demo_password_hash, 'Bac si Demo Toi Noi 2', 'FEMALE', '0909010012', 'D-TOI-ST-DOC2', 1, 0, 0),
  ('demo.toi.doctor3', 'demo.toi.doctor3@clinicpro.local', @demo_password_hash, 'Bac si Demo Toi Noi 3', 'MALE', '0909010013', 'D-TOI-ST-DOC3', 1, 0, 0),
  ('demo.toi.nurse', 'demo.toi.nurse@clinicpro.local', @demo_password_hash, 'Y ta Demo Toi Phong Noi 1', 'FEMALE', '0909010021', 'D-TOI-ST-NURSE', 1, 0, 0),
  ('demo.toi.patient.alloc', 'demo.toi.patient.alloc@clinicpro.local', @demo_password_hash, 'BN Demo Toi Can Phan Phong', 'MALE', '0909010101', NULL, 1, 0, 0),
  ('demo.toi.patient.normal', 'demo.toi.patient.normal@clinicpro.local', @demo_password_hash, 'BN Demo Toi Kham Moi', 'MALE', '0909010102', NULL, 1, 0, 0),
  ('demo.toi.patient.wait', 'demo.toi.patient.wait@clinicpro.local', @demo_password_hash, 'BN Demo Toi Cho Rat Lau', 'FEMALE', '0909010103', NULL, 1, 0, 0),
  ('demo.toi.patient.urgent', 'demo.toi.patient.urgent@clinicpro.local', @demo_password_hash, 'BN Demo Toi Uu Tien Cao', 'MALE', '0909010104', NULL, 1, 0, 0),
  ('demo.toi.patient.return', 'demo.toi.patient.return@clinicpro.local', @demo_password_hash, 'BN Demo Toi Quay Lai CLS', 'FEMALE', '0909010105', NULL, 1, 0, 0),
  ('demo.toi.patient.follow', 'demo.toi.patient.follow@clinicpro.local', @demo_password_hash, 'BN Demo Toi Tai Kham', 'MALE', '0909010106', NULL, 1, 0, 0),
  ('demo.toi.patient.r2a', 'demo.toi.patient.r2a@clinicpro.local', @demo_password_hash, 'BN Demo Toi Phong Noi 2 A', 'FEMALE', '0909010107', NULL, 1, 0, 0),
  ('demo.toi.patient.r2b', 'demo.toi.patient.r2b@clinicpro.local', @demo_password_hash, 'BN Demo Toi Phong Noi 2 B', 'MALE', '0909010108', NULL, 1, 0, 0)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  full_name = VALUES(full_name),
  gender = VALUES(gender),
  phone_number = VALUES(phone_number),
  staff_id = VALUES(staff_id),
  is_active = 1,
  is_deleted = 0,
  must_change_password = 0;

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.role_name = 'ROLE_RECEPTIONIST'
WHERE u.username = 'demo.toi.reception';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.role_name = 'ROLE_CASHIER'
WHERE u.username = 'demo.toi.cashier';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.role_name = 'ROLE_DOCTOR'
WHERE u.username IN ('demo.toi.doctor1', 'demo.toi.doctor2', 'demo.toi.doctor3');

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.role_name = 'ROLE_NURSE'
WHERE u.username = 'demo.toi.nurse';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.role_name = 'ROLE_PATIENT'
WHERE u.username LIKE 'demo.toi.patient.%';

INSERT INTO rooms (room_id, room_name, room_type, location, dept_id, status, is_deleted, base_avg_time, current_queue_length)
VALUES
  ('D-NOI-TOI-1', 'Demo Toi Phong Noi 1', 'GENERAL', 'Tang 2 - Khu demo toi', @dept_clinical, 'ACTIVE', 0, 12, 5),
  ('D-NOI-TOI-2', 'Demo Toi Phong Noi 2', 'GENERAL', 'Tang 2 - Khu demo toi', @dept_clinical, 'ACTIVE', 0, 12, 2),
  ('D-NOI-TOI-3', 'Demo Toi Phong Noi 3', 'GENERAL', 'Tang 2 - Khu demo toi', @dept_clinical, 'ACTIVE', 0, 12, 0)
ON DUPLICATE KEY UPDATE
  room_name = VALUES(room_name),
  room_type = VALUES(room_type),
  location = VALUES(location),
  dept_id = VALUES(dept_id),
  status = 'ACTIVE',
  is_deleted = 0,
  base_avg_time = VALUES(base_avg_time),
  current_queue_length = VALUES(current_queue_length);

INSERT INTO services (service_id, service_name, price, description, is_active, is_deleted, max_patients, dept_id)
VALUES (
  'D-SVC-NOI-TOI',
  'Demo Toi - Kham Noi uu tien',
  350000.00,
  'Dich vu demo ca toi de chung minh chong doi vo han va phan phong theo tai phong.',
  1,
  0,
  8,
  @dept_clinical
)
ON DUPLICATE KEY UPDATE
  service_name = VALUES(service_name),
  price = VALUES(price),
  description = VALUES(description),
  is_active = 1,
  is_deleted = 0,
  max_patients = VALUES(max_patients),
  dept_id = VALUES(dept_id);

DELETE FROM service_room
WHERE service_id = 'D-SVC-NOI-TOI';

INSERT INTO service_room (room_id, service_id)
VALUES
  ('D-NOI-TOI-1', 'D-SVC-NOI-TOI'),
  ('D-NOI-TOI-2', 'D-SVC-NOI-TOI'),
  ('D-NOI-TOI-3', 'D-SVC-NOI-TOI');

INSERT INTO doctors (doctor_id, user_id, dept_id, specialization, degree, license_number, is_head_of_dept, room_id, position, doctor_type)
SELECT 'D-DOC-TOI-NOI1', u.user_id, @dept_clinical, 'Noi tong quat', 'BS.CKI', 'D-TOI-LIC-NOI1', 0, 'D-NOI-TOI-1', 'Bac si lam sang', 'CLINICAL'
FROM users u WHERE u.username = 'demo.toi.doctor1'
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  dept_id = VALUES(dept_id),
  specialization = VALUES(specialization),
  degree = VALUES(degree),
  license_number = VALUES(license_number),
  room_id = VALUES(room_id),
  position = VALUES(position),
  doctor_type = VALUES(doctor_type);

INSERT INTO doctors (doctor_id, user_id, dept_id, specialization, degree, license_number, is_head_of_dept, room_id, position, doctor_type)
SELECT 'D-DOC-TOI-NOI2', u.user_id, @dept_clinical, 'Noi tong quat', 'BS.CKI', 'D-TOI-LIC-NOI2', 0, 'D-NOI-TOI-2', 'Bac si lam sang', 'CLINICAL'
FROM users u WHERE u.username = 'demo.toi.doctor2'
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  dept_id = VALUES(dept_id),
  specialization = VALUES(specialization),
  degree = VALUES(degree),
  license_number = VALUES(license_number),
  room_id = VALUES(room_id),
  position = VALUES(position),
  doctor_type = VALUES(doctor_type);

INSERT INTO doctors (doctor_id, user_id, dept_id, specialization, degree, license_number, is_head_of_dept, room_id, position, doctor_type)
SELECT 'D-DOC-TOI-NOI3', u.user_id, @dept_clinical, 'Noi tong quat', 'BS.CKI', 'D-TOI-LIC-NOI3', 0, 'D-NOI-TOI-3', 'Bac si lam sang', 'CLINICAL'
FROM users u WHERE u.username = 'demo.toi.doctor3'
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  dept_id = VALUES(dept_id),
  specialization = VALUES(specialization),
  degree = VALUES(degree),
  license_number = VALUES(license_number),
  room_id = VALUES(room_id),
  position = VALUES(position),
  doctor_type = VALUES(doctor_type);

INSERT INTO nurses (nurse_id, user_id, certification_number, education_level, department, dept_id, position, years_of_experience, support_skills)
SELECT 'D-NURSE-TOI', u.user_id, 'D-TOI-NURSE-CERT', 'Cao dang dieu duong', 'Lam Sang', @dept_clinical, 'Y ta ho tro phong kham', 3, 'Do sinh hieu, nhap thong tin ban dau, goi benh nhan'
FROM users u WHERE u.username = 'demo.toi.nurse'
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  certification_number = VALUES(certification_number),
  education_level = VALUES(education_level),
  department = VALUES(department),
  dept_id = VALUES(dept_id),
  position = VALUES(position),
  years_of_experience = VALUES(years_of_experience),
  support_skills = VALUES(support_skills);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-TOI-ALLOC', u.user_id, DATE('1995-01-10'), 'O+', 'Dia chi demo toi'
FROM users u WHERE u.username = 'demo.toi.patient.alloc'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-TOI-NORM', u.user_id, DATE('1990-02-11'), 'A+', 'Dia chi demo toi'
FROM users u WHERE u.username = 'demo.toi.patient.normal'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-TOI-WAIT', u.user_id, DATE('1988-03-12'), 'B+', 'Dia chi demo toi'
FROM users u WHERE u.username = 'demo.toi.patient.wait'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-TOI-URG', u.user_id, DATE('1985-04-13'), 'AB+', 'Dia chi demo toi'
FROM users u WHERE u.username = 'demo.toi.patient.urgent'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-TOI-RET', u.user_id, DATE('1979-05-14'), 'O-', 'Dia chi demo toi'
FROM users u WHERE u.username = 'demo.toi.patient.return'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-TOI-FUP', u.user_id, DATE('1992-06-15'), 'A-', 'Dia chi demo toi'
FROM users u WHERE u.username = 'demo.toi.patient.follow'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-TOI-R2A', u.user_id, DATE('1994-07-16'), 'O+', 'Dia chi demo toi'
FROM users u WHERE u.username = 'demo.toi.patient.r2a'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-TOI-R2B', u.user_id, DATE('1987-08-17'), 'A+', 'Dia chi demo toi'
FROM users u WHERE u.username = 'demo.toi.patient.r2b'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_toi_staff_schedule;
CREATE TEMPORARY TABLE tmp_demo_toi_staff_schedule (
  username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  slot_id BIGINT NOT NULL,
  work_date DATE NOT NULL,
  room_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  note VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
);

INSERT INTO tmp_demo_toi_staff_schedule (username, slot_id, work_date, room_id, note)
VALUES
  ('demo.toi.doctor1', @slot_afternoon, @demo_date, 'D-NOI-TOI-1', 'DEMO_PRIORITY_TOI_DOC1_PM'),
  ('demo.toi.doctor2', @slot_afternoon, @demo_date, 'D-NOI-TOI-2', 'DEMO_PRIORITY_TOI_DOC2_PM'),
  ('demo.toi.doctor3', @slot_afternoon, @demo_date, 'D-NOI-TOI-3', 'DEMO_PRIORITY_TOI_DOC3_PM'),
  ('demo.toi.nurse', @slot_afternoon, @demo_date, 'D-NOI-TOI-1', 'DEMO_PRIORITY_TOI_NURSE_PM');

INSERT INTO staff_schedules (user_id, slot_id, work_date, date, start_time, end_time, status, dept_id, room_id, note, created_at, updated_at)
SELECT u.user_id, t.slot_id, t.work_date, t.work_date, s.start_time, s.end_time, 'SCHEDULED', @dept_clinical, t.room_id, t.note, @demo_now, @demo_now
FROM tmp_demo_toi_staff_schedule t
JOIN users u ON u.username = t.username
JOIN working_time_slots s ON s.slot_id = t.slot_id
ON DUPLICATE KEY UPDATE
  room_id = VALUES(room_id),
  status = 'SCHEDULED',
  dept_id = VALUES(dept_id),
  date = VALUES(date),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  note = VALUES(note),
  updated_at = @demo_now;

DROP TEMPORARY TABLE IF EXISTS tmp_demo_toi_staff_schedule;

INSERT INTO appointments (appointment_id, patient_id, doctor_id, room_id, appointment_date, appointment_time, status, visit_type, follow_up_source_record_id, queue_number, service_id, notes, priority_score, waiting_since, created_at)
VALUES
  ('D-APT-TOI-FOLD', 'D-PAT-TOI-FUP', 'D-DOC-TOI-NOI1', 'D-NOI-TOI-1', DATE_SUB(@demo_date, INTERVAL 5 DAY), '18:00:00', 'COMPLETED', 'INITIAL', NULL, 1, 'D-SVC-NOI-TOI', 'Demo toi ho so cu da hoan tat de dat tai kham', 0, NULL, DATE_SUB(@demo_now, INTERVAL 6 DAY));

INSERT INTO appointment_services (appointment_id, service_id)
VALUES ('D-APT-TOI-FOLD', 'D-SVC-NOI-TOI');

INSERT INTO medical_records (record_id, appointment_id, doctor_id, patient_id, symptoms, diagnosis, doctor_notes, status, visit_type, parent_record_id, root_record_id, created_at)
VALUES ('D-REC-TOI-FOLD', 'D-APT-TOI-FOLD', 'D-DOC-TOI-NOI1', 'D-PAT-TOI-FUP', 'Ho, sot nhe', 'Viem hong cap da on dinh', 'Hen tai kham sau 5 ngay neu con ho.', 'COMPLETED', 'INITIAL', NULL, NULL, DATE_SUB(@demo_now, INTERVAL 5 DAY));

INSERT INTO invoices (invoice_id, record_id, cashier_id, total_service_fee, insurance_paid, customer_paid, payment_status, payment_method, paid_at, bhyt_sync_status, receipt_status)
VALUES ('D-INV-TOI-FOLD', 'D-REC-TOI-FOLD', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.toi.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', DATE_SUB(@demo_now, INTERVAL 5 DAY), 'NOT_SYNCED', 'NONE');

INSERT INTO service_orders (order_id, record_id, service_id, technician_id, assigned_room_id, result_note, indication_note, status, invoice_id, order_type)
VALUES ('D-ORD-TOI-FOLD', 'D-REC-TOI-FOLD', 'D-SVC-NOI-TOI', NULL, 'D-NOI-TOI-1', NULL, 'Demo toi da thanh toan va hoan tat', 'PAID', 'D-INV-TOI-FOLD', 'INITIAL');

INSERT INTO appointments (appointment_id, patient_id, doctor_id, room_id, appointment_date, appointment_time, status, visit_type, follow_up_source_record_id, queue_number, service_id, notes, priority_score, waiting_since, created_at)
VALUES
  ('D-APT-TOI-WAIT', 'D-PAT-TOI-WAIT', 'D-DOC-TOI-NOI1', 'D-NOI-TOI-1', @demo_date, '18:00:00', 'WAITING', 'INITIAL', NULL, 1, 'D-SVC-NOI-TOI', 'Mau ca toi - benh nhan moi cho rat lau, dung de demo chong doi vo han', 0, TIMESTAMP(@demo_date, '16:00:00'), TIMESTAMP(@demo_date, '16:00:00')),
  ('D-APT-TOI-RET', 'D-PAT-TOI-RET', 'D-DOC-TOI-NOI1', 'D-NOI-TOI-1', @demo_date, '18:00:00', 'WAITING', 'INITIAL', NULL, 2, 'D-SVC-NOI-TOI', 'Mau ca toi - quay lai sau can lam sang: priority_score 500', 500, TIMESTAMP(@demo_date, '17:45:00'), TIMESTAMP(@demo_date, '17:15:00')),
  ('D-APT-TOI-URG', 'D-PAT-TOI-URG', 'D-DOC-TOI-NOI1', 'D-NOI-TOI-1', @demo_date, '18:00:00', 'WAITING', 'INITIAL', NULL, 3, 'D-SVC-NOI-TOI', 'Mau ca toi - uu tien nghiep vu/cap cuu: priority_score 500', 500, TIMESTAMP(@demo_date, '17:46:00'), TIMESTAMP(@demo_date, '17:16:00')),
  ('D-APT-TOI-FUP', 'D-PAT-TOI-FUP', 'D-DOC-TOI-NOI1', 'D-NOI-TOI-1', @demo_date, '18:00:00', 'WAITING', 'FOLLOW_UP', 'D-REC-TOI-FOLD', 4, 'D-SVC-NOI-TOI', '[Tai kham: D-REC-TOI-FOLD] Mau ca toi - tai kham ve bac si cu, priority_score 100', 100, TIMESTAMP(@demo_date, '17:20:00'), TIMESTAMP(@demo_date, '17:05:00')),
  ('D-APT-TOI-NORM', 'D-PAT-TOI-NORM', 'D-DOC-TOI-NOI1', 'D-NOI-TOI-1', @demo_date, '18:00:00', 'WAITING', 'INITIAL', NULL, 5, 'D-SVC-NOI-TOI', 'Mau ca toi - benh nhan moi binh thuong', 0, TIMESTAMP(@demo_date, '17:50:00'), TIMESTAMP(@demo_date, '17:35:00')),
  ('D-APT-TOI-R2A', 'D-PAT-TOI-R2A', 'D-DOC-TOI-NOI2', 'D-NOI-TOI-2', @demo_date, '18:00:00', 'WAITING', 'INITIAL', NULL, 1, 'D-SVC-NOI-TOI', 'Mau ca toi - phong Noi Toi 2 dang co 2 benh nhan cho', 0, TIMESTAMP(@demo_date, '17:52:00'), TIMESTAMP(@demo_date, '17:40:00')),
  ('D-APT-TOI-R2B', 'D-PAT-TOI-R2B', 'D-DOC-TOI-NOI2', 'D-NOI-TOI-2', @demo_date, '18:00:00', 'WAITING', 'INITIAL', NULL, 2, 'D-SVC-NOI-TOI', 'Mau ca toi - phong Noi Toi 2 dang co 2 benh nhan cho', 0, TIMESTAMP(@demo_date, '17:54:00'), TIMESTAMP(@demo_date, '17:42:00')),
  ('D-APT-TOI-ALLOC', 'D-PAT-TOI-ALLOC', NULL, NULL, @demo_date, '18:00:00', 'CONFIRMED', 'INITIAL', NULL, NULL, 'D-SVC-NOI-TOI', 'Mau ca toi - chua thanh toan: khi thu tien se phan phong chinh thuc vao phong tai thap nhat', NULL, NULL, TIMESTAMP(@demo_date, '17:30:00'));

INSERT INTO appointment_services (appointment_id, service_id)
VALUES
  ('D-APT-TOI-WAIT', 'D-SVC-NOI-TOI'),
  ('D-APT-TOI-RET', 'D-SVC-NOI-TOI'),
  ('D-APT-TOI-URG', 'D-SVC-NOI-TOI'),
  ('D-APT-TOI-FUP', 'D-SVC-NOI-TOI'),
  ('D-APT-TOI-NORM', 'D-SVC-NOI-TOI'),
  ('D-APT-TOI-R2A', 'D-SVC-NOI-TOI'),
  ('D-APT-TOI-R2B', 'D-SVC-NOI-TOI'),
  ('D-APT-TOI-ALLOC', 'D-SVC-NOI-TOI');

INSERT INTO medical_records (record_id, appointment_id, doctor_id, patient_id, symptoms, diagnosis, doctor_notes, status, visit_type, parent_record_id, root_record_id, created_at)
VALUES
  ('D-REC-TOI-WAIT', 'D-APT-TOI-WAIT', 'D-DOC-TOI-NOI1', 'D-PAT-TOI-WAIT', 'Dau dau', 'Cho kham lau', 'Demo toi waiting-time bonus.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now),
  ('D-REC-TOI-RET', 'D-APT-TOI-RET', 'D-DOC-TOI-NOI1', 'D-PAT-TOI-RET', 'Da co ket qua can lam sang', 'Dang cho bac si ket luan', 'Demo toi quay lai sau can lam sang.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now),
  ('D-REC-TOI-URG', 'D-APT-TOI-URG', 'D-DOC-TOI-NOI1', 'D-PAT-TOI-URG', 'Dau nguc, kho tho nhe', 'Can uu tien danh gia', 'Demo toi uu tien nghiep vu/cap cuu.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now),
  ('D-REC-TOI-FUP', 'D-APT-TOI-FUP', 'D-DOC-TOI-NOI1', 'D-PAT-TOI-FUP', 'Tai kham sau don thuoc cu', 'Theo doi sau dieu tri', 'Demo toi record moi lien ket ho so cu.', 'WAITING', 'FOLLOW_UP', 'D-REC-TOI-FOLD', 'D-REC-TOI-FOLD', @demo_now),
  ('D-REC-TOI-NORM', 'D-APT-TOI-NORM', 'D-DOC-TOI-NOI1', 'D-PAT-TOI-NORM', 'Met moi', 'Cho kham moi', 'Demo toi benh nhan moi binh thuong.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now),
  ('D-REC-TOI-R2A', 'D-APT-TOI-R2A', 'D-DOC-TOI-NOI2', 'D-PAT-TOI-R2A', 'Dau bung', 'Cho kham phong 2', 'Demo toi phong Noi 2 A.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now),
  ('D-REC-TOI-R2B', 'D-APT-TOI-R2B', 'D-DOC-TOI-NOI2', 'D-PAT-TOI-R2B', 'Mat ngu', 'Cho kham phong 2', 'Demo toi phong Noi 2 B.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now);

INSERT INTO invoices (invoice_id, record_id, cashier_id, total_service_fee, insurance_paid, customer_paid, payment_status, payment_method, paid_at, bhyt_sync_status, receipt_status)
VALUES
  ('D-INV-TOI-WAIT', 'D-REC-TOI-WAIT', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.toi.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-TOI-RET', 'D-REC-TOI-RET', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.toi.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-TOI-URG', 'D-REC-TOI-URG', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.toi.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-TOI-FUP', 'D-REC-TOI-FUP', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.toi.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-TOI-NORM', 'D-REC-TOI-NORM', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.toi.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-TOI-R2A', 'D-REC-TOI-R2A', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.toi.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-TOI-R2B', 'D-REC-TOI-R2B', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.toi.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE');

INSERT INTO service_orders (order_id, record_id, service_id, technician_id, assigned_room_id, result_note, indication_note, status, invoice_id, order_type)
VALUES
  ('D-ORD-TOI-WAIT', 'D-REC-TOI-WAIT', 'D-SVC-NOI-TOI', NULL, 'D-NOI-TOI-1', NULL, 'Demo toi cho rat lau', 'PAID', 'D-INV-TOI-WAIT', 'INITIAL'),
  ('D-ORD-TOI-RET', 'D-REC-TOI-RET', 'D-SVC-NOI-TOI', NULL, 'D-NOI-TOI-1', NULL, 'Demo toi quay lai sau can lam sang', 'PAID', 'D-INV-TOI-RET', 'INITIAL'),
  ('D-ORD-TOI-URG', 'D-REC-TOI-URG', 'D-SVC-NOI-TOI', NULL, 'D-NOI-TOI-1', NULL, 'Demo toi uu tien cao', 'PAID', 'D-INV-TOI-URG', 'INITIAL'),
  ('D-ORD-TOI-FUP', 'D-REC-TOI-FUP', 'D-SVC-NOI-TOI', NULL, 'D-NOI-TOI-1', NULL, 'Demo toi tai kham record moi', 'PAID', 'D-INV-TOI-FUP', 'INITIAL'),
  ('D-ORD-TOI-NORM', 'D-REC-TOI-NORM', 'D-SVC-NOI-TOI', NULL, 'D-NOI-TOI-1', NULL, 'Demo toi kham moi', 'PAID', 'D-INV-TOI-NORM', 'INITIAL'),
  ('D-ORD-TOI-R2A', 'D-REC-TOI-R2A', 'D-SVC-NOI-TOI', NULL, 'D-NOI-TOI-2', NULL, 'Demo toi phong Noi 2 A', 'PAID', 'D-INV-TOI-R2A', 'INITIAL'),
  ('D-ORD-TOI-R2B', 'D-REC-TOI-R2B', 'D-SVC-NOI-TOI', NULL, 'D-NOI-TOI-2', NULL, 'Demo toi phong Noi 2 B', 'PAID', 'D-INV-TOI-R2B', 'INITIAL');

UPDATE rooms SET current_queue_length = 5 WHERE room_id = 'D-NOI-TOI-1';
UPDATE rooms SET current_queue_length = 2 WHERE room_id = 'D-NOI-TOI-2';
UPDATE rooms SET current_queue_length = 0 WHERE room_id = 'D-NOI-TOI-3';

SELECT
  'DEMO_EVENING_TOI_2026_04_27_READY' AS result,
  @demo_date AS demo_date,
  '18:00:00' AS appointment_time,
  'D-SVC-NOI-TOI' AS service_id;

SELECT
  r.room_id,
  r.room_name,
  r.current_queue_length
FROM rooms r
WHERE r.room_id IN ('D-NOI-TOI-1', 'D-NOI-TOI-2', 'D-NOI-TOI-3')
ORDER BY r.room_id;

SELECT
  a.appointment_id,
  a.queue_number,
  p.patient_id,
  u.full_name,
  a.priority_score AS base_priority,
  TIMESTAMPDIFF(MINUTE, a.waiting_since, @demo_now) AS waiting_minutes,
  CASE
    WHEN TIMESTAMPDIFF(MINUTE, a.waiting_since, @demo_now) >= 90 THEN 1200
    WHEN TIMESTAMPDIFF(MINUTE, a.waiting_since, @demo_now) >= 60 THEN 700
    WHEN TIMESTAMPDIFF(MINUTE, a.waiting_since, @demo_now) >= 30 THEN 300
    ELSE 0
  END AS aging_bonus,
  COALESCE(a.priority_score, 0)
    + COALESCE(TIMESTAMPDIFF(MINUTE, a.waiting_since, @demo_now), 0)
    + CASE
        WHEN TIMESTAMPDIFF(MINUTE, a.waiting_since, @demo_now) >= 90 THEN 1200
        WHEN TIMESTAMPDIFF(MINUTE, a.waiting_since, @demo_now) >= 60 THEN 700
        WHEN TIMESTAMPDIFF(MINUTE, a.waiting_since, @demo_now) >= 30 THEN 300
        ELSE 0
      END AS total_priority_score
FROM appointments a
JOIN patients p ON p.patient_id = a.patient_id
JOIN users u ON u.user_id = p.user_id
WHERE a.appointment_date = @demo_date
  AND a.room_id = 'D-NOI-TOI-1'
  AND a.status = 'WAITING'
ORDER BY total_priority_score DESC, a.queue_number ASC, a.created_at ASC;
