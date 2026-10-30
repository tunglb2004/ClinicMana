# HƯỚNG DẪN LUỒNG BẢO HIỂM Y TẾ VIỆT NAM

## 📋 Tổng quan

Tài liệu này mô tả chi tiết luồng xử lý bảo hiểm y tế trong hệ thống ClinicPro, bao gồm:
- Tra cứu CCCD và xác định trái tuyến
- Tính toán mức hưởng BHYT
- Lưu trữ và gửi hồ sơ XML

---

## 🔍 BƯỚC 1: Tra cứu CCCD và xác định "Trái tuyến"

### API Endpoint
```
GET /api/citizen-insurance/lookup/cccd/{cccdNumber}
```

### Response Format
```json
{
  "cccdNumber": "030012345678",
  "insuranceCode": "GD4010112345678",
  "fullName": "NGUYEN VAN A",
  "dateOfBirth": "19990515",
  "gender": 1,
  "genderName": "Nam",
  "address": "Số 123, Đường Láng, Đống Đa, Hà Nội",
  "maDkbd": "01012",
  "benefitLevel": 1,
  "provinceCode": "40",
  "isDungTuyen": false,
  "tuyenRatio": 0,
  "originalCoverageRate": 80,
  "finalCoverageRate": 0,
  "validTo": "2026-12-31",
  "status": "ACTIVE",
  "message": "⚠️ Trái tuyến khác tỉnh - Bệnh nhân không được hưởng BHYT..."
}
```

### Logic xác định Trái tuyến

#### 1. So sánh MA_DKBD với MA_CSKCB
```java
// MA_DKBD: Nơi đăng ký ban đầu của bệnh nhân (VD: "01012")
// MA_CSKCB: Mã phòng khám hiện tại (config trong application.properties)

if (maDkbd.equals(maCskcb)) {
    return true; // Đúng tuyến
}
```

#### 2. Chính sách "Thông tuyến huyện"
```java
// Lấy 2 số đầu mã thẻ BHYT làm mã tỉnh
// VD: GD4010112345678 -> "40" là mã tỉnh

String provinceCodePatient = insuranceCode.substring(2, 4);
String provinceCodeClinic = clinicConfig.getProvinceCode();

if (provinceCodePatient.equals(provinceCodeClinic)) {
    return true; // Cùng tỉnh -> Đúng tuyến (theo chính sách thông tuyến)
}
```

#### 3. Trái tuyến khác tỉnh
```java
// Nếu không thỏa 2 điều kiện trên
return false; // Trái tuyến khác tỉnh
```

---

## 📊 BƯỚC 2: Tính toán % mức hưởng

### Công thức
```
% Thực tế = % Mức hưởng gốc × % Tuyến ÷ 100
```

### 2.1. Mức hưởng gốc (từ ký tự thứ 3 của mã thẻ)

| Ký tự thứ 3 | Đối tượng | Mức hưởng |
|-------------|-----------|-----------|
| 1, 2 | Người có công, trẻ em dưới 6 tuổi | 100% |
| 3 | Người cao tuổi | 95% |
| 4 | Người lao động | 80% |

**Ví dụ:**
- `GD1...` hoặc `TE2...` → 100%
- `GD3...` → 95%
- `GD4...` → 80%

### 2.2. Tỷ lệ tuyến

| Trường hợp | Tỷ lệ tuyến |
|------------|-------------|
| Đúng tuyến (MA_DKBD = MA_CSKCB) | 100% |
| Cùng tỉnh (Thông tuyến huyện) | 100% |
| Trái tuyến khác tỉnh (ngoại trú) | 0% |

### 2.3. Ví dụ tính toán

#### Case 1: Đúng tuyến
```
Mã thẻ: GD4010112345678 (Mức hưởng gốc: 80%)
MA_DKBD: 01012
MA_CSKCB: 01012 (cùng)

→ Đúng tuyến → Tuyên ratio: 100%
→ Mức hưởng cuối: 80% × 100% = 80%
```

#### Case 2: Trái tuyến cùng tỉnh
```
Mã thẻ: GD4010112345678 (Mức hưởng gốc: 80%)
MA_DKBD: 01020 (Khác MA_CSKCB)
Mã tỉnh: 40 (Cùng tỉnh với phòng khám)

→ Cùng tỉnh → Tuyên ratio: 100%
→ Mức hưởng cuối: 80% × 100% = 80%
```

