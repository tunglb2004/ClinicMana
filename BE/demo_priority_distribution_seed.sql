-- Demo data for room load balancing and priority queue.
-- Scope: rebuild only D-* appointments/records/orders/invoices and demo.* accounts.
-- Morning demo date in the local clinic environment: 2026-04-27.
-- For the evening sample, run this file first, then run demo_priority_distribution_evening_patch.sql.

SET @demo_date = DATE('2026-04-27');
SET @demo_now = TIMESTAMP('2026-04-27 07:30:00');
SET @dept_clinical = 11;
SET @slot_morning = 5;
SET @slot_afternoon = 6;
-- Demo account password: Demo@123
SET @demo_password_hash = '$2b$10$PxV20z.J6iiHrGGM5S1d.ecTg5j1UHDtvnalTMVtfUaG9TX/xJ2oy';

DROP TEMPORARY TABLE IF EXISTS tmp_demo_priority_records;

CREATE TEMPORARY TABLE tmp_demo_priority_records AS
SELECT record_id
FROM medical_records
WHERE record_id LIKE 'D-REC-%'
   OR appointment_id LIKE 'D-APT-%';

DELETE pd
FROM prescription_details pd
JOIN prescriptions pr ON pr.prescription_id = pd.prescription_id
LEFT JOIN tmp_demo_priority_records dr ON dr.record_id = pr.record_id
WHERE dr.record_id IS NOT NULL
   OR pr.record_id LIKE 'D-REC-%';

DELETE pr
FROM prescriptions pr
LEFT JOIN tmp_demo_priority_records dr ON dr.record_id = pr.record_id
WHERE dr.record_id IS NOT NULL
   OR pr.record_id LIKE 'D-REC-%';

DELETE so
FROM service_orders so
LEFT JOIN tmp_demo_priority_records dr ON dr.record_id = so.record_id
WHERE dr.record_id IS NOT NULL
   OR so.record_id LIKE 'D-REC-%'
   OR so.invoice_id LIKE 'D-INV-%'
   OR so.order_id LIKE 'D-ORD-%';

DELETE inv
FROM invoices inv
LEFT JOIN tmp_demo_priority_records dr ON dr.record_id = inv.record_id
WHERE dr.record_id IS NOT NULL
   OR inv.record_id LIKE 'D-REC-%'
   OR inv.invoice_id LIKE 'D-INV-%';

UPDATE medical_records mr
SET mr.parent_record_id = NULL,
    mr.root_record_id = NULL
WHERE mr.record_id LIKE 'D-REC-%'
   OR mr.parent_record_id LIKE 'D-REC-%'
   OR mr.root_record_id LIKE 'D-REC-%';

UPDATE appointments a
SET a.follow_up_source_record_id = NULL
WHERE a.appointment_id LIKE 'D-APT-%'
   OR a.follow_up_source_record_id LIKE 'D-REC-%';

DELETE FROM medical_records
WHERE record_id IN (SELECT record_id FROM tmp_demo_priority_records)
   OR record_id LIKE 'D-REC-%';

DELETE FROM appointment_services
WHERE appointment_id LIKE 'D-APT-%';

DELETE FROM appointments
WHERE appointment_id LIKE 'D-APT-%';

DROP TEMPORARY TABLE IF EXISTS tmp_demo_priority_records;

DELETE FROM staff_schedules
WHERE work_date = @demo_date
  AND user_id IN (
    SELECT user_id FROM users
    WHERE username IN (
      'demo.doctor1',
      'demo.doctor2',
      'demo.doctor3',
      'demo.nurse',
      'demo.reception',
      'demo.cashier'
    )
  );

