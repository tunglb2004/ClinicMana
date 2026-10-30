-- Mock Citizen Data - BHXH master data for demo insurance lookup.

CREATE TABLE IF NOT EXISTS mock_citizen_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cccd_number VARCHAR(12) NOT NULL UNIQUE COMMENT 'So CCCD',
    insurance_code VARCHAR(15) UNIQUE COMMENT 'Ma the BHYT',
    full_name VARCHAR(255) NOT NULL COMMENT 'Ho ten day du',
    date_of_birth VARCHAR(8) COMMENT 'Ngay sinh yyyyMMdd',
    gender INT COMMENT 'Gioi tinh: 1=Nam, 2=Nu',
    address VARCHAR(255) COMMENT 'Dia chi',
    ma_dkbd VARCHAR(5) COMMENT 'Noi dang ky ban dau',
    benefit_level INT COMMENT 'Muc huong: 1, 2, 3, 4',
    coverage_rate INT COMMENT 'Muc huong BHYT: 80, 95, 100',
    valid_to DATE COMMENT 'Ngay het han the'
);

-- Recreate clean demo BHYT data. Addresses intentionally use UTF-8 Vietnamese text.
DELETE FROM mock_citizen_data;

INSERT INTO mock_citizen_data
    (cccd_number, insurance_code, full_name, date_of_birth, gender, address, ma_dkbd, benefit_level, coverage_rate, valid_to)
VALUES
    ('030012345678', 'GD4010112345678', 'NGUYEN VAN A', '19990515', 1, 'Số 123, Đường Láng, Đống Đa, Hà Nội', '01012', 4, 80, '2026-12-31'),
    ('033000000001', 'GD4010100000001', 'TRAN THI BICH', '19890101', 2, 'Số 12, phố Huế, Hai Bà Trưng, Hà Nội', '01012', 4, 80, '2026-12-31'),
    ('033000000002', 'TE1010100000002', 'LE VAN CUONG', '20200115', 1, 'Số 45, Nguyễn Trãi, Thanh Xuân, Hà Nội', '01012', 1, 100, '2026-12-31'),
    ('033000000003', 'HT3010100000003', 'PHAM THI DUNG', '19651220', 2, 'Số 88, Kim Mã, Ba Đình, Hà Nội', '01012', 3, 95, '2026-12-31'),
    ('033000000004', 'GD4010100000004', 'HOANG VAN EM', '19920310', 1, 'Số 19, Giải Phóng, Hoàng Mai, Hà Nội', '01012', 4, 80, '2026-12-31'),
    ('033000000005', 'GD4010100000005', 'DO THI HONG', '19930425', 2, 'Số 27, Trần Duy Hưng, Cầu Giấy, Hà Nội', '01012', 4, 80, '2026-12-31'),
    ('033000000006', 'GD4010100000006', 'VU VAN KHANH', '19940816', 1, 'Số 63, Lạc Long Quân, Tây Hồ, Hà Nội', '01012', 4, 80, '2026-12-31'),
    ('033000000007', 'TE1010100000007', 'BUI THI LAN', '20210305', 2, 'Số 101, Xuân Thủy, Cầu Giấy, Hà Nội', '01012', 1, 100, '2026-12-31'),
    ('033000000008', 'GD4010100000008', 'NGUYEN MINH QUAN', '19961111', 1, 'Số 9, Nguyễn Chí Thanh, Đống Đa, Hà Nội', '01012', 4, 80, '2026-12-31'),
    ('033000000009', 'HT3010100000009', 'TRINH THI MAI', '19600322', 2, 'Số 72, Bạch Mai, Hai Bà Trưng, Hà Nội', '01012', 3, 95, '2026-12-31'),
    ('033000000010', 'GD4010100000010', 'PHAN VAN NAM', '19981230', 1, 'Số 36, Tôn Đức Thắng, Đống Đa, Hà Nội', '01012', 4, 80, '2026-12-31');

-- ============================================================
-- MOCK INSURANCE SERVICES TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS mock_insurance_services (
    id INT AUTO_INCREMENT PRIMARY KEY,
    service_code VARCHAR(20) UNIQUE COMMENT 'Ma dich vu',
    service_name VARCHAR(255) COMMENT 'Ten dich vu',
    insurance_price DECIMAL(15, 2) COMMENT 'Gia BHYT',
    coverage_ratio INT DEFAULT 100 COMMENT 'Ty le chi tra BHYT'
);

