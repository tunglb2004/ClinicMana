# Test Cases - API Tra cứu CCCD và Trái tuyến

## 🧪 Test Case 1: Đúng tuyến - Hưởng 80%

### Request
```bash
GET http://localhost:8080/api/citizen-insurance/lookup/cccd/030012345678
```

### Expected Response
```json
{
  "cccdNumber": "030012345678",
  "insuranceCode": "GD4010112345678",
  "fullName": "NGUYEN VAN A",
  "dateOfBirth": "19990515",
  "gender": 1,
  "genderName": "Nam",
  "address": "Số 123, Đường Láng, Đống Đa, Hà Nội",
  "coverageRate": 80,
  "maDkbd": "01012",
  "benefitLevel": 1,
  "provinceCode": "40",
  "isDungTuyen": true,
  "tuyenRatio": 100,
  "originalCoverageRate": 80,
  "finalCoverageRate": 80,
  "validTo": "2026-12-31",
  "status": "ACTIVE",
  "message": "✅ Đúng tuyến - Bệnh nhân được hưởng 80% chi phí BHYT."
}
```

### Giải thích
- Mã thẻ: `GD4010112345678`
  - Ký tự thứ 3 = `4` → Mức hưởng gốc: **80%**
  - Mã tỉnh = `40`
- MA_DKBD: `01012` = MA_CSKCB → **Đúng tuyến**
- Tỷ lệ tuyến: **100%**
- **Kết quả: 80% × 100% = 80%**

---

## 🧪 Test Case 2: Trẻ em - Hưởng 100%

### Request
```bash
GET http://localhost:8080/api/citizen-insurance/lookup/cccd/030099999999
```

### Expected Response
```json
{
  "cccdNumber": "030099999999",
  "insuranceCode": "TE1010199999999",
  "fullName": "TRAN THI B",
  "dateOfBirth": "20200101",
  "gender": 2,
  "genderName": "Nữ",
  "address": "Số 456, Phố Huế, Hai Bà Trưng, Hà Nội",
  "coverageRate": 100,
  "maDkbd": "01015",
  "benefitLevel": 4,
  "provinceCode": "10",
  "isDungTuyen": true,
  "tuyenRatio": 100,
  "originalCoverageRate": 100,
  "finalCoverageRate": 100,
  "validTo": "2026-12-31",
  "status": "ACTIVE",
  "message": "✅ Đúng tuyến - Bệnh nhân được hưởng 100% chi phí BHYT."
}
```

### Giải thích
- Mã thẻ: `TE1010199999999`
  - Ký tự thứ 3 = `1` → Mức hưởng gốc: **100%** (Trẻ em)
  - Mã tỉnh = `10`
- MA_DKBD: `01015` ≠ MA_CSKCB (01012)
- Nhưng cùng tỉnh `01` → **Thông tuyến**
- Tỷ lệ tuyến: **100%**
- **Kết quả: 100% × 100% = 100%**

---

## 🧪 Test Case 3: Người cao tuổi - Hưởng 95%

### Request
```bash
GET http://localhost:8080/api/citizen-insurance/lookup/cccd/030088887777
```

### Expected Response
```json
{
  "cccdNumber": "030088887777",
  "insuranceCode": "GD4010188887777",
  "fullName": "PHAM THI D",
  "dateOfBirth": "19601225",
  "gender": 2,
  "genderName": "Nữ",
  "address": "Số 321, Kim Mã, Ba Đình, Hà Nội",
  "coverageRate": 95,
  "maDkbd": "01001",
  "benefitLevel": 3,
  "provinceCode": "40",
  "isDungTuyen": false,
  "tuyenRatio": 100,
  "originalCoverageRate": 95,
  "finalCoverageRate": 95,
  "validTo": "2026-12-31",
  "status": "ACTIVE",
  "message": "⚠️ Thông tuyến cùng tỉnh - Bệnh nhân được hưởng 95% chi phí BHYT."
}
```

### Giải thích
- Mã thẻ: `GD4010188887777`
  - Ký tự thứ 3 = `4` → Nhưng trong DB set `benefitLevel = 3` → **95%**
  - Mã tỉnh = `40`
- MA_DKBD: `01001` ≠ MA_CSKCB → **Không đúng tuyến**
- Nhưng cùng tỉnh `01` (phòng khám cũng tỉnh 01) → **Thông tuyến**
- Tỷ lệ tuyến: **100%**
- **Kết quả: 95% × 100% = 95%**

---

## 🧪 Test Case 4: Thẻ hết hạn

### Request
```bash
GET http://localhost:8080/api/citizen-insurance/lookup/cccd/030011112222
```

### Expected Response
```json
{
  "cccdNumber": "030011112222",
  "insuranceCode": "DN4010111112222",
  "fullName": "LE VAN C",
  "dateOfBirth": "19851010",
  "gender": 1,
  "genderName": "Nam",
  "address": "Số 789, Giải Phóng, Hoàng Mai, Hà Nội",
  "coverageRate": 80,
  "maDkbd": "01020",
  "benefitLevel": 2,
  "provinceCode": "40",
  "isDungTuyen": false,
  "tuyenRatio": 100,
  "originalCoverageRate": 80,
  "finalCoverageRate": 80,
  "validTo": "2023-01-01",
  "status": "EXPIRED",
  "message": "⚠️ Thẻ BHYT đã hết hạn. Vui lòng gia hạn thẻ trước khi khám."
}
```