INSERT INTO users (username, email, password_hash, full_name, gender, phone_number, staff_id, is_active, is_deleted, must_change_password)
VALUES
  ('demo.reception', 'demo.reception@clinicpro.local', @demo_password_hash, 'Le tan Demo', 'FEMALE', '0909000001', 'D-ST-REC', 1, 0, 0),
  ('demo.cashier', 'demo.cashier@clinicpro.local', @demo_password_hash, 'Thu ngan Demo', 'FEMALE', '0909000002', 'D-ST-CASH', 1, 0, 0),
  ('demo.doctor1', 'demo.doctor1@clinicpro.local', @demo_password_hash, 'Bac si Demo Noi 1', 'MALE', '0909000011', 'D-ST-DOC1', 1, 0, 0),
  ('demo.doctor2', 'demo.doctor2@clinicpro.local', @demo_password_hash, 'Bac si Demo Noi 2', 'FEMALE', '0909000012', 'D-ST-DOC2', 1, 0, 0),
  ('demo.doctor3', 'demo.doctor3@clinicpro.local', @demo_password_hash, 'Bac si Demo Noi 3', 'MALE', '0909000013', 'D-ST-DOC3', 1, 0, 0),
  ('demo.nurse', 'demo.nurse@clinicpro.local', @demo_password_hash, 'Y ta Demo Phong Noi 1', 'FEMALE', '0909000021', 'D-ST-NURSE', 1, 0, 0),
  ('demo.patient.alloc', 'demo.patient.alloc@clinicpro.local', @demo_password_hash, 'BN Demo Can Phan Phong Hom Nay', 'MALE', '0909000101', NULL, 1, 0, 0),
  ('demo.patient.normal', 'demo.patient.normal@clinicpro.local', @demo_password_hash, 'BN Demo Kham Moi', 'MALE', '0909000102', NULL, 1, 0, 0),
  ('demo.patient.wait', 'demo.patient.wait@clinicpro.local', @demo_password_hash, 'BN Demo Cho Lau', 'FEMALE', '0909000103', NULL, 1, 0, 0),
  ('demo.patient.urgent', 'demo.patient.urgent@clinicpro.local', @demo_password_hash, 'BN Demo Uu Tien Cao', 'MALE', '0909000104', NULL, 1, 0, 0),
  ('demo.patient.return', 'demo.patient.return@clinicpro.local', @demo_password_hash, 'BN Demo Quay Lai CLS', 'FEMALE', '0909000105', NULL, 1, 0, 0),
  ('demo.patient.follow', 'demo.patient.follow@clinicpro.local', @demo_password_hash, 'BN Demo Tai Kham', 'MALE', '0909000106', NULL, 1, 0, 0),
  ('demo.patient.r2a', 'demo.patient.r2a@clinicpro.local', @demo_password_hash, 'BN Demo Phong Noi 2 A', 'FEMALE', '0909000107', NULL, 1, 0, 0),
  ('demo.patient.r2b', 'demo.patient.r2b@clinicpro.local', @demo_password_hash, 'BN Demo Phong Noi 2 B', 'MALE', '0909000108', NULL, 1, 0, 0),
  ('demo.patient.tomorrow1', 'demo.patient.tomorrow1@clinicpro.local', @demo_password_hash, 'BN Demo Ngay Mai 1', 'FEMALE', '0909000201', NULL, 1, 0, 0),
  ('demo.patient.tomorrow2', 'demo.patient.tomorrow2@clinicpro.local', @demo_password_hash, 'BN Demo Ngay Mai 2', 'MALE', '0909000202', NULL, 1, 0, 0)
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
WHERE u.username = 'demo.reception';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.role_name = 'ROLE_CASHIER'
WHERE u.username = 'demo.cashier';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.role_name = 'ROLE_DOCTOR'
WHERE u.username IN ('demo.doctor1', 'demo.doctor2', 'demo.doctor3');

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.role_name = 'ROLE_NURSE'
WHERE u.username = 'demo.nurse';

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON r.role_name = 'ROLE_PATIENT'
WHERE u.username LIKE 'demo.patient.%';

INSERT INTO rooms (room_id, room_name, room_type, location, dept_id, status, is_deleted, base_avg_time, current_queue_length)
VALUES
  ('D-NOI-1', 'Demo Phong Noi 1', 'GENERAL', 'Tang 2 - Khu demo', @dept_clinical, 'ACTIVE', 0, 12, 5),
  ('D-NOI-2', 'Demo Phong Noi 2', 'GENERAL', 'Tang 2 - Khu demo', @dept_clinical, 'ACTIVE', 0, 12, 2),
  ('D-NOI-3', 'Demo Phong Noi 3', 'GENERAL', 'Tang 2 - Khu demo', @dept_clinical, 'ACTIVE', 0, 12, 0)
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
  'D-SVC-NOI',
  'Demo - Kham Noi uu tien',
  350000.00,
  'Dich vu demo de chung minh phan phong theo tai phong va hang doi uu tien.',
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
WHERE service_id = 'D-SVC-NOI';

