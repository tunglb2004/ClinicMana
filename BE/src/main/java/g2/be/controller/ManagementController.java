package g2.be.controller;

import g2.be.dto.response.DepartmentResponse;
import g2.be.dto.response.ReviewResponse;
import g2.be.dto.response.StaffResponse;
import g2.be.repository.AppointmentRepository;
import g2.be.repository.DepartmentRepository;
import g2.be.repository.InvoiceRepository;
import g2.be.repository.ReviewRepository;
import g2.be.repository.UserRepository;
import g2.be.service.ReviewService;
import g2.be.util.ClinicTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Tag(name = "Management", description = "APIs quản lý chung cho Manager")
public class ManagementController {

        private final DepartmentRepository departmentRepository;
        private final UserRepository userRepository;
        private final AppointmentRepository appointmentRepository;
        private final InvoiceRepository invoiceRepository;
        private final ReviewRepository reviewRepository;
        private final ReviewService reviewService;

        private static long longAt(Object[] row, int index) {
                row = normalizeRow(row);
                Object value = row != null && row.length > index ? row[index] : null;
                if (value instanceof Number number) {
                        return number.longValue();
                }
                return value != null ? Long.parseLong(value.toString()) : 0L;
        }

        private static double doubleAt(Object[] row, int index) {
                row = normalizeRow(row);
                Object value = row != null && row.length > index ? row[index] : null;
                if (value instanceof Number number) {
                        return number.doubleValue();
                }
                return value != null ? Double.parseDouble(value.toString()) : 0D;
        }

        private static BigDecimal decimalAt(Object[] row, int index) {
                row = normalizeRow(row);
                Object value = row != null && row.length > index ? row[index] : null;
                if (value instanceof BigDecimal decimal) {
                        return decimal;
                }
                if (value instanceof Number number) {
                        return BigDecimal.valueOf(number.doubleValue());
                }
                return value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
        }

        private static Object[] normalizeRow(Object[] row) {
                if (row != null && row.length == 1 && row[0] instanceof Object[] nested) {
                        return nested;
                }
                return row;
        }

        @GetMapping("/dashboard-summary")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN', 'ROLE_CLINIC_MANAGER', 'ROLE_MANAGER')")
        @Operation(summary = "Tổng hợp dashboard quản lý", description = "Trả về số liệu dashboard bằng các query tổng hợp thay vì tải nhiều danh sách lớn")
        public ResponseEntity<Map<String, Object>> getDashboardSummary() {
                LocalDate today = ClinicTime.today();
                LocalDate monthStartDate = today.withDayOfMonth(1);
                LocalDateTime monthStart = monthStartDate.atStartOfDay();
                LocalDateTime nextMonthStart = monthStartDate.plusMonths(1).atStartOfDay();
                Object[] staffStats = userRepository.getManagerStaffStats();
                Object[] appointmentStats = appointmentRepository.getManagerAppointmentStats(today);
                Object[] invoiceStats = invoiceRepository.getPaidSummaryBetween(monthStart, nextMonthStart);
                Object[] reviewStats = reviewRepository.getManagerReviewStats();

                LocalDate sevenDaysStartDate = today.minusDays(6);
                Map<String, BigDecimal> revenueByDate = new LinkedHashMap<>();
                invoiceRepository
                                .sumPaidCustomerAmountByDateBetween(sevenDaysStartDate.atStartOfDay(), today.plusDays(1).atStartOfDay())
                                .forEach(row -> {
                                        Object dateValue = row[0];
                                        String dateKey = dateValue instanceof java.sql.Date sqlDate
                                                        ? sqlDate.toLocalDate().toString()
                                                        : String.valueOf(dateValue);
                                        BigDecimal revenue = row[1] instanceof BigDecimal value ? value : BigDecimal.ZERO;
                                        revenueByDate.put(dateKey, revenue);
                                });

                List<Map<String, Object>> revenueByDay = java.util.stream.IntStream.rangeClosed(0, 6)
                                .mapToObj(offset -> today.minusDays(6L - offset))
                                .map(day -> {
                                        BigDecimal revenue = revenueByDate.getOrDefault(day.toString(), BigDecimal.ZERO);
                                        return Map.<String, Object>of(
                                                        "date", day.toString(),
                                                        "label", String.format("%02d/%02d", day.getDayOfMonth(), day.getMonthValue()),
                                                        "value", revenue);
                                })
                                .toList();

                Map<String, Object> metrics = new LinkedHashMap<>();
                metrics.put("totalStaff", longAt(staffStats, 0));
                metrics.put("doctorCount", longAt(staffStats, 1));
                metrics.put("nurseCount", longAt(staffStats, 2));
                metrics.put("activeStaff", longAt(staffStats, 3));
                metrics.put("todayAppointments", longAt(appointmentStats, 0));
                metrics.put("waitingAppointments", longAt(appointmentStats, 1));
                metrics.put("completedAppointments", longAt(appointmentStats, 2));
                metrics.put("monthRevenue", decimalAt(invoiceStats, 0));
                metrics.put("monthPaidInvoices", longAt(invoiceStats, 1));
                metrics.put("averageRating", doubleAt(reviewStats, 0));
                metrics.put("pendingReviews", longAt(reviewStats, 1));

                List<ReviewResponse> recentReviews = reviewService.getAllReviews(0, 5).getContent();

                return ResponseEntity.ok(Map.of(
                                "metrics", metrics,
                                "revenueByDay", revenueByDay,
                                "recentReviews", recentReviews));
        }

        @GetMapping("/departments")
        @Operation(summary = "Lấy danh sách phòng ban", description = "Lấy toàn bộ danh sách phòng ban cho dropdown")
        public ResponseEntity<List<DepartmentResponse>> getDepartments() {
                List<DepartmentResponse> departments = departmentRepository.findAll().stream()
                                .map(d -> DepartmentResponse.builder()
                                                .deptId(d.getDeptId())
                                                .deptName(d.getDeptName())
                                                .build())
                                .collect(Collectors.toList());
                return ResponseEntity.ok(departments);
        }

        @GetMapping("/staff")
        @Operation(summary = "Lấy danh sách nhân viên", description = "Lấy danh sách người dùng có thể gán lịch (Bác sĩ, tá, nhân viên...)")
        public ResponseEntity<List<StaffResponse>> getStaff() {
                // Đơn giản hóa: Lấy tất cả user không bị xóa và không phải chỉ là bệnh nhân
                // Trong thực tế sẽ lọc theo Role
                List<StaffResponse> staff = userRepository.findAll().stream()
                                .filter(u -> !u.getIsDeleted()
                                                && u.getRoles().stream()
                                                                .noneMatch(r -> r.getRoleName().equals("ROLE_PATIENT")))
                                .map(u -> StaffResponse.builder()
                                                .userId(u.getUserId())
                                                .fullName(u.getFullName())
                                                .username(u.getUsername())
                                                .build())
                                .collect(Collectors.toList());
                return ResponseEntity.ok(staff);
        }
}
