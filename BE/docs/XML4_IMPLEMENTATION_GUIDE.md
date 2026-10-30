# 📋 Hướng dẫn triển khai XML4 - Bảng 4 (Kết quả xét nghiệm CLS)

## 🎯 Mục đích

Bảng 4 lưu trữ **kết quả xét nghiệm cận lâm sàng** (lab tests, imaging, diagnostic procedures) để cung cấp "bằng chứng lâm sàng" cho chẩn đoán trong XML1.

> **Ví dụ thực tế:**  
> - Chẩn đoán "Đái tháo đường type 2" (ICD: E11.9) trong XML1  
> - **PHẢI** có kết quả xét nghiệm đường huyết cao trong XML4  
> - BHXH sẽ từ chối thanh toán nếu không có bằng chứng

---

## 📊 Cấu trúc hệ thống

### 1. **ServiceOrder Entity** (Lưu trữ DB - Đã có sẵn)
```
service_orders table
├── order_id (PK)
├── record_id (FK → medical_records)
├── service_id (FK → services)
├── technician_id
├── result_note (TEXT - kết quả xét nghiệm)
├── result_file_url (link file ảnh/PDF nếu có)
├── status (PENDING/COMPLETED)
└── created_at
```

**Đặc điểm quan trọng:**
- `result_note` cho phép NULL → hỗ trợ quy định bổ sung kết quả trong 30 ngày
- `status` = "PENDING" → chưa có kết quả
- `status` = "COMPLETED" → đã có kết quả
- Quan hệ `@ManyToOne` với `MedicalRecord` để liên kết data
- Quan hệ `@ManyToOne` với `Service` để lấy tên dịch vụ + giá

---

### 2. **Xml4ParaclinicalRequest DTO** (12 trường QĐ 130)
```java
@Builder
@Data
public class Xml4ParaclinicalRequest {
    private Integer stt;                  // [1] Số thứ tự
    private String maLk;                  // [2] Mã liên kết (= XML1.MA_LK)
    private String maDichVu;              // [3] Mã dịch vụ (QĐ 7603)
    private BigDecimal donGia;            // [4] Đơn giá
    private BigDecimal soLuong;           // [5] Số lượng
    private BigDecimal thanhTien;         // [6] Thành tiền
    private LocalDateTime thoiGianChiDinh; // [7] Thời gian chỉ định
    private String maBsChiDinh;           // [8] Mã BS chỉ định
    private String maChiSo;               // [9] Mã chỉ số
    private String tenChiSo;              // [10] Tên chỉ số
    private String giaTri;                // [11] Giá trị (NULLABLE)
    private String ngayKq;                // [12] Ngày kết quả (NULLABLE)
    private String donViDo;               // [13] Đơn vị đo
}
```

**Data Honesty:**
- `giaTri` = null → xét nghiệm chưa có kết quả
- `ngayKq` = null → xét nghiệm chưa hoàn thành
- **Không fake data** → BHXH sẽ hiểu clinic sẽ bổ sung trong 30 ngày

---

### 3. **Xml4ParaclinicalMapper Service**
```java
@Service
public class Xml4ParaclinicalMapper {
    
    // Chuyển đổi danh sách ServiceOrder → List<Xml4ParaclinicalRequest>
    public List<Xml4ParaclinicalRequest> mapToXml4List(
        List<ServiceOrder> serviceOrders, 
        String maLk
    );
    
    // Chuyển đổi 1 ServiceOrder → 1 Xml4ParaclinicalRequest
    public Xml4ParaclinicalRequest mapToXml4(ServiceOrder order, String maLk);
    
    // Gán số thứ tự (STT) cho danh sách
    public void assignStt(List<Xml4ParaclinicalRequest> xml4List);
}
```

**Logic mapping chính:**
- `serviceOrder.resultNote` → `xml4.giaTri` (giữ nguyên null nếu chưa có)
- `serviceOrder.createdAt` → `xml4.ngayKq` (format: `yyyyMMddHHmm`, chỉ khi status = COMPLETED)
- `service.price` → `xml4.donGia` (giá dịch vụ)
- `service.serviceName` → `xml4.tenChiSo` (tên dịch vụ)

---

### 4. **CombinedXmlResponse** (Wrapper)
```java
@Data
@Builder
public class CombinedXmlResponse {
    private Xml1MedicalRecordRequest xml1;
    private List<Xml4ParaclinicalRequest> xml4;
    private Xml4Metadata metadata;
    
    @Data
    @Builder
    public static class Xml4Metadata {
        private int totalTests;
        private long completedTests;
        private long pendingTests;
        private String statusMessage;
    }
}
```

**Giá trị:**
- Frontend nhận **cả XML1 + XML4 + metadata** trong 1 API call
- Hiển thị warning nếu còn xét nghiệm chưa có kết quả

---

## 🔗 API Endpoints