INSERT INTO service_room (room_id, service_id)
VALUES
  ('D-NOI-1', 'D-SVC-NOI'),
  ('D-NOI-2', 'D-SVC-NOI'),
  ('D-NOI-3', 'D-SVC-NOI');

INSERT INTO doctors (doctor_id, user_id, dept_id, specialization, degree, license_number, is_head_of_dept, room_id, position, doctor_type)
SELECT 'D-DOC-NOI1', u.user_id, @dept_clinical, 'Noi tong quat', 'BS.CKI', 'D-LIC-NOI1', 0, 'D-NOI-1', 'Bac si lam sang', 'CLINICAL'
FROM users u WHERE u.username = 'demo.doctor1'
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
SELECT 'D-DOC-NOI2', u.user_id, @dept_clinical, 'Noi tong quat', 'BS.CKI', 'D-LIC-NOI2', 0, 'D-NOI-2', 'Bac si lam sang', 'CLINICAL'
FROM users u WHERE u.username = 'demo.doctor2'
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
SELECT 'D-DOC-NOI3', u.user_id, @dept_clinical, 'Noi tong quat', 'BS.CKI', 'D-LIC-NOI3', 0, 'D-NOI-3', 'Bac si lam sang', 'CLINICAL'
FROM users u WHERE u.username = 'demo.doctor3'
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
SELECT 'D-NURSE-1', u.user_id, 'D-NURSE-CERT', 'Cao dang dieu duong', 'Lam Sang', @dept_clinical, 'Y ta ho tro phong kham', 3, 'Do sinh hieu, nhap thong tin ban dau, goi benh nhan'
FROM users u WHERE u.username = 'demo.nurse'
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
SELECT 'D-PAT-ALLOC', u.user_id, DATE('1995-01-10'), 'O+', 'Dia chi demo'
FROM users u WHERE u.username = 'demo.patient.alloc'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-NORM', u.user_id, DATE('1990-02-11'), 'A+', 'Dia chi demo'
FROM users u WHERE u.username = 'demo.patient.normal'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-WAIT', u.user_id, DATE('1988-03-12'), 'B+', 'Dia chi demo'
FROM users u WHERE u.username = 'demo.patient.wait'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-URG', u.user_id, DATE('1985-04-13'), 'AB+', 'Dia chi demo'
FROM users u WHERE u.username = 'demo.patient.urgent'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-RET', u.user_id, DATE('1979-05-14'), 'O-', 'Dia chi demo'
FROM users u WHERE u.username = 'demo.patient.return'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-FUP', u.user_id, DATE('1992-06-15'), 'A-', 'Dia chi demo'
FROM users u WHERE u.username = 'demo.patient.follow'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-R2A', u.user_id, DATE('1994-07-16'), 'O+', 'Dia chi demo'
FROM users u WHERE u.username = 'demo.patient.r2a'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-R2B', u.user_id, DATE('1987-08-17'), 'A+', 'Dia chi demo'
FROM users u WHERE u.username = 'demo.patient.r2b'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-TOM1', u.user_id, DATE('1996-09-18'), 'B+', 'Dia chi demo'
FROM users u WHERE u.username = 'demo.patient.tomorrow1'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

INSERT INTO patients (patient_id, user_id, date_of_birth, blood_type, address)
SELECT 'D-PAT-TOM2', u.user_id, DATE('1989-10-19'), 'AB+', 'Dia chi demo'
FROM users u WHERE u.username = 'demo.patient.tomorrow2'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), date_of_birth = VALUES(date_of_birth), blood_type = VALUES(blood_type), address = VALUES(address);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_staff_schedule;
CREATE TEMPORARY TABLE tmp_demo_staff_schedule (
  username VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  slot_id BIGINT NOT NULL,
  work_date DATE NOT NULL,
  room_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  note VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
);