INSERT INTO mock_insurance_services (service_code, service_name, insurance_price, coverage_ratio)
VALUES
    ('KB001', 'Khám bệnh nội khoa', 50000.00, 100),
    ('KB002', 'Khám bệnh ngoại khoa', 50000.00, 100),
    ('XN001', 'Xét nghiệm máu tổng quát', 100000.00, 100),
    ('XN002', 'Xét nghiệm nước tiểu', 50000.00, 100),
    ('CDHA001', 'Chụp X quang phổi', 150000.00, 100),
    ('CDHA002', 'Siêu âm bụng tổng quát', 200000.00, 100),
    ('THUOC001', 'Paracetamol 500mg', 5000.00, 100),
    ('THUOC002', 'Amoxicillin 500mg', 8000.00, 100)
ON DUPLICATE KEY UPDATE
    service_name = VALUES(service_name),
    insurance_price = VALUES(insurance_price),
    coverage_ratio = VALUES(coverage_ratio);

-- ============================================================
-- INSURANCE SUBMISSION & RECEIVED CLAIMS TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS insurance_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_code VARCHAR(100) UNIQUE NOT NULL COMMENT 'Ma ho so',
    service_order_id BIGINT COMMENT 'Ma phieu thanh toan trong he thong',
    cccd_number VARCHAR(12) COMMENT 'So CCCD benh nhan',
    insurance_code VARCHAR(15) COMMENT 'Ma the BHYT',
    patient_name VARCHAR(255) COMMENT 'Ho ten benh nhan',
    ma_dkbd VARCHAR(5) COMMENT 'Ma dang ky ban dau cua benh nhan',
    is_dung_tuyen BOOLEAN COMMENT 'Dung tuyen hay khong',
    total_amount DECIMAL(15, 2) COMMENT 'Tong chi phi',
    insurance_paid DECIMAL(15, 2) COMMENT 'BHYT thanh toan',
    patient_paid DECIMAL(15, 2) COMMENT 'Benh nhan thanh toan',
    file_path VARCHAR(500) COMMENT 'Duong dan file XML/JSON',
    xml_content LONGTEXT COMMENT 'Noi dung XML/JSON',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/SENT/ACCEPTED/REJECTED',
    response_message VARCHAR(1000) COMMENT 'Thong bao phan hoi tu BHXH',
    created_at DATETIME COMMENT 'Ngay gio tao',
    submitted_at DATETIME COMMENT 'Ngay gio gui len BHXH',
    responded_at DATETIME COMMENT 'Ngay gio nhan phan hoi',
    INDEX idx_cccd (cccd_number),
    INDEX idx_insurance_code (insurance_code),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS received_claims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_code VARCHAR(100) UNIQUE NOT NULL COMMENT 'Ma ho so tu phong kham',
    clinic_code VARCHAR(5) COMMENT 'Ma phong kham gui',
    cccd_number VARCHAR(12) COMMENT 'So CCCD benh nhan',
    insurance_code VARCHAR(15) COMMENT 'Ma the BHYT',
    patient_name VARCHAR(255) COMMENT 'Ho ten benh nhan',
    ma_dkbd VARCHAR(5) COMMENT 'Ma dang ky ban dau',
    is_dung_tuyen BOOLEAN COMMENT 'Dung tuyen hay khong',
    total_amount DECIMAL(15, 2) COMMENT 'Tong chi phi yeu cau',
    insurance_claim_amount DECIMAL(15, 2) COMMENT 'So tien BHYT yeu cau',
    insurance_approved_amount DECIMAL(15, 2) COMMENT 'So tien BHYT da duyet',
    patient_paid DECIMAL(15, 2) COMMENT 'Benh nhan da thanh toan',
    file_path VARCHAR(500) COMMENT 'Duong dan file nhan duoc',
    xml_content LONGTEXT COMMENT 'Noi dung XML/JSON nhan duoc',
    status VARCHAR(20) DEFAULT 'RECEIVED' COMMENT 'RECEIVED/PROCESSING/APPROVED/REJECTED/PAID',
    rejection_reason VARCHAR(1000) COMMENT 'Ly do tu choi',
    notes VARCHAR(2000) COMMENT 'Ghi chu cua BHXH',
    received_at DATETIME COMMENT 'Ngay gio nhan',
    processed_at DATETIME COMMENT 'Ngay gio xu ly xong',
    paid_at DATETIME COMMENT 'Ngay gio thanh toan',
    INDEX idx_clinic_code (clinic_code),
    INDEX idx_cccd (cccd_number),
    INDEX idx_status (status),
    INDEX idx_received_at (received_at)
);
