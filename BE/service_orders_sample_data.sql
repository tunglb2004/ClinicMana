-- ============================================
-- SEED DATA FOR services AND service_orders
-- Dữ liệu mẫu cho Bảng 4 (XML4) - Dịch vụ cận lâm sàng
-- ============================================
-- Giả sử các MedicalRecord sau đã tồn tại:
--   - MR001: Bệnh nhân đái tháo đường
--   - MR002: Bệnh nhân viêm phổi
--   - MR003: Bệnh nhân khám định kỳ
-- ============================================

-- ============================================
-- BẢNG DANH MỤC DỊCH VỤ (services)
-- Theo Quyết định 7603/QĐ-BYT
-- ============================================
INSERT INTO services (service_id, service_name, price, description, is_active) VALUES
('XN001', 'Xét nghiệm đường huyết lúc đói', 50000, 'Đo nồng độ glucose trong máu khi đói', TRUE),
('XN002', 'Xét nghiệm HbA1c', 150000, 'Đo hemoglobin đường hóa (đánh giá kiểm soát đường huyết 3 tháng)', TRUE),
('XN003', 'Xét nghiệm công thức máu', 80000, 'Đếm số lượng hồng cầu, bạch cầu, tiểu cầu', TRUE),
('XN004', 'Xét nghiệm Cholesterol toàn phần', 60000, 'Đo nồng độ cholesterol trong máu', TRUE),
('XN005', 'Nuôi cấy đờm tìm vi khuẩn', 200000, 'Xác định loại vi khuẩn gây nhiễm trùng đường hô hấp', TRUE),
('HA001', 'X-quang ngực thẳng', 120000, 'Chụp X-quang phổi 1 tư thế', TRUE),
('HA002', 'Siêu âm bụng tổng quát', 250000, 'Siêu âm gan, mật, lách, thận, tụy', TRUE);

-- ============================================
-- MR001: Bệnh nhân đái tháo đường
-- 2 xét nghiệm đã hoàn thành
-- ============================================
INSERT INTO service_orders (order_id, record_id, service_id, technician_id, result_note, result_file_url, status, created_at)
VALUES
('SO001', 'MR001', 'XN001', 'T001', 
 'Glucose: 8.5 mmol/L (Cao - Bình thường: 3.9-6.1)', 
 NULL, 
 'COMPLETED', 
 '2024-01-15 08:00:00'),

('SO002', 'MR001', 'XN002', 'T001', 
 'HbA1c: 8.2% (Cao - Bình thường: <5.7%)', 
 NULL, 
 'COMPLETED', 
 '2024-01-15 08:00:00');

-- ============================================
-- MR002: Bệnh nhân viêm phổi
-- 2 hoàn thành + 1 chờ kết quả
-- ============================================
INSERT INTO service_orders (order_id, record_id, service_id, technician_id, result_note, result_file_url, status, created_at)
VALUES
('SO003', 'MR002', 'XN003', 'T001', 
 'WBC: 15.2 x10^9/L (Cao - Nghi ngờ nhiễm trùng)', 
 NULL, 
 'COMPLETED', 
 '2024-01-16 09:00:00'),

('SO004', 'MR002', 'HA001', 'T002', 
 'Có đám mờ phổi thùy dưới phải. Kết luận: Viêm phổi thùy dưới phải', 
 'https://storage.clinic.vn/xray/MR002_20240116.jpg', 
 'COMPLETED', 
 '2024-01-16 10:00:00'),

('SO005', 'MR002', 'XN005', 'T001', 
 NULL, -- Chưa có kết quả
 NULL, 
 'PENDING', 
 '2024-01-16 09:00:00');

-- ============================================
-- MR003: Bệnh nhân khám định kỳ
-- 2 hoàn thành + 1 chưa thực hiện
-- ============================================
INSERT INTO service_orders (order_id, record_id, service_id, technician_id, result_note, result_file_url, status, created_at)
VALUES
('SO006', 'MR003', 'XN003', 'T001', 
 'HGB: 13.5 g/dL (Bình thường)', 
 NULL, 
 'COMPLETED', 
 '2024-01-17 08:30:00'),

('SO007', 'MR003', 'XN004', 'T001', 
 'Cholesterol: 5.8 mmol/L (Cao ranh giới - Khuyên ăn kiêng)', 
 NULL, 
 'COMPLETED', 
 '2024-01-17 08:30:00'),

('SO008', 'MR003', 'HA002', 'T002', 
 NULL, -- Bệnh nhân chưa đến làm
 NULL, 
 'PENDING', 
 '2024-01-17 08:00:00');

-- ============================================
-- TỔNG KẾT DỮ LIỆU MẪU
-- ============================================
-- Tổng services: 7 loại
-- Tổng service_orders: 8
-- 
-- MR001: 2/2 hoàn thành (100%)
-- MR002: 2/3 hoàn thành (67%)
-- MR003: 2/3 hoàn thành (67%)
-- 
-- Status:
-- - COMPLETED: 6 orders
-- - PENDING: 2 orders
-- ============================================