INSERT INTO tmp_demo_staff_schedule (username, slot_id, work_date, room_id, note)
VALUES
  ('demo.doctor1', @slot_morning, @demo_date, 'D-NOI-1', 'DEMO_PRIORITY_TODAY_DOC1_AM'),
  ('demo.doctor2', @slot_morning, @demo_date, 'D-NOI-2', 'DEMO_PRIORITY_TODAY_DOC2_AM'),
  ('demo.doctor3', @slot_morning, @demo_date, 'D-NOI-3', 'DEMO_PRIORITY_TODAY_DOC3_AM'),
  ('demo.nurse', @slot_morning, @demo_date, 'D-NOI-1', 'DEMO_PRIORITY_TODAY_NURSE_AM'),
  ('demo.doctor1', @slot_afternoon, @demo_date, 'D-NOI-1', 'DEMO_PRIORITY_TODAY_DOC1_PM'),
  ('demo.doctor2', @slot_afternoon, @demo_date, 'D-NOI-2', 'DEMO_PRIORITY_TODAY_DOC2_PM'),
  ('demo.doctor3', @slot_afternoon, @demo_date, 'D-NOI-3', 'DEMO_PRIORITY_TODAY_DOC3_PM');

INSERT INTO staff_schedules (user_id, slot_id, work_date, date, start_time, end_time, status, dept_id, room_id, note, created_at, updated_at)
SELECT u.user_id, t.slot_id, t.work_date, t.work_date, s.start_time, s.end_time, 'SCHEDULED', @dept_clinical, t.room_id, t.note, @demo_now, @demo_now
FROM tmp_demo_staff_schedule t
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

DROP TEMPORARY TABLE IF EXISTS tmp_demo_staff_schedule;

INSERT INTO appointments (appointment_id, patient_id, doctor_id, room_id, appointment_date, appointment_time, status, visit_type, follow_up_source_record_id, queue_number, service_id, notes, priority_score, waiting_since, created_at)
VALUES
  ('D-APT-FOLD', 'D-PAT-FUP', 'D-DOC-NOI1', 'D-NOI-1', DATE_SUB(@demo_date, INTERVAL 5 DAY), '07:00:00', 'COMPLETED', 'INITIAL', NULL, 1, 'D-SVC-NOI', 'Demo ho so cu da hoan tat de dat tai kham', 0, NULL, DATE_SUB(@demo_now, INTERVAL 6 DAY));

INSERT INTO appointment_services (appointment_id, service_id)
VALUES ('D-APT-FOLD', 'D-SVC-NOI');

INSERT INTO medical_records (record_id, appointment_id, doctor_id, patient_id, symptoms, diagnosis, doctor_notes, status, visit_type, parent_record_id, root_record_id, created_at)
VALUES ('D-REC-FOLD', 'D-APT-FOLD', 'D-DOC-NOI1', 'D-PAT-FUP', 'Ho, sot nhe', 'Viem hong cap da on dinh', 'Hen tai kham sau 5 ngay neu con ho.', 'COMPLETED', 'INITIAL', NULL, NULL, DATE_SUB(@demo_now, INTERVAL 5 DAY));

