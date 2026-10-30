# Dự án: BE (Spring Boot) — Công nghệ & Cấu trúc Package

Tài liệu này tổng hợp các công nghệ chính được sử dụng trong dự án và cấu trúc package (package = thư mục) để các lập trình viên mới có thể nhanh chóng hiểu được cấu trúc và trách nhiệm của từng phần.

---

## 1. Các công nghệ sử dụng

| Công nghệ | Mô tả |
|-----------|-------|
| **Java 17+** | Ngôn ngữ lập trình chính |
| **Spring Boot** | Framework xây dựng REST API và quản lý dependency |
| **Spring Security** | Xác thực (authentication) và phân quyền (authorization) với custom UserDetails và JwtAuthenticationFilter |
| **JWT (JSON Web Tokens)** | Access token (thời gian sống ngắn) và refresh token (thời gian sống dài) để quản lý phiên đăng nhập |
| **Spring Data JPA + Hibernate** | ORM để thao tác với cơ sở dữ liệu thông qua repository |
| **MySQL** | Hệ quản trị cơ sở dữ liệu quan hệ |
| **Lombok** | Giảm boilerplate code (builders, getters/setters) |
| **Maven** | Hệ thống build và quản lý dependency (pom.xml) |
| **SpringDoc OpenAPI (Swagger)** | Tự động tạo tài liệu API và giao diện Swagger UI |

---

## 2. Cấu hình ứng dụng (application.properties)

```properties
# Tên ứng dụng
spring.application.name=BE

# Cấu hình MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/clinic_management_db
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Cấu hình JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Cổng server
server.port=8080

# Cấu hình JWT
jwt.secret=<secret_key>
jwt.access-token-expiration=86400000      # 24 giờ (ms)
jwt.refresh-token-expiration=604800000    # 7 ngày (ms)

# Cấu hình SpringDoc OpenAPI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.api-docs.enabled=true
```

**Truy cập Swagger UI:** http://localhost:8080/swagger-ui.html  
**Truy cập API Docs (JSON):** http://localhost:8080/api-docs

---

## 3. Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────┐
│                        Client (FE)                          │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP Request
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                 Controller Layer (REST API)                 │
│         - Nhận request, validate input DTO                  │
│         - Gọi service, trả response DTO                     │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│         - Xử lý business logic                              │
│         - Quản lý transaction                               │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                          │
│         - Truy xuất dữ liệu qua Spring Data JPA             │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                     MySQL Database                          │
│              (clinic_management_db)                         │
└─────────────────────────────────────────────────────────────┘
```

### Các tầng chính:

| Tầng | Mô tả |
|------|-------|
| **Controller** | Các REST controller expose HTTP endpoint |
| **Service** | Business logic, xử lý transaction |
| **Repository** | Truy cập dữ liệu qua Spring Data JPA |
| **Security** | Filter, token utilities, user detail services |
| **DTO** | Request/Response object tách biệt domain entity khỏi API |
| **Entity** | Các đối tượng domain được map với JPA |

---

## 4. Cấu trúc Package chi tiết

```
src/main/java/g2/be/
│
├── BeApplication.java          # Entry point của Spring Boot
│
├── config/
│   └── SecurityConfig.java     # Cấu hình Spring Security, AuthenticationManager,
│                                # PasswordEncoder và filter chain
│
├── controller/
│   └── AuthController.java     # Các endpoint xác thực (login, register, refresh, logout)
│   └── [Các controller khác]   # Các API theo domain
│
├── dto/
│   ├── request/                # DTO cho request đầu vào
│   │   ├── LoginRequest.java
│   │   └── RegisterRequest.java
│   │
│   └── response/               # DTO cho response đầu ra
│       ├── LoginResponse.java
│       ├── RefreshTokenResponse.java
│       └── UserInfoResponse.java
│
├── entity/
│   ├── User.java               # Entity người dùng (quan hệ với Role, Doctor, Patient)
│   ├── Role.java               # Entity vai trò
│   ├── Doctor.java             # Entity bác sĩ
│   ├── Patient.java            # Entity bệnh nhân
│   ├── Department.java         # Entity khoa/phòng ban
│   └── RefreshToken.java       # Entity refresh token
│
├── repository/
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── DoctorRepository.java
│   ├── PatientRepository.java
│   └── RefreshTokenRepository.java
│
├── security/
│   ├── JwtUtil.java                # Tạo và validate JWT token
│   ├── JwtAuthenticationFilter.java # Filter kiểm tra token mỗi request
│   ├── UserDetailsImpl.java        # Implement UserDetails của Spring Security
│   └── UserDetailsServiceImpl.java # Load user từ DB cho Spring Security
│
└── service/
    ├── AuthService.java            # Logic xác thực (login, register, refresh, logout)
    └── RefreshTokenService.java    # Quản lý refresh token (tạo, verify, xóa)