### 1. **GET** `/api/xml-mapper/test/{invoiceId}`
Lấy dữ liệu XML1 + XML4 để test/preview
```json
{
  "xml1": { ... 65 fields ... },
  "xml4": [
    {
      "stt": 1,
      "maLk": "2024011500001",
      "maDichVu": "09.0319",
      "maChiSo": "BS0001",
      "tenChiSo": "Glucose máu lúc đói",
      "giaTri": "8.5",
      "ngayKq": "202401150830",
      "donViDo": "mmol/L"
    },
    {
      "stt": 2,
      "maLk": "2024011500001",
      "maDichVu": "09.0250",
      "maChiSo": "SPUTUM_CULTURE",
      "tenChiSo": "Nuôi cấy đờm",
      "giaTri": null,
      "ngayKq": null,
      "donViDo": "N/A"
    }
  ],
  "metadata": {
    "totalTests": 2,
    "completedTests": 1,
    "pendingTests": 1,
    "statusMessage": "⚠️ Có 1 xét nghiệm chưa có kết quả. Vui lòng bổ sung trong 30 ngày."
  }
}
```

---

### 2. **POST** `/api/xml-mapper/send-to-bhxh/{invoiceId}`
Giả lập gửi sang API BHXH (mock)
```
POST /api/xml-mapper/send-to-bhxh/INV001

Response:
{
  "success": true,
  "message": "Gửi thành công tới Mock API BHXH!",
  "data": "✅ BHXH đã tiếp nhận hồ sơ\n━━━━━━━━━━━━━━━━━━━━━━━━━\n📋 THÔNG TIN TỔNG HỢP\n- Mã đợt điều trị: 2024011500001\n- Bệnh nhân: Nguyễn Văn A\n- Tổng chi phí: 500000 VNĐ\n...\n🧪 KẾT QUẢ XÉT NGHIỆM\n- Tổng số xét nghiệm: 2\n- Đã có kết quả: 1\n- Chờ kết quả: 1\n━━━━━━━━━━━━━━━━━━━━━━━━━\n📊 TRẠNG THÁI\n⚠️ Đã tiếp nhận. Vui lòng bổ sung kết quả còn thiếu trong 30 ngày.",
  "note": "Trong Production, sẽ gọi thật sang API của BHXH Việt Nam"
}
```

---

## 📦 Dữ liệu mẫu

File: `service_orders_sample_data.sql`

**Kịch bản 1: Bệnh nhân đái tháo đường (MR001)**
- 2 xét nghiệm máu đã hoàn thành (COMPLETED)
- Glucose: 8.5 mmol/L (cao)
- HbA1c: 8.2% (cao)

**Kịch bản 2: Bệnh nhân viêm phổi (MR002)**
- 3 dịch vụ
- Công thức máu: WBC cao (COMPLETED) ✅
- X-quang ngực: có đám mờ (COMPLETED) ✅
- Nuôi cấy đờm: **chưa có kết quả** (PENDING) ⏳

**Kịch bản 3: Khám định kỳ (MR003)**
- 3 dịch vụ
- Công thức máu: bình thường (COMPLETED) ✅
- Cholesterol: cao ranh giới (COMPLETED) ✅
- Siêu âm bụng: **bệnh nhân chưa thực hiện** (PENDING) ⏳

---

## ⚙️ Cách chạy test

### Bước 1: Import dữ liệu mẫu
```bash
# Chạy SQL file
mysql -u root -p clinicpro_db < service_orders_sample_data.sql
```

### Bước 2: Kiểm tra data
```bash
# Trong MySQL
USE clinicpro_db;
SELECT * FROM services;
SELECT * FROM service_orders;
```

### Bước 3: Test API
```bash
# GET - Lấy XML1 + XML4
GET http://localhost:8080/api/xml-mapper/test/INV001

# POST - Giả lập gửi BHXH
POST http://localhost:8080/api/xml-mapper/send-to-bhxh/INV001
```

---

## 🎓 Các câu hỏi thường gặp

### Q1: Tại sao `giaTri` và `ngayKq` được phép NULL?
**A:** Theo QĐ 130/QĐ-BYT, phòng khám có quyền bổ sung kết quả xét nghiệm trong **30 ngày** kể từ ngày thanh toán. Điều này phù hợp với mô hình "thanh toán trước, xét nghiệm sau" của phòng khám.

Trong hệ thống: Khi `ServiceOrder.status = "PENDING"` → `result_note = NULL` → XML4 `giaTri = NULL`.

---

### Q2: BHXH có từ chối thanh toán nếu XML4 còn giá trị NULL không?
**A:** **Không từ chối ngay.** BHXH sẽ:
1. Tiếp nhận hồ sơ
2. Đặt trạng thái "Chờ bổ sung"
3. Yêu cầu clinic gửi lại XML4 đầy đủ trong 30 ngày

Nếu quá 30 ngày không bổ sung → BHXH có quyền từ chối hoặc yêu cầu giải trình.

---

### Q3: Tại sao không tạo bảng test_results mới mà dùng service_orders?
**A:** Vì:
- **service_orders đã lưu đủ thông tin** cần thiết cho XML4
- Tránh trùng lặp data (1 ServiceOrder = 1 test result)
- ServiceOrder có sẵn quan hệ với Service (có giá, tên dịch vụ)
- Tiết kiệm công sức maintain 2 bảng giống nhau

