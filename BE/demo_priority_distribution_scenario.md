# Kịch Bản Demo Phân Phối Phòng Và Ưu Tiên Hàng Đợi

Ngày demo: `27/04/2026`.

Seed data:
- Mẫu ca sáng: `BE/demo_priority_distribution_seed.sql`
- Mẫu ca tối: chạy lại mẫu ca sáng, sau đó chạy `BE/demo_priority_distribution_evening_patch.sql`
- Ca tối dùng bộ demo riêng có chữ `toi`: tài khoản `demo.toi.*`, phòng `D-NOI-TOI-*`, dịch vụ `D-SVC-NOI-TOI`, lịch hẹn `D-APT-TOI-*`
- Phạm vi seed: chỉ rebuild dữ liệu demo có prefix `D-*` và tài khoản `demo.*`
- Mật khẩu tất cả tài khoản demo: `Demo@123`
- Đăng nhập bằng email hoặc số điện thoại, không đăng nhập bằng username.

## Tài Khoản Demo Ca Sáng

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Lễ tân | `demo.reception@clinicpro.local` | `Demo@123` |
| Thu ngân | `demo.cashier@clinicpro.local` | `Demo@123` |
| Bác sĩ Nội 1 | `demo.doctor1@clinicpro.local` | `Demo@123` |
| Bác sĩ Nội 2 | `demo.doctor2@clinicpro.local` | `Demo@123` |
| Bác sĩ Nội 3 | `demo.doctor3@clinicpro.local` | `Demo@123` |
| Y tá | `demo.nurse@clinicpro.local` | `Demo@123` |

## Tài Khoản Demo Ca Tối

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Lễ tân tối | `demo.toi.reception@clinicpro.local` | `Demo@123` |
| Thu ngân tối | `demo.toi.cashier@clinicpro.local` | `Demo@123` |
| Bác sĩ Nội tối 1 | `demo.toi.doctor1@clinicpro.local` | `Demo@123` |
| Bác sĩ Nội tối 2 | `demo.toi.doctor2@clinicpro.local` | `Demo@123` |
| Bác sĩ Nội tối 3 | `demo.toi.doctor3@clinicpro.local` | `Demo@123` |
| Y tá tối | `demo.toi.nurse@clinicpro.local` | `Demo@123` |

## Mẫu 1 - Demo Ca Sáng

Dịch vụ demo:

```text
D-SVC-NOI - Demo - Kham Noi uu tien
```

Phòng cùng dịch vụ:

| Phòng | Bác sĩ | Ghi chú |
| --- | --- | --- |
| `D-NOI-1` | `demo.doctor1@clinicpro.local` | Phòng đang đông, dùng để demo ưu tiên |
| `D-NOI-2` | `demo.doctor2@clinicpro.local` | Phòng có ít bệnh nhân hơn |
| `D-NOI-3` | `demo.doctor3@clinicpro.local` | Phòng trống, dùng để demo phân phối phòng |

Tải phòng ban đầu:

| Phòng | Số bệnh nhân đang chờ |
| --- | ---: |
| `D-NOI-1` | 5 |
| `D-NOI-2` | 2 |
| `D-NOI-3` | 0 |

Nạp dữ liệu:

```powershell
cd E:\DA\BE
$props = @{}
Get-Content .\src\main\resources\application.properties | ForEach-Object {
  if ($_ -match '^(spring\.datasource\.(url|username|password))=(.*)$') {
    $props[$matches[1]] = $matches[3]
  }
}
$jar = 'C:\Users\TRUONG GIANG\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar'
java -cp "scripts;$jar" SqlRunner $props['spring.datasource.url'] $props['spring.datasource.username'] $props['spring.datasource.password'] .\demo_priority_distribution_seed.sql
```

### Bước 1 - Xem hàng đợi theo phòng

Mở:

```text
/queue-display/room/D-NOI-1
/queue-display/room/D-NOI-2
/queue-display/room/D-NOI-3
```

Expected:
- `D-NOI-1` có 5 bệnh nhân đang chờ.
- `D-NOI-2` có 2 bệnh nhân đang chờ.
- `D-NOI-3` ban đầu chưa có bệnh nhân đang chờ.