### Giải thích
- `validTo`: 2023-01-01 < Ngày hiện tại
- **Status: EXPIRED**
- Hệ thống vẫn tính toán mức hưởng nhưng cảnh báo không được sử dụng

---

## 🧪 Test Case 5: Trái tuyến khác tỉnh - Không hưởng (0%)

### Tạo data test (thêm vào mock_citizen_data.sql)
```sql
-- Test case: Trái tuyến khác tỉnh
INSERT INTO mock_citizen_data (cccd_number, insurance_code, full_name, date_of_birth, gender, address, ma_dkbd, benefit_level, coverage_rate, valid_to) 
VALUES ('079088888888', 'GD7910188888888', 'NGUYEN VAN TRAI TUYEN', '19900101', 1, 'TP. Hồ Chí Minh', '79012', 1, 80, '2026-12-31')
ON DUPLICATE KEY UPDATE cccd_number = cccd_number;
```

### Request
```bash
GET http://localhost:8080/api/citizen-insurance/lookup/cccd/079088888888
```

### Expected Response
```json
{
  "cccdNumber": "079088888888",
  "insuranceCode": "GD7910188888888",
  "fullName": "NGUYEN VAN TRAI TUYEN",
  "dateOfBirth": "19900101",
  "gender": 1,
  "genderName": "Nam",
  "address": "TP. Hồ Chí Minh",
  "coverageRate": 80,
  "maDkbd": "79012",
  "benefitLevel": 1,
  "provinceCode": "79",
  "isDungTuyen": false,
  "tuyenRatio": 0,
  "originalCoverageRate": 80,
  "finalCoverageRate": 0,
  "validTo": "2026-12-31",
  "status": "ACTIVE",
  "message": "⚠️ Trái tuyến khác tỉnh - Bệnh nhân không được hưởng BHYT. Cần giấy chuyển viện hoặc thanh toán 100% chi phí."
}
```

### Giải thích
- Mã thẻ: `GD7910188888888`
  - Mã tỉnh = `79` (TP. Hồ Chí Minh)
- MA_DKBD: `79012` (Tỉnh 79)
- Phòng khám: Tỉnh `01` (Hà Nội)
- **Khác tỉnh** → Trái tuyến
- Tỷ lệ tuyến: **0%**
- **Kết quả: 80% × 0% = 0%**
- Bệnh nhân phải trả 100% chi phí

---

## 📊 Bảng tổng hợp kết quả

| CCCD | Mã thẻ | Mã tỉnh | MA_DKBD | Đúng tuyến? | Gốc | Tuyến | Cuối |
|------|--------|---------|---------|-------------|-----|-------|------|
| 030012345678 | GD4010112345678 | 40 | 01012 | ✅ Đúng | 80% | 100% | **80%** |
| 030099999999 | TE1010199999999 | 10 | 01015 | ⚠️ Thông | 100% | 100% | **100%** |
| 030088887777 | GD4010188887777 | 40 | 01001 | ⚠️ Thông | 95% | 100% | **95%** |
| 030011112222 | DN4010111112222 | 40 | 01020 | ❌ Hết hạn | 80% | 100% | **0%** |
| 079088888888 | GD7910188888888 | 79 | 79012 | ❌ Trái | 80% | 0% | **0%** |

---

## 🔧 Postman Collection

### Import vào Postman
```json
{
  "info": {
    "name": "Insurance Lookup API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Lookup CCCD - Đúng tuyến",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/citizen-insurance/lookup/cccd/030012345678",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "citizen-insurance", "lookup", "cccd", "030012345678"]
        }
      }
    },
    {
      "name": "Lookup CCCD - Trẻ em 100%",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/citizen-insurance/lookup/cccd/030099999999",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "citizen-insurance", "lookup", "cccd", "030099999999"]
        }
      }
    },
    {
      "name": "Lookup CCCD - Trái tuyến khác tỉnh",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/citizen-insurance/lookup/cccd/079088888888",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "citizen-insurance", "lookup", "cccd", "079088888888"]
        }
      }
    }
  ]
}
```

---

## ✅ Checklist Testing

- [ ] Test Case 1: Đúng tuyến 80% - PASS
- [ ] Test Case 2: Trẻ em 100% - PASS
- [ ] Test Case 3: Người cao tuổi 95% - PASS
- [ ] Test Case 4: Thẻ hết hạn - PASS
- [ ] Test Case 5: Trái tuyến khác tỉnh 0% - PASS
- [ ] Kiểm tra logic thông tuyến huyện
- [ ] Kiểm tra message hiển thị đúng
- [ ] Kiểm tra API trả về đầy đủ trường

**Last updated**: 2026-03-03