**So sánh:**
| Trường XML4 | ServiceOrder | TestResult (không cần) |
|-------------|--------------|------------------------|
| MA_DICH_VU | service.serviceId | ✅ |
| DON_GIA | service.price | ❌ (phải tích hợp thêm) |
| TEN_CHI_SO | service.serviceName | ✅ |
| GIA_TRI | resultNote | resultValue |
| NGAY_KQ | createdAt (nếu COMPLETED) | resultDate |

→ ServiceOrder đã đủ, không cần TestResult.

---

### Q4: Làm sao phân biệt dịch vụ nào thuộc MR nào?
**A:** Dựa vào foreign key `record_id` trong bảng `service_orders`.

```sql
-- Lấy tất cả dịch vụ của MR001
SELECT * FROM service_orders WHERE record_id = 'MR001';
```

Trong code:
```java
List<ServiceOrder> orders = serviceOrderRepository
    .findByMedicalRecord_RecordId("MR001");
```

---

### Q5: Có cần tạo XML2, XML3 không?
**A:** Tùy thuộc vào yêu cầu thầy/graduation committee:
- **XML2** (Bảng 2): Chi tiết thuốc
- **XML3** (Bảng 3): Chi tiết vật tư y tế / dịch vụ khác
- **XML5** (Bảng 5): Diễn biến lâm sàng

**Đề xuất:**
- API thật của BHXH thường yêu cầu **ít nhất XML1 + XML4**
- Nếu phòng khám **không bán thuốc** → không cần XML2
- XML4 đã cover dịch vụ cận lâm sàng (lab tests + imaging) → đủ cho demo

---

## 🔄 Workflow hoàn chỉnh

```
1. Bệnh nhân đến phòng khám
   ↓
2. Bác sĩ khám → Tạo MedicalRecord
   ↓
3. Bác sĩ chỉ định xét nghiệm
   → Tạo ServiceOrder (status = PENDING, result_note = NULL)
   ↓
4. Bệnh nhân thanh toán
   → Tạo Invoice (status = PAID)
   ↓
5. Gửi XML1 + XML4 (chưa đầy đủ) sang BHXH
   → BHXH tiếp nhận, trạng thái "Chờ bổ sung"
   ↓
6. Bệnh nhân làm xét nghiệm (trong 1-3 ngày)
   ↓
7. Lab trả kết quả
   → UPDATE service_orders 
      SET status = 'COMPLETED', 
          result_note = 'Glucose: 8.5 mmol/L'
   ↓
8. Gửi lại XML4 (đầy đủ) sang BHXH
   → BHXH duyệt tự động → Chuyển tiền
```

---

## 📚 Các file liên quan

| File | Mô tả |
|------|-------|
| [ServiceOrder.java](../src/main/java/g2/be/entity/ServiceOrder.java) | Entity lưu trữ dịch vụ đã thực hiện |
| [Service.java](../src/main/java/g2/be/entity/Service.java) | Entity danh mục dịch vụ |
| [ServiceOrderRepository.java](../src/main/java/g2/be/repository/ServiceOrderRepository.java) | JPA Repository |
| [Xml4ParaclinicalRequest.java](../src/main/java/g2/be/dto/request/Xml4ParaclinicalRequest.java) | DTO 12 trường |
| [Xml4ParaclinicalMapper.java](../src/main/java/g2/be/service/Xml4ParaclinicalMapper.java) | Mapper service |
| [Xml1MapperController.java](../src/main/java/g2/be/controller/Xml1MapperController.java) | API endpoints |
| [service_orders_sample_data.sql](../service_orders_sample_data.sql) | Dữ liệu mẫu |

---

## ✅ Checklist hoàn thành

- [x] Sử dụng ServiceOrder entity (đã có sẵn)
- [x] Tạo ServiceOrderRepository (đã có sẵn)
- [x] Tạo `Xml4ParaclinicalRequest` DTO (12 fields)
- [x] Tạo `Xml4ParaclinicalMapper` service
- [x] Tạo `CombinedXmlResponse` wrapper
- [x] Cập nhật controller để hỗ trợ XML4
- [x] Tạo SQL seed data với 3 kịch bản
- [x] Viết tài liệu hướng dẫn
- [ ] (Tùy chọn) Tạo API cập nhật kết quả xét nghiệm
- [ ] (Tùy chọn) Tạo XML2 (thuốc) và XML3 (dịch vụ khác)

---

## 🚀 Tổng kết

Hệ thống XML4 hiện tại đã:
- ✅ Tuân thủ QĐ 130/QĐ-BYT (12 trường bắt buộc)
- ✅ Hỗ trợ quy định bổ sung kết quả trong 30 ngày
- ✅ Áp dụng "Data Honesty" (null thay vì fake data)
- ✅ Tích hợp liền mạch với XML1
- ✅ Cung cấp metadata để frontend hiển thị warning
- ✅ **Tái sử dụng ServiceOrder có sẵn** (không tạo thừa bảng mới) ⭐

**Sẵn sàng cho demo/presentation graduation project! 🎓**
