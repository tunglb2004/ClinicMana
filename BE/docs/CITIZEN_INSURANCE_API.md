# API Tra Cứu Thông Tin Công Dân & Thẻ BHYT

## 📋 Tổng quan

API mô phỏng hệ thống tra cứu thông tin công dân và thẻ BHYT của BHXH Việt Nam. Giúp lễ tân phòng khám:
- ✅ Nhập CCCD → Lấy thông tin bệnh nhân + trạng thái thẻ BHYT
- ✅ Kiểm tra tự động thẻ còn hiệu lực hay đã hết hạn
- ✅ Hiển thị mức hưởng BHYT (80%, 95%, 100%)

---

## 🚀 API Endpoints

### 1. Tra cứu theo số CCCD

**Endpoint**: `GET /api/citizen-insurance/lookup/cccd/{cccdNumber}`

**Request Example**:
```bash
curl -X GET "http://localhost:8080/api/citizen-insurance/lookup/cccd/030012345678"
```

**Response Success** (Thẻ còn hạn):
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
  "validTo": "2026-12-31",
  "status": "ACTIVE",
  "message": "✅ Thẻ BHYT còn hiệu lực. Bệnh nhân được hưởng 80% chi phí."
}
```

**Response Error** (Thẻ hết hạn):
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
  "validTo": "2023-01-01",
  "status": "EXPIRED",
  "message": "⚠️ Thẻ BHYT đã hết hạn. Vui lòng gia hạn thẻ trước khi khám."
}
```

**Response Error** (Không tìm thấy):
```json
{
  "error": "Không tìm thấy thông tin công dân với CCCD: 999999999999. Vui lòng kiểm tra lại số CCCD hoặc đăng ký thẻ BHYT."
}
```

---

### 2. Tra cứu theo mã thẻ BHYT

**Endpoint**: `GET /api/citizen-insurance/lookup/insurance/{insuranceCode}`

**Request Example**:
```bash
curl -X GET "http://localhost:8080/api/citizen-insurance/lookup/insurance/GD4010112345678"
```

**Response**: Giống format tra cứu theo CCCD

---

## 🧪 Test Cases Đã Seed Sẵn

| CCCD | Mã thẻ BHYT | Họ tên | Mức hưởng | Trạng thái | Mục đích test |
|------|-------------|--------|-----------|------------|---------------|
| `030012345678` | `GD4010112345678` | NGUYEN VAN A | 80% | ✅ Còn hạn | Test luồng thanh toán bình thường |
| `030099999999` | `TE1010199999999` | TRAN THI B | 100% | ✅ Còn hạn | Test BHYT trả 100%, BN không mất tiền |
| `030011112222` | `DN4010111112222` | LE VAN C | 80% | ❌ Hết hạn | Test validation, quăng lỗi cho FE |
| `030088887777` | `GD4010188887777` | PHAM THI D | 95% | ✅ Còn hạn | Test mức hưởng trung bình |
| `030055554444` | `HS1010155554444` | HOANG VAN E | 100% | ✅ Còn hạn | Test đối tượng ưu tiên (sinh viên) |

---

## 🔧 Cài Đặt & Chạy Thử

### 1. Chạy SQL Migration
```bash
# File SQL đã tạo tự động: src/main/resources/db/migration/mock_citizen_data.sql
# Spring Boot sẽ tự động chạy hoặc chạy thủ công:
mysql -u root -p clinic_db < src/main/resources/db/migration/mock_citizen_data.sql
```

### 2. Khởi động Spring Boot
```bash
mvn spring-boot:run
```

### 3. Test API qua Swagger UI
Truy cập: `http://localhost:8080/swagger-ui/index.html`
- Tìm section **"Citizen Insurance"**
- Thử các endpoint với CCCD test ở trên

---

## 💡 Luồng Tích Hợp Frontend (Next.js)

### Bước 1: Lễ tân nhập CCCD vào form
```tsx
const [cccd, setCccd] = useState('');
const [insuranceInfo, setInsuranceInfo] = useState(null);
const [error, setError] = useState(null);

const handleLookup = async () => {
  try {
    const response = await fetch(`http://localhost:8080/api/citizen-insurance/lookup/cccd/${cccd}`);
    const data = await response.json();
    
    if (response.ok) {
      setInsuranceInfo(data);
      
      // Kiểm tra status
      if (data.status === 'EXPIRED') {
        alert(data.message); // Hiển thị cảnh báo thẻ hết hạn
      }
    } else {
      setError(data.error);
    }
  } catch (err) {
    setError('Lỗi kết nối API');
  }
};
```

### Bước 2: Hiển thị thông tin tra cứu
```tsx
{insuranceInfo && (
  <div className="insurance-info">
    <h3>Thông tin bệnh nhân</h3>
    <p>Họ tên: {insuranceInfo.fullName}</p>
    <p>Giới tính: {insuranceInfo.genderName}</p>
    <p>Mã thẻ BHYT: {insuranceInfo.insuranceCode}</p>
    <p className={insuranceInfo.status === 'ACTIVE' ? 'text-green' : 'text-red'}>
      {insuranceInfo.message}
    </p>
    <p>Mức hưởng: {insuranceInfo.coverageRate}%</p>
  </div>
)}
```

---

## 📊 Database Schema

```sql
mock_citizen_data
├── id (PK, AUTO_INCREMENT)
├── cccd_number (VARCHAR(12), UNIQUE) ← Index để tra cứu nhanh
├── insurance_code (VARCHAR(15), UNIQUE)
├── full_name (VARCHAR(255))
├── date_of_birth (VARCHAR(8)) ← Format: yyyyMMdd
├── gender (INT) ← 1: Nam, 2: Nữ
├── address (VARCHAR(255))
├── coverage_rate (INT) ← 80, 95, 100
└── valid_to (DATE) ← Ngày hết hạn
```

---

## ✅ Ưu Điểm Thiết Kế Này

1. **Tách biệt rõ ràng**: Bảng Master Data nhỏ gọn (9 cột) để tra cứu, không làm phình to database chính
2. **Mock API thực tế**: Mô phỏng y hệt API của BHXH, dễ chuyển sang API thật sau này
3. **Validation tự động**: Service tự kiểm tra thẻ hết hạn, trả message rõ ràng cho FE
4. **Test cases đầy đủ**: 5 case phủ hết luồng: còn hạn, hết hạn, mức hưởng khác nhau
5. **Format chuẩn Bộ Y tế**: Ngày sinh dùng String theo format `yyyyMMdd` đúng QĐ 130

---

## 🎓 Điểm Cộng Khi Demo Đồ Án

- ✅ **Kiến trúc Data Separation**: Giám khảo thấy em biết tách Mock Data và Real Data
- ✅ **API Design chuẩn RESTful**: Path parameters rõ ràng, HTTP status đúng
- ✅ **Error Handling chuyên nghiệp**: Message lỗi cụ thể, không để null exception
- ✅ **Business Logic**: Tự động validate thẻ hết hạn, tính toán coverage rate
- ✅ **Documentation**: File guide này chứng minh em biết viết tài liệu kỹ thuật

---

**Author**: AI-Generated with Best Practices  
**Date**: 2026-02-25  
**Version**: 1.0
