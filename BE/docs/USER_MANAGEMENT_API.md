# 👥 User Management API Documentation

## Tổng quan
API quản lý tài khoản người dùng cho hệ thống Clinic Management. Chỉ có **ADMIN** và **CASHIER** có quyền truy cập các APIs này.

---

## 🔐 Authentication
Tất cả các endpoints yêu cầu JWT token trong header:
```
Authorization: Bearer {your_jwt_token}
```

---

## 📋 Endpoints

### 1. Lấy danh sách người dùng
**GET** `/api/users`

**Query Parameters:**
| Tham số | Kiểu | Mặc định | Mô tả |
|---------|------|----------|-------|
| page | Integer | 0 | Số trang (bắt đầu từ 0) |
| size | Integer | 10 | Số lượng bản ghi mỗi trang |
| sortBy | String | userId | Trường sắp xếp (userId, username, email, fullName, createdAt) |
| sortDir | String | asc | Hướng sắp xếp (asc hoặc desc) |
| keyword | String | - | Từ khóa tìm kiếm (username, email, fullName, phoneNumber) |
| isActive | Boolean | - | Lọc theo trạng thái active |

**Response:**
```json
{
  "content": [
    {
      "userId": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "fullName": "John Doe",
      "cccd": "001234567890",
      "gender": "MALE",
      "phoneNumber": "0901234567",
      "avatarUrl": "https://example.com/avatar.jpg",
      "isActive": true,
      "isDeleted": false,
      "roles": ["ROLE_PATIENT"],
      "createdAt": "2026-01-15T10:30:00",
      "updatedAt": "2026-01-15T10:30:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 50,
  "totalPages": 5,
  "last": false,
  "first": true
}
```

**Ví dụ:**
```bash
# Lấy trang đầu tiên
GET /api/users?page=0&size=10

# Tìm kiếm theo tên
GET /api/users?keyword=john&page=0&size=10

# Lọc user đang active
GET /api/users?isActive=true&page=0&size=10

# Sắp xếp theo ngày tạo giảm dần
GET /api/users?sortBy=createdAt&sortDir=desc
```

---

### 2. Lấy thông tin người dùng theo ID
**GET** `/api/users/{userId}`

**Path Parameters:**
- `userId` (Long, required): ID của người dùng

**Response:**
```json
{
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "cccd": "001234567890",
  "gender": "MALE",
  "phoneNumber": "0901234567",
  "avatarUrl": "https://example.com/avatar.jpg",
  "isActive": true,
  "isDeleted": false,
  "roles": ["ROLE_PATIENT"],
  "createdAt": "2026-01-15T10:30:00",
  "updatedAt": "2026-01-15T10:30:00"
}
```

**Ví dụ:**
```bash
GET /api/users/1
```

---

### 3. Tạo tài khoản người dùng
**POST** `/api/users`

**Request Body:**
```json
{
  "username": "jane_doe",
  "email": "jane@example.com",
  "password": "securePassword123",
  "fullName": "Jane Doe",
  "cccd": "001234567891",
  "gender": "FEMALE",
  "phoneNumber": "0907654321",
  "avatarUrl": "https://example.com/jane-avatar.jpg",
  "roleNames": ["ROLE_PATIENT"]
}
```

**Validation:**
- `username`: Bắt buộc, 3-50 ký tự, chỉ chứa chữ cái, số và dấu gạch dưới
- `email`: Bắt buộc, định dạng email hợp lệ
- `password`: Bắt buộc, tối thiểu 6 ký tự
- `fullName`: Bắt buộc, tối đa 100 ký tự
- `cccd`: Tùy chọn, 9-20 ký tự
- `phoneNumber`: Tùy chọn, 10-15 chữ số
- `roleNames`: Tùy chọn, mặc định là ["ROLE_PATIENT"]

**Response:** (Status 201 Created)
```json
{
  "userId": 2,
  "username": "jane_doe",
  "email": "jane@example.com",
  "fullName": "Jane Doe",
  "cccd": "001234567891",
  "gender": "FEMALE",
  "phoneNumber": "0907654321",
  "avatarUrl": "https://example.com/jane-avatar.jpg",
  "isActive": true,
  "isDeleted": false,
  "roles": ["ROLE_PATIENT"],
  "createdAt": "2026-02-02T21:30:00",
  "updatedAt": "2026-02-02T21:30:00"
}
```

**Errors:**
- `400`: Validation error
- `409`: Username, email hoặc số điện thoại đã tồn tại

---

### 4. Cập nhật thông tin người dùng
**PUT** `/api/users/{userId}`

**Path Parameters:**
- `userId` (Long, required): ID của người dùng

**Request Body:**
```json
{
  "email": "newemail@example.com",
  "fullName": "Jane Updated Doe",
  "cccd": "001234567892",
  "gender": "FEMALE",
  "phoneNumber": "0907654322",
  "avatarUrl": "https://example.com/new-avatar.jpg",
  "roleNames": ["ROLE_PATIENT", "ROLE_DOCTOR"],
  "isActive": true
}
```

**Note:** Tất cả các trường đều tùy chọn. Chỉ các trường được gửi lên mới được cập nhật.

**Response:**
```json
{
  "userId": 2,
  "username": "jane_doe",
  "email": "newemail@example.com",
  "fullName": "Jane Updated Doe",
  "cccd": "001234567892",
  "gender": "FEMALE",
  "phoneNumber": "0907654322",
  "avatarUrl": "https://example.com/new-avatar.jpg",
  "isActive": true,
  "isDeleted": false,
  "roles": ["ROLE_PATIENT", "ROLE_DOCTOR"],
  "createdAt": "2026-02-02T21:30:00",
  "updatedAt": "2026-02-02T21:35:00"
}
```