#### Case 3: Trái tuyến khác tỉnh
```
Mã thẻ: GD7910112345678 (Mức hưởng gốc: 80%)
MA_DKBD: 79012 (Tỉnh 79 - TP.HCM)
Phòng khám: Tỉnh 01 (Hà Nội)

→ Khác tỉnh → Tuyên ratio: 0%
→ Mức hưởng cuối: 80% × 0% = 0%
→ Bệnh nhân phải trả 100% chi phí
```

---

## 💾 BƯỚC 3: Gửi file XML và lưu trữ

### 3.1. Lưu trữ tại ClinicPro (Phía phòng khám)

#### Bảng: `insurance_submissions`
```sql
CREATE TABLE insurance_submissions (
    id BIGINT PRIMARY KEY,
    submission_code VARCHAR(100) UNIQUE,  -- Mã hồ sơ
    service_order_id BIGINT,              -- ID phiếu thanh toán
    cccd_number VARCHAR(12),
    insurance_code VARCHAR(15),
    patient_name VARCHAR(255),
    ma_dkbd VARCHAR(5),
    is_dung_tuyen BOOLEAN,
    total_amount DECIMAL(15,2),           -- Tổng chi phí
    insurance_paid DECIMAL(15,2),         -- BHYT trả
    patient_paid DECIMAL(15,2),           -- Bệnh nhân trả
    file_path VARCHAR(500),               -- Đường dẫn file XML
    xml_content LONGTEXT,                 -- Nội dung XML
    status VARCHAR(20),                   -- PENDING/SENT/ACCEPTED/REJECTED
    created_at DATETIME,
    submitted_at DATETIME
);
```

#### Ví dụ lưu trữ
```java
InsuranceSubmission submission = InsuranceSubmission.builder()
    .submissionCode("SUBM_20260303_001")
    .serviceOrderId(123L)
    .cccdNumber("030012345678")
    .insuranceCode("GD4010112345678")
    .patientName("NGUYEN VAN A")
    .maDkbd("01012")
    .isDungTuyen(true)
    .totalAmount(new BigDecimal("500000"))
    .insurancePaid(new BigDecimal("400000"))  // 80% của 500k
    .patientPaid(new BigDecimal("100000"))    // 20% của 500k
    .filePath("/storage/insurance/submissions/2026-03-03/SUBM_20260303_001.xml")
    .status("PENDING")
    .build();
    
submissionRepository.save(submission);
```

### 3.2. Lưu trữ tại Mock BHXH Server

#### Bảng: `received_claims`
```sql
CREATE TABLE received_claims (
    id BIGINT PRIMARY KEY,
    submission_code VARCHAR(100) UNIQUE,
    clinic_code VARCHAR(5),               -- Mã phòng khám gửi
    cccd_number VARCHAR(12),
    insurance_code VARCHAR(15),
    patient_name VARCHAR(255),
    ma_dkbd VARCHAR(5),
    is_dung_tuyen BOOLEAN,
    total_amount DECIMAL(15,2),
    insurance_claim_amount DECIMAL(15,2), -- Yêu cầu BHYT trả
    insurance_approved_amount DECIMAL(15,2), -- BHXH duyệt
    patient_paid DECIMAL(15,2),
    xml_content LONGTEXT,
    status VARCHAR(20),                   -- RECEIVED/PROCESSING/APPROVED/PAID
    received_at DATETIME,
    processed_at DATETIME
);
```

---

## 🔄 BƯỚC 4: Luồng dữ liệu hoàn chỉnh