```

---

## 5. Luồng xác thực (Authentication Flow)

### 5.1. Luồng đăng nhập (Login)

```
1. Client gửi username/password → POST /api/auth/login
2. AuthController nhận LoginRequest
3. AuthenticationManager xác thực qua UserDetailsServiceImpl
4. Nếu thành công:
   - JwtUtil tạo access token (24 giờ)
   - RefreshTokenService tạo refresh token (7 ngày, lưu DB)
5. Trả về LoginResponse: accessToken, refreshToken, thông tin user
```

### 5.2. Luồng làm mới token (Refresh)

```
1. Client gửi refresh token → POST /api/auth/refresh
2. Server kiểm tra refresh token trong DB
3. Nếu hợp lệ và chưa hết hạn:
   - Tạo access token mới
   - (Tùy chọn) Rotate refresh token mới
4. Trả về token mới
```

### 5.3. Luồng đăng xuất (Logout)

```
1. Client gửi refresh token → POST /api/auth/logout
2. Server xóa refresh token khỏi DB
3. Token không thể sử dụng lại
```

### 5.4. Luồng truy cập API được bảo vệ

```
1. Client gửi request với header: Authorization: Bearer <access_token>
2. JwtAuthenticationFilter chặn request
3. Validate token bằng JwtUtil
4. Nếu hợp lệ → cho phép truy cập
5. Nếu không hợp lệ → trả về 401 Unauthorized
```

---

## 6. Mô tả các Entity chính

| Entity | Mô tả | Quan hệ |
|--------|-------|---------|
| **User** | Người dùng hệ thống | ManyToMany với Role, OneToOne với Doctor/Patient |
| **Role** | Vai trò (ADMIN, DOCTOR, PATIENT, ...) | ManyToMany với User |
| **Doctor** | Thông tin bác sĩ | OneToOne với User, ManyToOne với Department |
| **Patient** | Thông tin bệnh nhân | OneToOne với User |
| **Department** | Khoa/Phòng ban | OneToMany với Doctor |
| **RefreshToken** | Token làm mới phiên | OneToOne với User |

---

## 7. Các file quan trọng cần xem khi sửa đổi Auth/Security

| File | Đường dẫn |
|------|-----------|
| Tạo/Validate JWT | `src/main/java/g2/be/security/JwtUtil.java` |
| Filter xác thực | `src/main/java/g2/be/security/JwtAuthenticationFilter.java` |
| Load user từ DB | `src/main/java/g2/be/security/UserDetailsServiceImpl.java` |
| Logic xác thực | `src/main/java/g2/be/service/AuthService.java` |
| Quản lý refresh token | `src/main/java/g2/be/service/RefreshTokenService.java` |
| Cấu hình Security | `src/main/java/g2/be/config/SecurityConfig.java` |

---

## 8. Hướng dẫn chạy dự án

### Yêu cầu:
- Java 17+
- Maven 3.6+
- MySQL Server (database: `clinic_management_db`)

### Các bước:

```bash
# 1. Clone hoặc mở project
cd E:\DA\BE

# 2. Cài đặt dependency
mvn clean install

# 3. Chạy ứng dụng
mvn spring-boot:run

# Hoặc chạy từ IDE (IntelliJ/Eclipse): Run BeApplication.java
```

### Kiểm tra API:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/api-docs

### Các endpoint chính:
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/auth/login` | Đăng nhập |
| POST | `/api/auth/register` | Đăng ký |
| POST | `/api/auth/refresh` | Làm mới token |
| POST | `/api/auth/logout` | Đăng xuất |

---

## 9. Thuật ngữ

| Thuật ngữ | Giải thích |
|-----------|------------|
| **Access Token** | JWT được gửi trong header `Authorization: Bearer <token>` để truy cập tài nguyên được bảo vệ |
| **Refresh Token** | Token lưu server-side dùng để lấy access token mới khi access token hết hạn |
| **DTO** | Data Transfer Object - object dùng cho API request/response |
| **Entity** | Đối tượng domain được map với bảng trong database |
| **Repository** | Interface truy cập dữ liệu theo pattern Spring Data JPA |

---

## 10. Gợi ý mở rộng tài liệu

- [ ] Thêm API Reference chi tiết (request/response mẫu)
- [ ] Tạo Postman Collection cho các endpoint
- [ ] Vẽ diagram UML cho các entity
- [ ] Thêm hướng dẫn deploy lên server

---

**Cập nhật lần cuối:** 15/01/2026