### Bước 2 - Bác sĩ xem hàng đợi ưu tiên

Đăng nhập:

```text
demo.doctor1@clinicpro.local / Demo@123
```

Mở:

```text
/dashboard/doctor/patients
```

Tại mốc demo khoảng `07:30`, thứ tự kỳ vọng ở phòng `D-NOI-1`:

| Thứ tự | Lịch hẹn | Loại | Base priority | Waiting/Aging |
| ---: | --- | --- | ---: | --- |
| 1 | `D-APT-RET` | Quay lại sau CLS | 500 | chờ từ `06:50` |
| 2 | `D-APT-URG` | Ưu tiên nghiệp vụ | 500 | chờ từ `06:51` |
| 3 | `D-APT-FUP` | Tái khám | 100 | chờ từ `06:52` |
| 4 | `D-APT-WAIT` | Khám mới chờ lâu | 0 | chờ từ `06:53` |
| 5 | `D-APT-NORM` | Khám mới thường | 0 | chờ từ `06:55` |

Công thức đang dùng:

```text
TotalPriorityScore =
  BasePriority
+ WaitingMinutes
+ AgingBonus
```

AgingBonus:

```text
>= 30 phút: +300
>= 60 phút: +700
>= 90 phút: +1200
```

Điểm cần nói khi demo:
- Bệnh nhân quay lại sau CLS và ca ưu tiên nghiệp vụ có base `500`, nên đứng đầu.
- Tái khám có base `100`, được ưu ái hơn khám mới thường.
- Bệnh nhân mới vẫn được cộng thời gian chờ và AgingBonus để không bị chờ vô hạn.

### Bước 3 - Phân phòng chính thức khi thanh toán

Đăng nhập:

```text
demo.cashier@clinicpro.local / Demo@123
```

Tìm lịch:

```text
D-APT-ALLOC
```

Expected:
- Trước thanh toán: chưa có `room_id`, trạng thái `CONFIRMED`.
- Sau thanh toán: hệ thống phân phòng chính thức vào `D-NOI-3`.
- Bệnh nhân có số thứ tự trong hàng đợi của `D-NOI-3`.
- Sau khi đã in/thanh toán, hệ thống không tự đổi phòng nữa.

## Mẫu 2 - Demo Ca Tối

Mẫu ca tối dùng bộ tài khoản, phòng, dịch vụ và lịch hẹn riêng có chữ `toi`. Mục tiêu mẫu này là demo rõ cơ chế chống chờ vô hạn, đồng thời có bước phân phối phòng sau khi thanh toán như ca sáng.

Dịch vụ demo tối:

```text
D-SVC-NOI-TOI - Demo Toi - Kham Noi uu tien
```

Phòng cùng dịch vụ:

| Phòng | Bác sĩ | Ghi chú |
| --- | --- | --- |
| `D-NOI-TOI-1` | `demo.toi.doctor1@clinicpro.local` | Phòng đang đông, dùng để demo chống chờ vô hạn |
| `D-NOI-TOI-2` | `demo.toi.doctor2@clinicpro.local` | Phòng có ít bệnh nhân hơn |
| `D-NOI-TOI-3` | `demo.toi.doctor3@clinicpro.local` | Phòng trống, dùng để demo phân phối phòng |

Tải phòng ban đầu:

| Phòng | Số bệnh nhân đang chờ |
| --- | ---: |
| `D-NOI-TOI-1` | 5 |
| `D-NOI-TOI-2` | 2 |
| `D-NOI-TOI-3` | 0 |

Nạp dữ liệu:

```powershell
cd E:\DA\BE
$props = @{}
Get-Content .\src\main\resources\application.properties | ForEach-Object {
  if ($_ -match '^(spring\.datasource\.(url|username|password))=(.*)$') {
    $props[$matches[1]] = $matches[3]
  }
}
$jar = 'C:\Users\TRUONG GIANG\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar'
java -cp "scripts;$jar" SqlRunner $props['spring.datasource.url'] $props['spring.datasource.username'] $props['spring.datasource.password'] .\demo_priority_distribution_seed.sql
java -cp "scripts;$jar" SqlRunner $props['spring.datasource.url'] $props['spring.datasource.username'] $props['spring.datasource.password'] .\demo_priority_distribution_evening_patch.sql
```