### Sơ đồ luồng
```
┌─────────────────────────────────────────────────────────────┐
│ 1. Lễ tân nhập CCCD                                         │
│    → API: GET /api/citizen-insurance/lookup/cccd/{cccd}    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Nhận thông tin bệnh nhân                                 │
│    - Họ tên, mã thẻ, MA_DKBD                               │
│    - Tỉnh, đúng/trái tuyến                                  │
│    - Mức hưởng: originalCoverageRate, finalCoverageRate    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Khám bệnh & tính tiền                                    │
│    - Tổng chi phí: 500,000 VNĐ                             │
│    - BHYT trả (T_BHTT): 500k × 80% = 400k                  │
│    - Bệnh nhân trả (T_BNCCT): 500k × 20% = 100k            │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Thanh toán xong → Tạo XML                                │
│    - Gọi: mapToXml1Request()                               │
│    - Lưu vào: insurance_submissions (status: PENDING)      │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Gửi XML lên Mock BHXH Server                             │
│    - API: POST /api/bhxh/claims                            │
│    - Update: insurance_submissions (status: SENT)          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. Mock Server nhận và lưu                                  │
│    - Lưu vào: received_claims (status: RECEIVED)           │
│    - Xử lý & duyệt (status: APPROVED)                      │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. Đối soát & thanh toán                                    │
│    - BHXH chuyển tiền cho phòng khám                       │
│    - Update cả 2 bảng: status = PAID                       │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚙️ Cấu hình hệ thống

### application.properties
```properties
# Cấu hình thông tin phòng khám
clinic.insurance.ma-cskcb=01012
clinic.insurance.name=Phong kham Da khoa ABC
clinic.insurance.province-code=01
```

### Giải thích
- `ma-cskcb`: Mã cơ sở khám chữa bệnh (5 ký tự)
- `province-code`: Mã tỉnh (2 số) để xác định trái tuyến

---

## 🧪 Test Cases

### Test Case 1: Đúng tuyến
```
Input: CCCD = 030012345678
Expected:
- insuranceCode: GD4010112345678
- maDkbd: 01012 (= MA_CSKCB)
- isDungTuyen: true
- originalCoverageRate: 80%
- finalCoverageRate: 80%
```

### Test Case 2: Trái tuyến cùng tỉnh
```
Input: CCCD = 030088887777
Expected:
- insuranceCode: GD4010188887777
- maDkbd: 01001 (≠ MA_CSKCB nhưng cùng tỉnh 01)
- isDungTuyen: false
- tuyenRatio: 100% (thông tuyến huyện)
- finalCoverageRate: 95%
```

### Test Case 3: Trái tuyến khác tỉnh
```
Input: CCCD với mã thẻ GD7910112345678
Expected:
- provinceCode: 79 (TP.HCM)
- isDungTuyen: false
- tuyenRatio: 0%
- finalCoverageRate: 0%
- message: "Trái tuyến khác tỉnh - Không được hưởng BHYT"
```

---

## 📝 Ghi chú quan trọng

### 1. Về Trái tuyến
- **Đúng tuyến**: MA_DKBD = MA_CSKCB
- **Thông tuyến huyện**: Cùng tỉnh → vẫn hưởng 100%
- **Trái tuyến tỉnh**: Khác tỉnh → 0% (nếu không có giấy chuyển viện)

### 2. Về lưu trữ XML
- **Cách 1**: Lưu file vào disk, chỉ lưu đường dẫn vào DB
- **Cách 2**: Lưu nội dung XML vào LONGTEXT trong DB
- **Khuyến nghị**: Kết hợp cả 2 cách để dễ backup và truy vấn

### 3. Về đối soát
- Phòng khám xem: `insurance_submissions`
- BHXH xem: `received_claims`
- So sánh: `submission_code` phải khớp nhau

---

## 🔗 Files liên quan

- Entity: [MockCitizenData.java](../src/main/java/g2/be/entity/MockCitizenData.java)
- Service: [CitizenInsuranceService.java](../src/main/java/g2/be/service/CitizenInsuranceService.java)
- Response DTO: [CitizenInsuranceInfoResponse.java](../src/main/java/g2/be/dto/response/CitizenInsuranceInfoResponse.java)
- Config: [ClinicInsuranceConfig.java](../src/main/java/g2/be/config/ClinicInsuranceConfig.java)
- Submission Entity: [InsuranceSubmission.java](../src/main/java/g2/be/entity/InsuranceSubmission.java)
- Claim Entity: [ReceivedClaim.java](../src/main/java/g2/be/entity/ReceivedClaim.java)
- Database: [mock_citizen_data.sql](../src/main/resources/db/migration/mock_citizen_data.sql)

---

**Tạo ngày**: 2026-03-03  
**Version**: 1.0
