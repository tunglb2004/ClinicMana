-- Mock Service Catalog - Danh mục giá trần BHYT theo Thông tư 22
-- Giá_Cơ_Sở = min(Giá_Phòng_Khám, Giá_Trần_BHYT)
-- Tiền_BHYT_Trả = Giá_Cơ_Sở × Mức_Hưởng_Thẻ × Tỷ_Lệ_Dịch_Vụ
-- Tiền_Bệnh_Nhân_Trả = Giá_Phòng_Khám − Tiền_BHYT_Trả

-- 1. Tạo bảng
CREATE TABLE IF NOT EXISTS mock_service_catalog (
    id INT AUTO_INCREMENT PRIMARY KEY,
    service_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã chuẩn Bộ Y tế',
    service_name VARCHAR(255) NOT NULL COMMENT 'Tên dịch vụ',
    ceiling_price DECIMAL(15, 2) NOT NULL COMMENT 'Giá trần BHYT',
    coverage_ratio INT DEFAULT 100 COMMENT 'Tỷ lệ thanh toán (%)',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Trạng thái hiệu lực'
);

-- 2. Dọn sạch bảng cũ để nạp bộ dữ liệu chuẩn Thông tư 22
TRUNCATE TABLE mock_service_catalog;

-- 3. Bơm 15 mã dịch vụ kinh điển nhất của Phòng khám ngoại trú
INSERT INTO mock_service_catalog (service_code, service_name, ceiling_price, coverage_ratio) 
VALUES 
-- ==========================================
-- NHÓM 1: KHÁM BỆNH LÂM SÀNG
-- ==========================================
('KHAM_LAM_SANG', 'Khám bệnh lâm sàng (Phòng khám đa khoa)', 34500, 100),

-- ==========================================
-- NHÓM 2: CHẨN ĐOÁN HÌNH ẢNH & THĂM DÒ CHỨC NĂNG
-- ==========================================
('SIEU_AM_BUNG', 'Siêu âm ổ bụng (thường)', 49300, 100),
('XQ_TIM_PHOI', 'Chụp X-quang tim phổi thẳng (1 tư thế)', 53200, 100),
('DIEN_TAM_DO', 'Điện tâm đồ (ECG)', 34900, 100),
('NOI_SOI_TMH', 'Nội soi Tai Mũi Họng ống cứng', 104000, 100),

-- ==========================================
-- NHÓM 3: XÉT NGHIỆM (LAB)
-- ==========================================
('XN_MAU_18TS', 'Tổng phân tích tế bào máu ngoại vi (Bằng máy laser)', 39300, 100),
('XN_GLUCOSE', 'Định lượng Glucose máu', 23100, 100),
('XN_AST_GOT', 'Đo hoạt độ AST (GOT) máu', 23100, 100),
('XN_ALT_GPT', 'Đo hoạt độ ALT (GPT) máu', 23100, 100),
('XN_CHOLESTEROL', 'Định lượng Cholesterol toàn phần', 23100, 100),
('XN_NUOC_TIEU', 'Tổng phân tích nước tiểu (Bằng máy tự động)', 37800, 100),

-- ==========================================
-- NHÓM 4: THỦ THUẬT NHỎ
-- ==========================================
('KHAU_VT_NONG', 'Khâu vết thương phần mềm nông < 2cm', 153000, 100),
('THAY_BANG', 'Thay băng vết thương chiều dài < 15cm', 64900, 100),
('LAY_DI_VAT_TMH', 'Lấy dị vật tai mũi họng (đơn giản)', 60000, 100),

-- ==========================================
-- NHÓM 5: CASE ĐẶC BIỆT (ĐỂ TEST BẪY LOGIC)
-- ==========================================
-- Dịch vụ kỹ thuật cao/Thuốc đặc trị: Bảo hiểm chỉ hỗ trợ 50% mức trần
('DV_KY_THUAT_CAO', 'Dịch vụ kỹ thuật cao (Cần hội chẩn)', 2000000, 50);
-- 4. Hibernate/JPA may create this table without the SQL default above.
-- Keep imported catalog rows active so BHYT lookup by is_active=true works after a DB reset.
UPDATE mock_service_catalog
SET is_active = TRUE
WHERE is_active IS NULL;