**Errors:**
- `404`: Không tìm thấy người dùng
- `409`: Email hoặc số điện thoại đã tồn tại

---

### 5. Xóa người dùng (Soft Delete)
**DELETE** `/api/users/{userId}`

**Path Parameters:**
- `userId` (Long, required): ID của người dùng cần xóa

**Response:**
```json
{
  "message": "Xóa người dùng thành công"
}
```

**Note:** Đây là soft delete - người dùng chỉ được đánh dấu `isDeleted = true` và `isActive = false`, không bị xóa khỏi database.

**Errors:**
- `404`: Không tìm thấy người dùng
- `409`: Người dùng đã bị xóa trước đó

---

### 6. Khóa tài khoản
**PATCH** `/api/users/{userId}/lock`

**Path Parameters:**
- `userId` (Long, required): ID của người dùng cần khóa

**Response:**
```json
{
  "message": "Khóa tài khoản thành công"
}
```

**Note:** Set `isActive = false` - người dùng không thể đăng nhập

**Errors:**
- `404`: Không tìm thấy người dùng
- `409`: Tài khoản đã bị khóa hoặc đã bị xóa

---

### 7. Mở khóa tài khoản
**PATCH** `/api/users/{userId}/unlock`

**Path Parameters:**
- `userId` (Long, required): ID của người dùng cần mở khóa

**Response:**
```json
{
  "message": "Mở khóa tài khoản thành công"
}
```

**Note:** Set `isActive = true` - người dùng có thể đăng nhập trở lại

**Errors:**
- `404`: Không tìm thấy người dùng
- `409`: Tài khoản đang hoạt động hoặc đã bị xóa

---

## 🔑 Roles & Permissions

### Danh sách Roles trong hệ thống:
- `ROLE_ADMIN`: Quản trị viên
- `ROLE_CASHIER`: Thu ngân
- `ROLE_DOCTOR`: Bác sĩ
- `ROLE_NURSE`: Điều dưỡng
- `ROLE_PHARMACIST`: Dược sĩ
- `ROLE_RECEPTIONIST`: Lễ tân
- `ROLE_PATIENT`: Bệnh nhân
- `ROLE_MANAGER`: Quản lý

### Quyền truy cập User Management:
✅ **ADMIN**: Toàn quyền  
✅ **CASHIER**: Toàn quyền  
❌ **Các roles khác**: Không có quyền

---

## 🧪 Test với Postman/cURL

### 1. Login để lấy token (với user có role ADMIN hoặc CASHIER):
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Response sẽ có `accessToken`, copy token này.

### 2. Lấy danh sách users:
```bash
curl -X GET "http://localhost:8080/api/users?page=0&size=10" \
  -H "Authorization: Bearer {your_access_token}"
```

### 3. Tạo user mới:
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {your_access_token}" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "password123",
    "fullName": "New User",
    "phoneNumber": "0901234567",
    "roleNames": ["ROLE_PATIENT"]
  }'
```

### 4. Cập nhật user:
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {your_access_token}" \
  -d '{
    "fullName": "Updated Name",
    "phoneNumber": "0907654321"
  }'
```

### 5. Khóa tài khoản:
```bash
curl -X PATCH http://localhost:8080/api/users/1/lock \
  -H "Authorization: Bearer {your_access_token}"
```

### 6. Mở khóa tài khoản:
```bash
curl -X PATCH http://localhost:8080/api/users/1/unlock \
  -H "Authorization: Bearer {your_access_token}"
```

### 7. Xóa user:
```bash
curl -X DELETE http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer {your_access_token}"
```

---

## 📊 Error Codes

| Status Code | Mô tả |
|------------|-------|
| 200 | Thành công |
| 201 | Tạo mới thành công |
| 400 | Bad Request - Dữ liệu không hợp lệ |
| 401 | Unauthorized - Chưa đăng nhập |
| 403 | Forbidden - Không có quyền truy cập |
| 404 | Not Found - Không tìm thấy tài nguyên |
| 409 | Conflict - Dữ liệu bị trùng lặp |
| 500 | Internal Server Error |

---

## 🎯 Business Logic

### Tạo User:
1. Kiểm tra username, email, phoneNumber có bị trùng không
2. Mã hóa password bằng BCrypt
3. Gán roles (mặc định ROLE_PATIENT nếu không chỉ định)
4. Set `isActive = true`, `isDeleted = false`
5. Lưu vào database

### Cập nhật User:
1. Kiểm tra user có tồn tại và chưa bị xóa
2. Nếu đổi email/phoneNumber, kiểm tra trùng lặp
3. Chỉ cập nhật các trường được gửi lên
4. Có thể thay đổi roles và status

### Xóa User (Soft Delete):
1. Không xóa khỏi database
2. Set `isDeleted = true`, `isActive = false`
3. User không thể đăng nhập và không hiển thị trong danh sách

### Khóa/Mở khóa:
1. Chỉ thay đổi trạng thái `isActive`
2. User bị khóa không thể đăng nhập
3. Không thể khóa/mở khóa user đã bị xóa

---

## 🔗 Swagger UI
Truy cập Swagger UI để test API:
```
http://localhost:8080/swagger-ui.html
```

Click "Authorize" và nhập token dạng: `Bearer {your_token}`

---

## 📝 Notes
- Tất cả các API đều yêu cầu authentication
- Chỉ ADMIN và CASHIER có quyền truy cập
- Passwords được mã hóa bằng BCrypt
- Timestamps tự động được tạo/cập nhật
- Hỗ trợ phân trang, sắp xếp, tìm kiếm đầy đủ
