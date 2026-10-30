DROP TABLE IF EXISTS service_room;

CREATE TABLE service_room (
    room_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    service_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (room_id, service_id),
    FOREIGN KEY (room_id) REFERENCES rooms(room_id),
    FOREIGN KEY (service_id) REFERENCES services(service_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed data: map dịch vụ vào phòng phù hợp

-- Phòng khám đa khoa (DK01, DK02) -> Khám tổng quát
INSERT INTO service_room (room_id, service_id) SELECT 'DK01', service_id FROM services WHERE service_name LIKE '%khám%' OR service_name LIKE '%tổng quát%' OR service_name LIKE '%tư vấn%';
INSERT INTO service_room (room_id, service_id) SELECT 'DK02', service_id FROM services WHERE service_name LIKE '%khám%' OR service_name LIKE '%tổng quát%' OR service_name LIKE '%tư vấn%';

-- Phòng chuyên khoa Tim mạch (CK_TM)
INSERT IGNORE INTO service_room (room_id, service_id) SELECT 'CK_TM', service_id FROM services WHERE service_name LIKE '%tim%' OR service_name LIKE '%mạch%' OR service_name LIKE '%huyết áp%' OR service_name LIKE '%điện tâm đồ%' OR service_name LIKE '%ECG%';

-- Phòng chuyên khoa Tiêu hóa (CK_TH)
INSERT IGNORE INTO service_room (room_id, service_id) SELECT 'CK_TH', service_id FROM services WHERE service_name LIKE '%tiêu hóa%' OR service_name LIKE '%dạ dày%' OR service_name LIKE '%gan%' OR service_name LIKE '%nội soi%';

-- Phòng chuyên khoa Tai Mũi Họng (CK_TMH)
INSERT IGNORE INTO service_room (room_id, service_id) SELECT 'CK_TMH', service_id FROM services WHERE service_name LIKE '%tai%' OR service_name LIKE '%mũi%' OR service_name LIKE '%họng%' OR service_name LIKE '%thính%';

-- Phòng chuyên khoa Răng Hàm Mặt (CK_RHM)
-- Dùng COLLATE utf8mb4_bin để tránh 'hàm' match false positive với 'Khám' (accent-insensitive)
INSERT IGNORE INTO service_room (room_id, service_id) SELECT 'CK_RHM', service_id FROM services WHERE service_name COLLATE utf8mb4_bin LIKE '%răng%' OR service_name COLLATE utf8mb4_bin LIKE '%Răng%' OR service_name LIKE '%nha %' OR service_name LIKE '%nha khoa%' OR service_name COLLATE utf8mb4_bin LIKE '%hàm%' OR service_name COLLATE utf8mb4_bin LIKE '%Hàm%';

-- Phòng Lấy máu XN (LAB_MAU)
INSERT IGNORE INTO service_room (room_id, service_id) SELECT 'LAB_MAU', service_id FROM services WHERE service_name LIKE '%máu%' OR service_name LIKE '%huyết%' OR service_name LIKE '%xét nghiệm%' OR service_name LIKE '%sinh hóa%';

-- Phòng Siêu âm (LAB_SA)
INSERT IGNORE INTO service_room (room_id, service_id) SELECT 'LAB_SA', service_id FROM services WHERE service_name LIKE '%siêu âm%' OR service_name LIKE '%SA %';

-- Phòng X-Quang (LAB_XQ)
INSERT IGNORE INTO service_room (room_id, service_id) SELECT 'LAB_XQ', service_id FROM services WHERE service_name LIKE '%X-quang%' OR service_name LIKE '%Xquang%' OR service_name LIKE '%chụp%' OR service_name LIKE '%CT%' OR service_name LIKE '%MRI%';

-- Phòng XN máu (LAB211)
INSERT IGNORE INTO service_room (room_id, service_id) SELECT 'LAB211', service_id FROM services WHERE service_name LIKE '%máu%' OR service_name LIKE '%xét nghiệm%';