### Bước 1 - Xem hàng đợi ca tối theo phòng

Mở:

```text
/queue-display/room/D-NOI-TOI-1
/queue-display/room/D-NOI-TOI-2
/queue-display/room/D-NOI-TOI-3
```

Expected:
- `D-NOI-TOI-1` có 5 bệnh nhân đang chờ.
- `D-NOI-TOI-2` có 2 bệnh nhân đang chờ.
- `D-NOI-TOI-3` ban đầu chưa có bệnh nhân đang chờ.

### Bước 2 - Bác sĩ xem hàng đợi chống chờ vô hạn

Đăng nhập:

```text
demo.toi.doctor1@clinicpro.local / Demo@123
```

Mở:

```text
/dashboard/doctor/patients
```

Tại mốc demo `18:00`, thứ tự kỳ vọng ở phòng `D-NOI-TOI-1`:

| Thứ tự | Lịch hẹn | Loại | Base priority | Lý do |
| ---: | --- | --- | ---: | --- |
| 1 | `D-APT-TOI-WAIT` | Khám mới chờ rất lâu | 0 | chờ từ `16:00`, được AgingBonus `+1200` |
| 2 | `D-APT-TOI-RET` | Quay lại sau CLS | 500 | base cao nhưng mới chờ từ `17:45` |
| 3 | `D-APT-TOI-URG` | Ưu tiên nghiệp vụ | 500 | base cao nhưng mới chờ từ `17:46` |
| 4 | `D-APT-TOI-FUP` | Tái khám | 100 | được ưu ái hơn khám mới thường |
| 5 | `D-APT-TOI-NORM` | Khám mới thường | 0 | mới chờ từ `17:50` |

Điểm cần nói khi demo:
- Không phải cứ quay lại CLS là luôn đè mọi nhóm khác.
- Nếu bệnh nhân mới chờ quá lâu, `WaitingMinutes + AgingBonus` sẽ kéo bệnh nhân lên đầu.
- Đây là rule chống starvation cho hàng đợi.

### Bước 3 - Phân phòng chính thức khi thanh toán ca tối

Đăng nhập:

```text
demo.toi.cashier@clinicpro.local / Demo@123
```

Tìm lịch:

```text
D-APT-TOI-ALLOC
```

Expected:
- Trước thanh toán: chưa có `room_id`, trạng thái `CONFIRMED`.
- Sau thanh toán: hệ thống phân phòng chính thức vào `D-NOI-TOI-3`, vì đây là phòng cùng dịch vụ `D-SVC-NOI-TOI` có tải thấp nhất.
- Bệnh nhân có số thứ tự trong hàng đợi của `D-NOI-TOI-3`.
- Mở `/queue-display/room/D-NOI-TOI-3` để thấy bệnh nhân mới được thêm vào phòng sau thanh toán.
- Sau khi đã in/thanh toán, hệ thống không tự đổi phòng nữa.

## Kiểm Tra Nhanh Bằng SQL

```powershell
cd E:\DA\BE
$props = @{}
Get-Content .\src\main\resources\application.properties | ForEach-Object {
  if ($_ -match '^(spring\.datasource\.(url|username|password))=(.*)$') {
    $props[$matches[1]] = $matches[3]
  }
}
$jar = 'C:\Users\TRUONG GIANG\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar'
java -cp "scripts;$jar" SqlRunner $props['spring.datasource.url'] $props['spring.datasource.username'] $props['spring.datasource.password'] --query "SELECT a.appointment_id, a.appointment_date, a.appointment_time, a.status, a.room_id, a.queue_number, a.priority_score, a.waiting_since FROM appointments a WHERE a.appointment_id LIKE 'D-APT-%' ORDER BY a.appointment_date, a.appointment_time, a.room_id, a.queue_number;"
```