INSERT INTO invoices (invoice_id, record_id, cashier_id, total_service_fee, insurance_paid, customer_paid, payment_status, payment_method, paid_at, bhyt_sync_status, receipt_status)
VALUES ('D-INV-FOLD', 'D-REC-FOLD', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', DATE_SUB(@demo_now, INTERVAL 5 DAY), 'NOT_SYNCED', 'NONE');

INSERT INTO service_orders (order_id, record_id, service_id, technician_id, assigned_room_id, result_note, indication_note, status, invoice_id, order_type)
VALUES ('D-ORD-FOLD', 'D-REC-FOLD', 'D-SVC-NOI', NULL, 'D-NOI-1', NULL, 'Demo da thanh toan va hoan tat', 'PAID', 'D-INV-FOLD', 'INITIAL');

INSERT INTO appointments (appointment_id, patient_id, doctor_id, room_id, appointment_date, appointment_time, status, visit_type, follow_up_source_record_id, queue_number, service_id, notes, priority_score, waiting_since, created_at)
VALUES
  ('D-APT-RET', 'D-PAT-RET', 'D-DOC-NOI1', 'D-NOI-1', @demo_date, '07:00:00', 'WAITING', 'INITIAL', NULL, 1, 'D-SVC-NOI', 'Mau ca sang - quay lai sau can lam sang: priority_score 500', 500, TIMESTAMP(@demo_date, '06:50:00'), TIMESTAMP(@demo_date, '06:20:00')),
  ('D-APT-URG', 'D-PAT-URG', 'D-DOC-NOI1', 'D-NOI-1', @demo_date, '07:00:00', 'WAITING', 'INITIAL', NULL, 2, 'D-SVC-NOI', 'Mau ca sang - uu tien nghiep vu/cap cuu: priority_score 500', 500, TIMESTAMP(@demo_date, '06:51:00'), TIMESTAMP(@demo_date, '06:25:00')),
  ('D-APT-FUP', 'D-PAT-FUP', 'D-DOC-NOI1', 'D-NOI-1', @demo_date, '07:00:00', 'WAITING', 'FOLLOW_UP', 'D-REC-FOLD', 3, 'D-SVC-NOI', '[Tai kham: D-REC-FOLD] Mau ca sang - tai kham ve bac si cu, priority_score 100', 100, TIMESTAMP(@demo_date, '06:52:00'), TIMESTAMP(@demo_date, '06:30:00')),
  ('D-APT-WAIT', 'D-PAT-WAIT', 'D-DOC-NOI1', 'D-NOI-1', @demo_date, '07:00:00', 'WAITING', 'INITIAL', NULL, 4, 'D-SVC-NOI', 'Mau ca sang - benh nhan moi cho lau co WaitingMinutes/AgingBonus', 0, TIMESTAMP(@demo_date, '06:53:00'), TIMESTAMP(@demo_date, '06:35:00')),
  ('D-APT-NORM', 'D-PAT-NORM', 'D-DOC-NOI1', 'D-NOI-1', @demo_date, '07:00:00', 'WAITING', 'INITIAL', NULL, 5, 'D-SVC-NOI', 'Mau ca sang - benh nhan moi binh thuong', 0, TIMESTAMP(@demo_date, '06:55:00'), TIMESTAMP(@demo_date, '06:40:00')),
  ('D-APT-R2A', 'D-PAT-R2A', 'D-DOC-NOI2', 'D-NOI-2', @demo_date, '07:00:00', 'WAITING', 'INITIAL', NULL, 1, 'D-SVC-NOI', 'Mau ca sang - phong Noi 2 dang co 2 benh nhan cho', 0, TIMESTAMP(@demo_date, '06:56:00'), TIMESTAMP(@demo_date, '06:45:00')),
  ('D-APT-R2B', 'D-PAT-R2B', 'D-DOC-NOI2', 'D-NOI-2', @demo_date, '07:00:00', 'WAITING', 'INITIAL', NULL, 2, 'D-SVC-NOI', 'Mau ca sang - phong Noi 2 dang co 2 benh nhan cho', 0, TIMESTAMP(@demo_date, '06:57:00'), TIMESTAMP(@demo_date, '06:50:00')),
  ('D-APT-ALLOC', 'D-PAT-ALLOC', NULL, NULL, @demo_date, '07:00:00', 'CONFIRMED', 'INITIAL', NULL, NULL, 'D-SVC-NOI', 'Mau ca sang - chua thanh toan: khi thu tien se phan phong chinh thuc vao phong tai thap nhat', NULL, NULL, TIMESTAMP(@demo_date, '06:30:00'));

INSERT INTO appointment_services (appointment_id, service_id)
VALUES
  ('D-APT-RET', 'D-SVC-NOI'),
  ('D-APT-URG', 'D-SVC-NOI'),
  ('D-APT-WAIT', 'D-SVC-NOI'),
  ('D-APT-NORM', 'D-SVC-NOI'),
  ('D-APT-FUP', 'D-SVC-NOI'),
  ('D-APT-R2A', 'D-SVC-NOI'),
  ('D-APT-R2B', 'D-SVC-NOI'),
  ('D-APT-ALLOC', 'D-SVC-NOI');

INSERT INTO medical_records (record_id, appointment_id, doctor_id, patient_id, symptoms, diagnosis, doctor_notes, status, visit_type, parent_record_id, root_record_id, created_at)
VALUES
  ('D-REC-RET', 'D-APT-RET', 'D-DOC-NOI1', 'D-PAT-RET', 'Da co ket qua can lam sang', 'Dang cho bac si ket luan', 'Demo quay lai sau can lam sang.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now),
  ('D-REC-URG', 'D-APT-URG', 'D-DOC-NOI1', 'D-PAT-URG', 'Dau nguc, kho tho nhe', 'Can uu tien danh gia', 'Demo uu tien nghiep vu/cap cuu.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now),
  ('D-REC-WAIT', 'D-APT-WAIT', 'D-DOC-NOI1', 'D-PAT-WAIT', 'Dau dau', 'Cho kham lau', 'Demo waiting-time bonus.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now),
  ('D-REC-NORM', 'D-APT-NORM', 'D-DOC-NOI1', 'D-PAT-NORM', 'Met moi', 'Cho kham moi', 'Demo benh nhan moi binh thuong.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now),
  ('D-REC-FUP', 'D-APT-FUP', 'D-DOC-NOI1', 'D-PAT-FUP', 'Tai kham sau don thuoc cu', 'Theo doi sau dieu tri', 'Demo record moi lien ket ho so cu.', 'WAITING', 'FOLLOW_UP', 'D-REC-FOLD', 'D-REC-FOLD', @demo_now),
  ('D-REC-R2A', 'D-APT-R2A', 'D-DOC-NOI2', 'D-PAT-R2A', 'Dau bung', 'Cho kham phong 2', 'Demo phong Noi 2 A.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now),
  ('D-REC-R2B', 'D-APT-R2B', 'D-DOC-NOI2', 'D-PAT-R2B', 'Mat ngu', 'Cho kham phong 2', 'Demo phong Noi 2 B.', 'WAITING', 'INITIAL', NULL, NULL, @demo_now);

INSERT INTO invoices (invoice_id, record_id, cashier_id, total_service_fee, insurance_paid, customer_paid, payment_status, payment_method, paid_at, bhyt_sync_status, receipt_status)
VALUES
  ('D-INV-RET', 'D-REC-RET', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-URG', 'D-REC-URG', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-WAIT', 'D-REC-WAIT', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-NORM', 'D-REC-NORM', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-FUP', 'D-REC-FUP', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-R2A', 'D-REC-R2A', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE'),
  ('D-INV-R2B', 'D-REC-R2B', (SELECT CAST(user_id AS CHAR) FROM users WHERE username = 'demo.cashier'), 350000.00, 0.00, 350000.00, 'PAID', 'CASH', @demo_now, 'NOT_SYNCED', 'NONE');

INSERT INTO service_orders (order_id, record_id, service_id, technician_id, assigned_room_id, result_note, indication_note, status, invoice_id, order_type)
VALUES
  ('D-ORD-RET', 'D-REC-RET', 'D-SVC-NOI', NULL, 'D-NOI-1', NULL, 'Demo quay lai sau can lam sang', 'PAID', 'D-INV-RET', 'INITIAL'),
  ('D-ORD-URG', 'D-REC-URG', 'D-SVC-NOI', NULL, 'D-NOI-1', NULL, 'Demo uu tien cao', 'PAID', 'D-INV-URG', 'INITIAL'),
  ('D-ORD-WAIT', 'D-REC-WAIT', 'D-SVC-NOI', NULL, 'D-NOI-1', NULL, 'Demo cho lau', 'PAID', 'D-INV-WAIT', 'INITIAL'),
  ('D-ORD-NORM', 'D-REC-NORM', 'D-SVC-NOI', NULL, 'D-NOI-1', NULL, 'Demo kham moi', 'PAID', 'D-INV-NORM', 'INITIAL'),
  ('D-ORD-FUP', 'D-REC-FUP', 'D-SVC-NOI', NULL, 'D-NOI-1', NULL, 'Demo tai kham record moi', 'PAID', 'D-INV-FUP', 'INITIAL'),
  ('D-ORD-R2A', 'D-REC-R2A', 'D-SVC-NOI', NULL, 'D-NOI-2', NULL, 'Demo phong Noi 2 A', 'PAID', 'D-INV-R2A', 'INITIAL'),
  ('D-ORD-R2B', 'D-REC-R2B', 'D-SVC-NOI', NULL, 'D-NOI-2', NULL, 'Demo phong Noi 2 B', 'PAID', 'D-INV-R2B', 'INITIAL');

UPDATE rooms SET current_queue_length = 5 WHERE room_id = 'D-NOI-1';
UPDATE rooms SET current_queue_length = 2 WHERE room_id = 'D-NOI-2';
UPDATE rooms SET current_queue_length = 0 WHERE room_id = 'D-NOI-3';

SELECT 'DEMO_MORNING_2026_04_27_READY' AS result, @demo_date AS demo_date, '07:00:00' AS appointment_time;
