from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Mm, Pt, RGBColor


PRIMARY_OUTPUT_PATH = Path(r"C:\Report4_Mo_ta_class_chuc_nang.docx")
FALLBACK_OUTPUT_PATH = Path.home() / "Report4_Mo_ta_class_chuc_nang.docx"
FONT_NAME = "Times New Roman"
BODY_SIZE = 12
HEADER_FILL = "F4B183"
LABEL_FILL = "FCE4D6"
TEXT_COLOR = RGBColor(0, 0, 0)


SECTIONS = [
    {
        "title": "View customer accounts list",
        "screen_name": "Customer Accounts List",
        "screen_rows": [
            ("1", "Search box", "Input text", "No", "Free text", "Nhập từ khóa để lọc theo tên bệnh nhân, email, số điện thoại hoặc mã bệnh nhân trước khi FE gọi lại API danh sách."),
            ("2", "Status filter", "Select", "No", "3 trạng thái", "Lọc danh sách theo ALL, ACTIVE, LOCKED; FE chuyển thành tham số isActive tương ứng khi gọi API."),
            ("3", "Patients table", "Data table", "Yes", "Theo pageSize", "Hiển thị STT, mã bệnh nhân, họ tên, SĐT, email, ngày sinh, CCCD và trạng thái hoạt động."),
            ("4", "Pagination", "Pagination", "No", "N/A", "Điều hướng trang hiện tại, đồng bộ currentPage, totalPages và totalElements theo dữ liệu PageResponse từ backend."),
            ("5", "Page size selector", "Select", "No", "10/20/30/40/50", "Cho phép đổi số dòng hiển thị mỗi trang và tự reset về trang đầu sau khi thay đổi."),
        ],
        "diagram_note": "Class diagram for this use case follows the current implementation chain in source code: Admin patient page -> patientService -> PatientManagementController -> PatientManagementService. The detailed sequence reference is available in BE/docs/SEQUENCE_UML_CUSTOMER_APPOINTMENT_PAYMENT_SERVICE.puml.",
        "classes": [
            {
                "name": "PatientManagementPage",
                "description": "Trang quản lý bệnh nhân của admin. Component này chịu trách nhiệm tải danh sách bệnh nhân theo từ khóa, trạng thái và phân trang; sau đó render bảng dữ liệu để người dùng xem customer accounts list.",
                "base_class": "React function component",
                "constructor": "Không có constructor OOP; state được quản lý bằng useState/useEffect/useMemo/useCallback.",
                "prototype": "export default function PatientManagementPage(): JSX.Element",
                "source_file": "clinic-management-fe/app/dashboard/admin/patients/page.tsx",
                "namespace": "Next.js App Router page module",
                "attributes": [
                    ("patients", "AdminPatientRecord[]", "Danh sách bệnh nhân đang hiển thị trên bảng."),
                    ("loading", "boolean", "Cờ trạng thái tải dữ liệu danh sách."),
                    ("searchTerm / appliedSearchTerm", "string", "Từ khóa đang nhập và từ khóa đã debounce để gửi sang API."),
                    ("statusFilter", "'ALL' | 'ACTIVE' | 'LOCKED'", "Bộ lọc trạng thái tài khoản bệnh nhân."),
                    ("currentPage / pageSize / totalPages", "number", "Điều khiển phân trang và kích thước trang hiển thị."),
                ],
                "methods": [
                    ("fetchPatients", "state phân trang + filter", "Promise<void>", "Gọi patientService.getAdminPatients, cập nhật patients, totalPages và totalElements."),
                    ("handlePageSizeChange", "ChangeEvent<HTMLSelectElement>", "void", "Thay đổi số lượng bản ghi mỗi trang và kích hoạt tải lại dữ liệu."),
                    ("setCurrentPage", "page number", "void", "Điều hướng giữa các trang trong danh sách customer accounts."),
                ],
            },
            {
                "name": "patientService",
                "description": "Singleton object phía frontend nhóm các lời gọi REST cho chức năng bệnh nhân. Trong use case xem danh sách, object này chịu trách nhiệm gọi API /admin/patients/list và trả PageResponse đã unwrap.",
                "base_class": "Plain object singleton export",
                "constructor": "Không có constructor; các method được export trực tiếp trong ES module.",
                "prototype": "getAdminPatients(page, size, keyword, sortBy, sortDir, isActive): Promise<PageResponse<AdminPatientRecord>>",
                "source_file": "clinic-management-fe/src/services/patient.service.ts",
                "namespace": "ES module @/services/patient.service",
                "attributes": [("N/A", "N/A", "Service wrapper không lưu state nội bộ.")],
                "methods": [
                    ("getAdminPatients", "page, size, keyword, sortBy, sortDir, isActive", "Promise<PageResponse<AdminPatientRecord>>", "Gửi GET /admin/patients/list với query string để lấy danh sách phân trang."),
                    ("getAdminPatientById", "userId", "Promise<AdminPatientRecord>", "Lấy chi tiết một bệnh nhân để dùng lại ở các use case khác trong cùng màn hình."),
                ],
            },
            {
                "name": "PatientManagementController",
                "description": "REST controller backend cho nhóm API quản lý bệnh nhân. Endpoint getAllPatients chịu trách nhiệm nhận tham số phân trang, sắp xếp, keyword và isActive từ frontend rồi ủy quyền sang service layer.",
                "base_class": "Spring @RestController",
                "constructor": "Lombok @RequiredArgsConstructor inject PatientManagementService.",
                "prototype": "public ResponseEntity<PageResponse<UserDetailResponse>> getAllPatients(int page, int size, String sortBy, String sortDir, String keyword, Boolean isActive)",
                "source_file": "BE/src/main/java/g2/be/controller/PatientManagementController.java",
                "namespace": "g2.be.controller",
                "attributes": [("patientManagementService", "PatientManagementService", "Business service xử lý nghiệp vụ quản lý bệnh nhân.")],
                "methods": [
                    ("getAllPatients", "page, size, sortBy, sortDir, keyword, isActive", "ResponseEntity<PageResponse<UserDetailResponse>>", "Expose GET /api/admin/patients/list với role guard cho các vai trò nội bộ."),
                ],
            },
            {
                "name": "PatientManagementService",
                "description": "Service nghiệp vụ lọc đúng các user có ROLE_PATIENT, áp dụng keyword hoặc isActive, sau đó map kết quả Page<User> sang PageResponse<UserDetailResponse> để trả cho frontend.",
                "base_class": "Spring @Service",
                "constructor": "Lombok @RequiredArgsConstructor inject UserRepository, PatientRepository, RoleRepository, PasswordEncoder, EmailService.",
                "prototype": "public PageResponse<UserDetailResponse> getAllPatients(int page, int size, String sortBy, String sortDir, String keyword, Boolean isActive)",
                "source_file": "BE/src/main/java/g2/be/service/PatientManagementService.java",
                "namespace": "g2.be.service",
                "attributes": [
                    ("userRepository", "UserRepository", "Truy vấn danh sách user có role PATIENT và trạng thái tương ứng."),
                    ("patientRepository", "PatientRepository", "Truy xuất hồ sơ patient khi cần ghép dữ liệu chi tiết."),
                    ("ROLE_PATIENT", "String", "Hằng lọc role bệnh nhân trong toàn bộ luồng danh sách."),
                ],
                "methods": [
                    ("getAllPatients", "page, size, sortBy, sortDir, keyword, isActive", "PageResponse<UserDetailResponse>", "Chọn đúng repository query theo keyword hoặc isActive rồi map kết quả sang DTO trả về."),
                    ("getPatientById", "userId", "UserDetailResponse", "Dùng khi người dùng cần xem sâu một bệnh nhân trong cùng module quản lý."),
                ],
            },
        ],
    },
    {
        "title": "Cancel customer information",
        "screen_name": "Cancel Customer Information",
        "screen_rows": [
            ("1", "Delete button", "Button", "Yes", "N/A", "Nút Xóa trên từng dòng bệnh nhân mở modal xác nhận xóa mềm customer information."),
            ("2", "Delete confirmation modal", "Modal", "Yes", "N/A", "Hiển thị bệnh nhân được chọn và yêu cầu người dùng xác nhận trước khi gọi API DELETE."),
            ("3", "Confirm delete button", "Button", "Yes", "N/A", "Sau khi bấm, FE gọi patientService.deleteAdminPatient(userId) và tải lại danh sách khi thành công."),
            ("4", "Error/success message", "Toast / alert", "No", "N/A", "Hiển thị lỗi nghiệp vụ như bệnh nhân đã bị xóa hoặc thông báo xóa thành công."),
        ],
        "diagram_note": "Delete flow in current source is a soft delete only. The account is marked isDeleted = true and isActive = false; no physical delete is executed in database.",
        "classes": [
            {
                "name": "PatientManagementPage",
                "description": "Trong use case hủy thông tin khách hàng, cùng component trang quản lý bệnh nhân giữ selected patient cần xóa, mở modal xác nhận và gọi hàm xóa mềm khi người dùng đồng ý.",
                "base_class": "React function component",
                "constructor": "Không có constructor OOP; delete state được giữ bằng useState.",
                "prototype": "export default function PatientManagementPage(): JSX.Element",
                "source_file": "clinic-management-fe/app/dashboard/admin/patients/page.tsx",
                "namespace": "Next.js App Router page module",
                "attributes": [
                    ("deleteModal", "{ isOpen: boolean; patient: AdminPatientRecord | null }", "State giữ modal xác nhận xóa và bệnh nhân được chọn."),
                    ("saving", "boolean", "Cờ cho biết đang gửi request xóa xuống backend."),
                    ("patients", "AdminPatientRecord[]", "Danh sách được refresh lại sau khi xóa thành công."),
                ],
                "methods": [
                    ("setDeleteModal", "patient or null", "void", "Mở/đóng modal xác nhận xóa."),
                    ("handleConfirmDelete", "N/A", "Promise<void>", "Gọi patientService.deleteAdminPatient, đóng modal và tải lại danh sách."),
                ],
            },
            {
                "name": "patientService",
                "description": "Frontend service wrapper đóng gói DELETE /admin/patients/{userId}. Đây là điểm giao tiếp trực tiếp giữa giao diện và backend trong luồng cancel customer information.",
                "base_class": "Plain object singleton export",
                "constructor": "Không có constructor.",
                "prototype": "deleteAdminPatient(userId: number): Promise<void>",
                "source_file": "clinic-management-fe/src/services/patient.service.ts",
                "namespace": "ES module @/services/patient.service",
                "attributes": [("N/A", "N/A", "Không lưu state nội bộ.")],
                "methods": [
                    ("deleteAdminPatient", "userId", "Promise<void>", "Gửi DELETE /admin/patients/{userId} cho luồng xóa mềm bệnh nhân."),
                    ("lockAdminPatient", "userId", "Promise<void>", "Method liên quan cùng module trạng thái bệnh nhân nhưng không trực tiếp dùng trong xóa."),
                ],
            },
            {
                "name": "PatientManagementController",
                "description": "REST controller nhận yêu cầu DELETE từ frontend và trả MessageResponse sau khi service hoàn tất xóa mềm bệnh nhân.",
                "base_class": "Spring @RestController",
                "constructor": "Lombok @RequiredArgsConstructor inject PatientManagementService.",
                "prototype": "public ResponseEntity<MessageResponse> deletePatient(Long patientId)",
                "source_file": "BE/src/main/java/g2/be/controller/PatientManagementController.java",
                "namespace": "g2.be.controller",
                "attributes": [("patientManagementService", "PatientManagementService", "Service xử lý nghiệp vụ xóa mềm bệnh nhân.")],
                "methods": [
                    ("deletePatient", "patientId", "ResponseEntity<MessageResponse>", "Expose DELETE /api/admin/patients/{patientId} với role guard SYSTEM_ADMIN và CASHIER."),
                ],
            },
            {
                "name": "PatientManagementService",
                "description": "Service thực hiện soft delete đối với user có ROLE_PATIENT: kiểm tra tồn tại, kiểm tra chưa bị xóa, sau đó set isDeleted = true và isActive = false trước khi lưu lại.",
                "base_class": "Spring @Service",
                "constructor": "Lombok @RequiredArgsConstructor inject UserRepository và các dependency cùng module.",
                "prototype": "public MessageResponse deletePatient(Long userId)",
                "source_file": "BE/src/main/java/g2/be/service/PatientManagementService.java",
                "namespace": "g2.be.service",
                "attributes": [
                    ("userRepository", "UserRepository", "Tìm user theo id và lưu lại cờ isDeleted/isActive sau khi cập nhật."),
                    ("ROLE_PATIENT", "String", "Bảo đảm chỉ account bệnh nhân mới đi qua nhánh xóa mềm này."),
                ],
                "methods": [
                    ("deletePatient", "userId", "MessageResponse", "Kiểm tra tồn tại, kiểm tra role, đánh dấu xóa mềm và trả thông báo thành công."),
                    ("lockPatient", "userId", "MessageResponse", "Phương thức trạng thái liên quan, cùng thao tác với isActive nhưng không đặt isDeleted."),
                ],
            },
            {
                "name": "MessageResponse",
                "description": "DTO phản hồi đơn giản của backend cho các thao tác dạng command như xóa, khóa, mở khóa. Trong luồng xóa mềm, object này chỉ chứa message để frontend hiển thị kết quả.",
                "base_class": "POJO DTO",
                "constructor": "Lombok @Data, @NoArgsConstructor, @AllArgsConstructor, @Builder.",
                "prototype": "public class MessageResponse",
                "source_file": "BE/src/main/java/g2/be/dto/response/MessageResponse.java",
                "namespace": "g2.be.dto.response",
                "attributes": [("message", "String", "Nội dung phản hồi như 'Xóa bệnh nhân thành công'.")],
                "methods": [("N/A", "N/A", "N/A", "DTO chỉ mang dữ liệu phản hồi, không có hành vi nghiệp vụ riêng.")],
            },
        ],
    },
    {
        "title": "Update appointment and customer information",
        "screen_name": "Update Appointment and Customer Information",
        "screen_rows": [
            ("1", "Edit button", "Button", "Yes", "N/A", "Nút Sửa trên màn admin patients gọi openEditModal để tải dữ liệu chi tiết bệnh nhân vào modal."),
            ("2", "Patient edit modal", "Modal form", "Yes", "Nhiều field", "Cho phép cập nhật fullName, email, SĐT, CCCD, ngày sinh, BHYT, chiều cao, cân nặng, dị ứng, địa chỉ và thông tin liên hệ khẩn cấp."),
            ("3", "Save changes button", "Button", "Yes", "N/A", "Gửi dữ liệu từ PatientFormModal sang patientService.updateAdminPatient để cập nhật thông tin customer."),
            ("4", "Appointment update API", "Service/API", "No", "N/A", "Hệ thống hiện có appointmentService.updateAppointment và backend PUT /api/appointments/{appointmentId}, nhưng source FE hiện chưa có page riêng gọi luồng này."),
            ("5", "Result message", "Toast / alert", "No", "N/A", "Hiển thị lỗi trùng email/SĐT/CCCD hoặc thông báo cập nhật thành công sau khi lưu."),
        ],
        "diagram_note": "Current implementation note: customer information update is fully wired in FE and BE. Appointment update is exposed by src/services/appointment.service.ts and backend AppointmentController/AppointmentService, but no current page in app/ or src/ invokes it. In addition, the FE UpdateAppointmentRequest interface declares notes, while backend UpdateAppointmentRequest currently does not include notes.",
        "classes": [
            {
                "name": "PatientFormModal",
                "description": "Modal form thực tế dùng để sửa thông tin khách hàng. Component này build form state từ AdminPatientRecord ban đầu, cho phép người dùng chỉnh sửa các field bệnh nhân rồi submit về trang cha.",
                "base_class": "React function component",
                "constructor": "Không có constructor OOP; formValues được giữ bằng useState và đồng bộ qua useEffect.",
                "prototype": "export function PatientFormModal(props: PatientFormModalProps): JSX.Element",
                "source_file": "clinic-management-fe/app/dashboard/admin/patients/PatientFormModal.tsx",
                "namespace": "Next.js client component module",
                "attributes": [
                    ("formValues", "PatientFormValues", "Toàn bộ dữ liệu form chỉnh sửa customer information."),
                    ("initialData", "AdminPatientRecord | null", "Dữ liệu chi tiết bệnh nhân ban đầu để đổ vào modal."),
                    ("mode", "'create' | 'edit'", "Điều khiển nhãn nút và tiêu đề modal."),
                    ("loading", "boolean", "Khóa nút submit khi đang gửi request cập nhật."),
                ],
                "methods": [
                    ("buildFormValues", "initialData", "PatientFormValues", "Chuẩn hóa dữ liệu chi tiết bệnh nhân thành state hiển thị của modal."),
                    ("handleChange", "field, value", "void", "Cập nhật từng field của formValues khi người dùng nhập liệu."),
                    ("handleSubmit", "N/A", "Promise<void>", "Gửi formValues hiện tại lên callback onSubmit của trang cha."),
                ],
            },
            {
                "name": "PatientManagementPage",
                "description": "Trang cha điều phối luồng cập nhật customer information: tải chi tiết bệnh nhân, mở modal edit và gửi payload cập nhật sang backend sau khi người dùng nhấn lưu.",
                "base_class": "React function component",
                "constructor": "Không có constructor OOP; edit state và selected patient giữ bằng useState.",
                "prototype": "export default function PatientManagementPage(): JSX.Element",
                "source_file": "clinic-management-fe/app/dashboard/admin/patients/page.tsx",
                "namespace": "Next.js App Router page module",
                "attributes": [
                    ("editingPatient", "AdminPatientRecord | null", "Bệnh nhân đang được chỉnh sửa trong modal."),
                    ("saving", "boolean", "Cờ cho biết luồng load detail hoặc submit update đang chạy."),
                    ("error", "string | null", "Thông báo lỗi nghiệp vụ hiển thị lên trang nếu cập nhật thất bại."),
                ],
                "methods": [
                    ("openEditModal", "patient", "Promise<void>", "Gọi patientService.getAdminPatientById rồi mở modal với dữ liệu chi tiết mới nhất."),
                    ("handleUpdatePatient", "values", "Promise<void>", "Chuyển form values thành mutation payload và gọi patientService.updateAdminPatient."),
                ],
            },
            {
                "name": "patientService",
                "description": "FE service wrapper cho nhánh cập nhật thông tin khách hàng. Nó tách luồng lấy chi tiết bệnh nhân và cập nhật bệnh nhân thành hai lời gọi REST độc lập.",
                "base_class": "Plain object singleton export",
                "constructor": "Không có constructor.",
                "prototype": "getAdminPatientById(userId): Promise<AdminPatientRecord>; updateAdminPatient(userId, data): Promise<AdminPatientRecord>",
                "source_file": "clinic-management-fe/src/services/patient.service.ts",
                "namespace": "ES module @/services/patient.service",
                "attributes": [("N/A", "N/A", "Không lưu state nội bộ.")],
                "methods": [
                    ("getAdminPatientById", "userId", "Promise<AdminPatientRecord>", "Gửi GET /admin/patients/{userId} để nạp dữ liệu edit."),
                    ("updateAdminPatient", "userId, AdminPatientMutationPayload", "Promise<AdminPatientRecord>", "Gửi PUT /admin/patients/{userId} để cập nhật customer information."),
                ],
            },
            {
                "name": "PatientManagementController",
                "description": "REST controller backend nhận UpdatePatientRequest từ giao diện và trả lại UserDetailResponse sau khi service cập nhật dữ liệu user + patient profile.",
                "base_class": "Spring @RestController",
                "constructor": "Lombok @RequiredArgsConstructor inject PatientManagementService.",
                "prototype": "public ResponseEntity<UserDetailResponse> updatePatient(Long patientId, UpdatePatientRequest request)",
                "source_file": "BE/src/main/java/g2/be/controller/PatientManagementController.java",
                "namespace": "g2.be.controller",
                "attributes": [("patientManagementService", "PatientManagementService", "Business service xử lý cập nhật customer information." )],
                "methods": [
                    ("updatePatient", "patientId, UpdatePatientRequest", "ResponseEntity<UserDetailResponse>", "Expose PUT /api/admin/patients/{patientId} cho RECEPTIONIST, CASHIER và SYSTEM_ADMIN."),
                ],
            },
            {
                "name": "PatientManagementService",
                "description": "Service cập nhật customer information thật sự trong backend. Nó kiểm tra trùng email/SĐT/CCCD, cập nhật bảng users và patient profile, sau đó trả lại UserDetailResponse đã map từ entity mới nhất.",
                "base_class": "Spring @Service",
                "constructor": "Lombok @RequiredArgsConstructor inject UserRepository, PatientRepository và các dependency cùng module.",
                "prototype": "public UserDetailResponse updatePatient(Long userId, UpdatePatientRequest request)",
                "source_file": "BE/src/main/java/g2/be/service/PatientManagementService.java",
                "namespace": "g2.be.service",
                "attributes": [
                    ("userRepository", "UserRepository", "Dùng để kiểm tra trùng lặp và lưu lại user sau chỉnh sửa."),
                    ("patientRepository", "PatientRepository", "Lưu hồ sơ bệnh nhân sau khi các field chuyên biệt được cập nhật."),
                ],
                "methods": [
                    ("updatePatient", "userId, UpdatePatientRequest", "UserDetailResponse", "Kiểm tra role PATIENT, validate uniqueness rồi lưu User và Patient."),
                ],
            },
            {
                "name": "appointmentService",
                "description": "FE wrapper đã khai báo hợp đồng update appointment bằng PUT /appointments/{appointmentId}. Tuy nhiên ở code frontend hiện tại method này chưa được page/component nào trong app/ hoặc src/ gọi trực tiếp.",
                "base_class": "Plain object singleton export",
                "constructor": "Không có constructor.",
                "prototype": "updateAppointment(appointmentId: string, data: UpdateAppointmentRequest): Promise<AppointmentItem>",
                "source_file": "clinic-management-fe/src/services/appointment.service.ts",
                "namespace": "ES module @/services/appointment.service",
                "attributes": [("N/A", "N/A", "Không lưu state nội bộ.")],
                "methods": [
                    ("updateAppointment", "appointmentId, UpdateAppointmentRequest", "Promise<AppointmentItem>", "Gửi PUT /appointments/{appointmentId}; interface FE có field notes nhưng hiện backend không nhận field này."),
                    ("getAppointmentDetail", "appointmentId", "Promise<AppointmentDetail>", "Dùng để lấy chi tiết lịch hẹn phục vụ các màn khác trong module lễ tân/cashier/bác sĩ."),
                ],
            },
            {
                "name": "AppointmentController",
                "description": "REST controller của lịch hẹn. Endpoint updateAppointment cho phép cập nhật ngày giờ, trạng thái, bác sĩ, phòng và danh sách dịch vụ của một appointment hiện có.",
                "base_class": "Spring @RestController",
                "constructor": "Lombok @RequiredArgsConstructor inject AppointmentService.",
                "prototype": "public ApiResponse<AppointmentResponse> updateAppointment(String appointmentId, UpdateAppointmentRequest request)",
                "source_file": "BE/src/main/java/g2/be/controller/AppointmentController.java",
                "namespace": "g2.be.controller",
                "attributes": [("appointmentService", "AppointmentService", "Service nghiệp vụ xử lý create/update/check-in/complete cho lịch hẹn." )],
                "methods": [
                    ("updateAppointment", "appointmentId, UpdateAppointmentRequest", "ApiResponse<AppointmentResponse>", "Expose PUT /api/appointments/{appointmentId} cho RECEPTIONIST, SYSTEM_ADMIN và CASHIER."),
                ],
            },
            {
                "name": "AppointmentService",
                "description": "Service backend cập nhật appointment theo dữ liệu request. Logic hiện tại cho phép sửa appointmentDate, appointmentTime, status, doctorId, roomId và serviceIds; sau đó tự resolve lại phòng nếu các thông tin điều phối thay đổi.",
                "base_class": "Spring @Service",
                "constructor": "Lombok @RequiredArgsConstructor inject AppointmentRepository, DoctorRepository, RoomRepository, StaffScheduleRepository và các repository liên quan.",
                "prototype": "public AppointmentResponse updateAppointment(String appointmentId, UpdateAppointmentRequest request)",
                "source_file": "BE/src/main/java/g2/be/service/AppointmentService.java",
                "namespace": "g2.be.service",
                "attributes": [
                    ("appointmentRepository", "AppointmentRepository", "Tìm và lưu appointment cần cập nhật."),
                    ("doctorRepository", "DoctorRepository", "Resolve doctor mới nếu request có doctorId."),
                    ("roomRepository / staffScheduleRepository", "Repository", "Dùng để resolve lại phòng phù hợp theo doctor/date/time/room."),
                ],
                "methods": [
                    ("updateAppointment", "appointmentId, UpdateAppointmentRequest", "AppointmentResponse", "Cập nhật các field được hỗ trợ, validate lại bác sĩ-dịch vụ và lưu appointment đã thay đổi."),
                    ("getAppointmentDetail", "appointmentId", "AppointmentDetailResponse", "Dùng để tra cứu chi tiết khi cần kiểm tra lại lịch hẹn sau cập nhật."),
                ],
            },
        ],
    },
    {
        "title": "Create appointment for customer (record expected visit)",
        "screen_name": "Create Appointment for Customer",
        "screen_rows": [
            ("1", "Patient search", "Search input + dropdown", "Yes", "Free text", "Tìm bệnh nhân theo tên, mã BN hoặc số điện thoại; chọn một bệnh nhân trước khi tạo lịch hẹn."),
            ("2", "Appointment date", "Date input", "Yes", "YYYY-MM-DD", "Chọn ngày khám; giá trị nhỏ nhất là ngày hiện tại theo local date của trình duyệt."),
            ("3", "Service checklist", "Multi-select list", "Yes", ">= 1 service", "Chọn một hoặc nhiều hạng mục khám; dùng catalogService.getServices('CLINICAL') để nạp dữ liệu."),
            ("4", "Available slots", "Radio/list selector", "Yes", "Theo slot trả về", "Sau khi có ngày khám và serviceIds, FE gọi appointmentService.getAvailableSlots để lấy ca còn chỗ."),
            ("5", "Clinical room selector", "Select", "Conditional", "N/A", "Chỉ hiển thị khi danh sách dịch vụ có 'Khám Nội khoa tổng quát'; FE gọi appointmentService.getClinicalRooms để chọn phòng/bác sĩ trực ca."),
            ("6", "Notes", "Textarea", "No", "Free text", "Lưu ghi chú hoặc lý do khám vào request tạo lịch hẹn."),
            ("7", "Create button", "Button", "Yes", "N/A", "Gửi CreateAppointmentRequest lên backend và chuyển hướng về danh sách lịch hẹn lễ tân khi thành công."),
        ],
        "diagram_note": "The current receptionist implementation creates appointments through a dedicated page at app/dashboard/receptionist/create-appointment/page.tsx. This page also queries available slots and clinical rooms before it actually calls POST /api/appointments.",
        "classes": [
            {
                "name": "CreateAppointmentPage",
                "description": "Trang lễ tân tạo lịch hẹn cho khách hàng. Component này điều phối tìm bệnh nhân, chọn dịch vụ, tính điều kiện hiển thị phòng/bác sĩ, gọi API lấy ca trống và cuối cùng gửi request tạo appointment.",
                "base_class": "React function component",
                "constructor": "Không có constructor OOP; state điều phối luồng tạo lịch được quản lý bằng useState/useEffect/useCallback.",
                "prototype": "export default function CreateAppointmentPage(): JSX.Element",
                "source_file": "clinic-management-fe/app/dashboard/receptionist/create-appointment/page.tsx",
                "namespace": "Next.js App Router page module",
                "attributes": [
                    ("patientId / selectedPatient", "string / PatientResponse | null", "Bệnh nhân được chọn cho lịch hẹn mới."),
                    ("selectedDate / selectedSlot", "string / AvailableSlot | null", "Ngày khám và ca khám đã chọn."),
                    ("selectedServices", "string[]", "Danh sách serviceId được chọn cho yêu cầu khám."),
                    ("clinicalRooms / selectedRoomId", "ClinicalRoom[] / string", "Danh sách phòng-bác sĩ trực ca và lựa chọn phòng hiện tại."),
                    ("notes", "string", "Ghi chú được gửi cùng CreateAppointmentRequest."),
                ],
                "methods": [
                    ("searchPatients", "keyword", "Promise<void>", "Debounce tìm bệnh nhân qua patientService.getPatients và hiển thị dropdown kết quả."),
                    ("toggleService", "serviceId", "void", "Thêm hoặc bỏ dịch vụ khỏi selectedServices và reset selectedRoomId khi cần."),
                    ("handleCreate", "N/A", "Promise<void>", "Validate bắt buộc rồi gọi appointmentService.createAppointment để tạo lịch hẹn."),
                ],
            },
            {
                "name": "patientService",
                "description": "FE wrapper cung cấp khả năng tìm bệnh nhân trong lúc lễ tân tạo lịch hẹn. Trang create appointment dùng method getPatients để lấy candidate list từ backend.",
                "base_class": "Plain object singleton export",
                "constructor": "Không có constructor.",
                "prototype": "getPatients(page, size, keyword, sortBy, sortDir): Promise<PageResponse<PatientResponse>>",
                "source_file": "clinic-management-fe/src/services/patient.service.ts",
                "namespace": "ES module @/services/patient.service",
                "attributes": [("N/A", "N/A", "Không lưu state nội bộ.")],
                "methods": [
                    ("getPatients", "page, size, keyword, sortBy, sortDir", "Promise<PageResponse<PatientResponse>>", "Gửi request tìm bệnh nhân phục vụ dropdown chọn customer trước khi tạo lịch."),
                ],
            },
            {
                "name": "catalogService",
                "description": "FE service export từ payment.service.ts nhưng được tái sử dụng trong màn tạo lịch để nạp danh mục dịch vụ lâm sàng. Đây là nguồn dữ liệu chính cho service checklist của form tạo lịch.",
                "base_class": "Plain object singleton export",
                "constructor": "Không có constructor.",
                "prototype": "getServices(category?: ServiceCategory, deptId?: number): Promise<ServiceItem[]>",
                "source_file": "clinic-management-fe/src/services/payment.service.ts",
                "namespace": "ES module @/services/payment.service",
                "attributes": [("N/A", "N/A", "Không lưu state nội bộ.")],
                "methods": [
                    ("getServices", "category = 'CLINICAL'", "Promise<ServiceItem[]>", "Lấy danh mục dịch vụ dùng cho lựa chọn hạng mục khám khi tạo appointment."),
                ],
            },
            {
                "name": "appointmentService",
                "description": "FE wrapper chịu trách nhiệm gọi toàn bộ API cần thiết cho luồng tạo lịch: lấy ca trống, lấy phòng khám/bác sĩ trực và gửi request POST tạo appointment.",
                "base_class": "Plain object singleton export",
                "constructor": "Không có constructor.",
                "prototype": "createAppointment(data), getAvailableSlots(date, serviceIds), getClinicalRooms(slotId, date)",
                "source_file": "clinic-management-fe/src/services/appointment.service.ts",
                "namespace": "ES module @/services/appointment.service",
                "attributes": [("N/A", "N/A", "Không lưu state nội bộ.")],
                "methods": [
                    ("createAppointment", "CreateAppointmentRequest", "Promise<AppointmentItem>", "Gửi POST /appointments để tạo appointment cho customer."),
                    ("getAvailableSlots", "date, serviceIds", "Promise<AvailableSlot[]>", "Lấy danh sách ca còn chỗ theo ngày và dịch vụ đã chọn."),
                    ("getClinicalRooms", "slotId, date", "Promise<ClinicalRoom[]>", "Lấy danh sách phòng nội khoa có bác sĩ trực cho ca được chọn."),
                ],
            },
            {
                "name": "AppointmentController",
                "description": "REST controller backend nhận CreateAppointmentRequest từ màn lễ tân và trả AppointmentResponse khi tạo thành công.",
                "base_class": "Spring @RestController",
                "constructor": "Lombok @RequiredArgsConstructor inject AppointmentService.",
                "prototype": "public ApiResponse<AppointmentResponse> createAppointment(CreateAppointmentRequest request)",
                "source_file": "BE/src/main/java/g2/be/controller/AppointmentController.java",
                "namespace": "g2.be.controller",
                "attributes": [("appointmentService", "AppointmentService", "Service nghiệp vụ cho create/update/check-in appointment.")],
                "methods": [
                    ("createAppointment", "CreateAppointmentRequest", "ApiResponse<AppointmentResponse>", "Expose POST /api/appointments cho RECEPTIONIST, SYSTEM_ADMIN và CASHIER."),
                ],
            },
            {
                "name": "AppointmentService",
                "description": "Service backend tạo appointment thật sự. Logic hiện tại kiểm tra patientId, doctorId (nếu có), resolve phòng, nạp danh sách service còn hợp lệ, validate tương thích bác sĩ-dịch vụ và kiểm tra capacity ca khám trước khi lưu.",
                "base_class": "Spring @Service",
                "constructor": "Lombok @RequiredArgsConstructor inject AppointmentRepository, PatientRepository, DoctorRepository, RoomRepository, StaffScheduleRepository và các repository liên quan.",
                "prototype": "public AppointmentResponse createAppointment(CreateAppointmentRequest request)",
                "source_file": "BE/src/main/java/g2/be/service/AppointmentService.java",
                "namespace": "g2.be.service",
                "attributes": [
                    ("patientRepository", "PatientRepository", "Dùng để kiểm tra và nạp patient trước khi tạo appointment."),
                    ("doctorRepository", "DoctorRepository", "Resolve bác sĩ nếu request có doctorId."),
                    ("roomRepository / staffScheduleRepository", "Repository", "Resolve phòng và lịch trực phù hợp cho ca khám."),
                    ("DEFAULT_MAX_PATIENTS", "int", "Giá trị fallback capacity khi dữ liệu slot cũ chưa có maxPatients rõ ràng."),
                ],
                "methods": [
                    ("createAppointment", "CreateAppointmentRequest", "AppointmentResponse", "Tạo appointmentId mới, validate slot capacity rồi lưu Appointment với danh sách dịch vụ đã chọn."),
                    ("validateSlotCapacity", "date, time, services, room", "void", "Kiểm tra số lượng bệnh nhân tối đa trong ca/phòng trước khi cho phép tạo lịch."),
                    ("resolveAppointmentRoom", "roomId, doctor, date, time", "Room", "Chọn phòng phù hợp theo đầu vào và lịch làm việc hiện tại."),
                ],
            },
            {
                "name": "CreateAppointmentRequest",
                "description": "DTO đầu vào cho POST /api/appointments. Object này mang bệnh nhân, bác sĩ/phòng (nếu có), danh sách dịch vụ, ngày giờ khám, trạng thái khởi tạo và ghi chú.",
                "base_class": "POJO DTO",
                "constructor": "Lombok @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder.",
                "prototype": "public class CreateAppointmentRequest",
                "source_file": "BE/src/main/java/g2/be/dto/request/CreateAppointmentRequest.java",
                "namespace": "g2.be.dto.request",
                "attributes": [
                    ("patientId", "String", "Mã bệnh nhân được chọn trên form lễ tân."),
                    ("doctorId", "String", "Mã bác sĩ nếu người dùng chọn phòng/bác sĩ cụ thể."),
                    ("roomId", "String", "Mã phòng khám nội khoa hoặc phòng được gán thủ công."),
                    ("serviceIds", "List<String>", "Danh sách các hạng mục khám được chọn."),
                    ("appointmentDate / appointmentTime", "LocalDate / LocalTime", "Ngày và giờ khám dự kiến."),
                    ("status / notes", "String / String", "Trạng thái khởi tạo và ghi chú đi kèm appointment."),
                ],
                "methods": [("N/A", "N/A", "N/A", "DTO dữ liệu thuần dùng cho request tạo lịch hẹn.")],
            },
        ],
    },
    {
        "title": "Collect prepayment for general examination (cash/QR)",
        "screen_name": "Collect Prepayment for General Examination",
        "screen_rows": [
            ("1", "CCCD input", "Input text", "No", "CCCD", "Thu ngân nhập CCCD nếu muốn tra BHYT và áp giảm trừ trước khi thanh toán."),
            ("2", "Payment method selector", "Button / toggle", "Yes", "CASH or QR", "Chọn thanh toán tiền mặt hoặc QR trước khi gửi request thu tạm ứng."),
            ("3", "Pay button", "Button", "Yes", "N/A", "Gọi paymentService.collectPrepayment với appointmentId, paymentMethod và cccdNumber nếu có."),
            ("4", "QR payment area", "QR image / panel", "Conditional", "N/A", "Hiển thị paymentUrl trả về từ backend khi chọn QR và bắt đầu polling trạng thái thanh toán."),
            ("5", "Receipt detail panel", "Read-only form", "Yes", "N/A", "Hiển thị thông tin bệnh nhân, bác sĩ, dịch vụ, tiền BHYT, tiền bệnh nhân và trạng thái thanh toán."),
            ("6", "Export receipt button", "Button", "No", "N/A", "Cho phép xuất phiếu thu sau khi đã có invoiceId hợp lệ."),
        ],
        "diagram_note": "Prepayment flow in current code is implemented on the cashier invoice detail page. CASH immediately marks payment as PAID and moves the appointment into waiting flow; QR returns a paymentUrl and the FE polls payment status until success/failure.",
        "classes": [
            {
                "name": "InvoiceDetailContent",
                "description": "Component FE chính của màn thu tạm ứng tại quầy. Nó tải chi tiết phiếu khám, cho phép nhập CCCD, chọn CASH/QR, gọi API prepayment và theo dõi kết quả thanh toán để in phiếu thu.",
                "base_class": "React function component",
                "constructor": "Không có constructor OOP; state và polling interval được quản lý bằng hooks.",
                "prototype": "function InvoiceDetailContent({ id }: { id: string }): JSX.Element",
                "source_file": "clinic-management-fe/app/dashboard/cashier/invoices/[id]/page.tsx",
                "namespace": "Next.js client component module",
                "attributes": [
                    ("detail", "FinancialDetailResponse | null", "Dữ liệu phiếu khám/phiếu thu hiện tại đang hiển thị."),
                    ("paymentMethod", "'CASH' | 'QR'", "Phương thức thanh toán được thu ngân chọn."),
                    ("cccdInput", "string", "Giá trị CCCD nhập tay để tra BHYT."),
                    ("qrPaymentUrl", "string | null", "Đường dẫn ảnh QR trả về từ backend khi thanh toán QR."),
                    ("paymentStatusPollingRef", "Interval ref", "Interval kiểm tra trạng thái hóa đơn đang chờ thanh toán QR."),
                ],
                "methods": [
                    ("handleSwitchToQR", "N/A", "Promise<void>", "Khởi tạo thanh toán QR qua paymentService.collectPrepayment và bắt đầu polling invoice status."),
                    ("handlePayment", "N/A", "Promise<void>", "Khởi tạo thanh toán CASH; sau khi nhận invoiceId thì cũng kiểm tra trạng thái để đồng bộ UI."),
                    ("startPaymentStatusPolling", "invoiceId", "void", "Định kỳ gọi service tài chính kiểm tra paymentStatus cho đến khi PAID hoặc thất bại."),
                    ("handleExportReceipt", "N/A", "Promise<void>", "Xuất phiếu thu sau khi thanh toán thành công."),
                ],
            },
            {
                "name": "paymentService",
                "description": "FE singleton object đóng gói API thanh toán. Trong use case thu tạm ứng, method collectPrepayment là điểm gọi trực tiếp tới POST /payments/prepayment.",
                "base_class": "Plain object singleton export",
                "constructor": "Không có constructor.",
                "prototype": "collectPrepayment(data: PrepaymentRequest): Promise<PaymentInitiationResponse>",
                "source_file": "clinic-management-fe/src/services/payment.service.ts",
                "namespace": "ES module @/services/payment.service",
                "attributes": [("N/A", "N/A", "Không lưu state nội bộ.")],
                "methods": [
                    ("collectPrepayment", "PrepaymentRequest", "Promise<PaymentInitiationResponse>", "Gửi POST /payments/prepayment để khởi tạo thu tạm ứng lần đầu."),
                    ("supplementPayment", "SupplementPaymentRequest", "Promise<PaymentInitiationResponse>", "Dùng cho đợt thu bổ sung sau khám; cùng file nhưng khác luồng nghiệp vụ."),
                ],
            },
            {
                "name": "PaymentController",
                "description": "REST controller backend cho nhóm API thanh toán. Endpoint collectPrepayment nhận request từ cashier UI và trả PaymentResponse đã được business service xây dựng.",
                "base_class": "Spring @RestController",
                "constructor": "Lombok @RequiredArgsConstructor inject PaymentService.",
                "prototype": "public ApiResponse<PaymentResponse> collectPrepayment(PrepaymentRequest request)",
                "source_file": "BE/src/main/java/g2/be/controller/PaymentController.java",
                "namespace": "g2.be.controller",
                "attributes": [("paymentService", "PaymentService", "Service xử lý nghiệp vụ thu tiền và webhook." )],
                "methods": [
                    ("collectPrepayment", "PrepaymentRequest", "ApiResponse<PaymentResponse>", "Expose POST /api/payments/prepayment cho CASHIER và SYSTEM_ADMIN."),
                ],
            },
            {
                "name": "PaymentService",
                "description": "Service nghiệp vụ trung tâm cho thu tạm ứng. Nó kiểm tra hóa đơn đã thanh toán chưa, tính tổng tiền từ services trong appointment, tra BHYT nếu có CCCD, tạo MedicalRecord nếu thiếu, tạo Invoice và ServiceOrder cho từng dịch vụ; với CASH còn tự động gán bác sĩ/phòng tối ưu và chuyển appointment sang WAITING.",
                "base_class": "Spring @Service",
                "constructor": "Lombok @RequiredArgsConstructor inject AppointmentRepository, MedicalRecordRepository, InvoiceRepository, ServiceOrderRepository, CitizenInsuranceService, RoomRepository, LoadBalancingService.",
                "prototype": "public PaymentResponse collectPrepayment(PrepaymentRequest request)",
                "source_file": "BE/src/main/java/g2/be/service/PaymentService.java",
                "namespace": "g2.be.service",
                "attributes": [
                    ("appointmentRepository", "AppointmentRepository", "Nạp appointment và danh sách dịch vụ gắn với lịch hẹn."),
                    ("invoiceRepository", "InvoiceRepository", "Kiểm tra duplicate paid invoice và lưu hóa đơn vừa tạo."),
                    ("medicalRecordRepository", "MedicalRecordRepository", "Tạo mới hồ sơ khám nếu appointment chưa có medical record."),
                    ("citizenInsuranceService", "CitizenInsuranceService", "Tính breakdown BHYT khi cashier nhập CCCD hợp lệ."),
                    ("loadBalancingService", "LoadBalancingService", "Tự động gán bác sĩ/phòng tối ưu sau khi CASH đã thanh toán."),
                ],
                "methods": [
                    ("collectPrepayment", "PrepaymentRequest", "PaymentResponse", "Tính tiền, tạo hóa đơn/service orders, cập nhật appointment và trả thông tin paymentUrl hoặc trạng thái PAID."),
                    ("processSepayWebhook", "payload, authorizationHeader", "void", "Xử lý callback SePay cho các hóa đơn QR đang PENDING."),
                    ("autoAssignDoctor", "appointment", "void", "Cân bằng tải và gán bác sĩ/phòng khi thanh toán tiền mặt hoàn tất."),
                ],
            },
            {
                "name": "PrepaymentRequest",
                "description": "DTO request của POST /api/payments/prepayment. Backend không nhận amount từ FE; số tiền được tính lại từ services của appointment để tránh lệch dữ liệu hoặc gian lận tham số đầu vào.",
                "base_class": "POJO DTO",
                "constructor": "Lombok @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder.",
                "prototype": "public class PrepaymentRequest",
                "source_file": "BE/src/main/java/g2/be/dto/request/PrepaymentRequest.java",
                "namespace": "g2.be.dto.request",
                "attributes": [
                    ("appointmentId", "String", "Mã lịch hẹn cần thu tiền tạm ứng."),
                    ("paymentMethod", "String", "Phương thức thanh toán: CASH, QR, ..."),
                    ("cccdNumber", "String", "Tùy chọn; dùng để tra BHYT và tính giảm trừ nếu có."),
                ],
                "methods": [("N/A", "N/A", "N/A", "DTO request thuần cho API thu tạm ứng.")],
            },
            {
                "name": "PaymentResponse",
                "description": "DTO response trả về sau khi khởi tạo thu tiền. Object này chứa invoiceId, trạng thái thanh toán, số tiền bệnh nhân phải trả, breakdown BHYT và paymentUrl khi phương thức là QR/PENDING.",
                "base_class": "POJO DTO",
                "constructor": "Lombok @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder.",
                "prototype": "public class PaymentResponse",
                "source_file": "BE/src/main/java/g2/be/dto/response/PaymentResponse.java",
                "namespace": "g2.be.dto.response",
                "attributes": [
                    ("invoiceId", "String", "Mã hóa đơn tạo ra cho đợt thu tạm ứng."),
                    ("paymentStatus", "String", "PAID, PENDING hoặc FAILED."),
                    ("amount", "BigDecimal", "Số tiền bệnh nhân cần trả ở lần thanh toán này."),
                    ("totalServiceFee", "BigDecimal", "Tổng giá dịch vụ của appointment trước giảm trừ BHYT."),
                    ("insurancePaid / customerPaid", "BigDecimal", "Số tiền BHYT thanh toán và số tiền bệnh nhân phải trả."),
                    ("paymentUrl", "String", "URL ảnh QR/checkout được trả về khi thanh toán QR."),
                    ("insuranceBreakdown", "InsuranceBreakdownResponse", "Chi tiết giảm trừ BHYT nếu lookup thành công."),
                ],
                "methods": [("N/A", "N/A", "N/A", "DTO phản hồi dữ liệu thuần cho frontend cashier.")],
            },
        ],
    },
    {
        "title": "Quản lý dịch vụ (Admin Service Management C-U-D)",
        "screen_name": "Admin Service Management",
        "screen_rows": [
            ("1", "Search box", "Input text", "No", "Free text", "Tìm dịch vụ theo mã hoặc tên để lọc bảng danh mục dịch vụ."),
            ("2", "Department filter", "Select", "No", "Danh sách khoa", "Lọc dịch vụ theo khoa phụ trách bằng deptId đang chọn."),
            ("3", "Add service button", "Button", "No", "N/A", "Mở ServiceModal ở chế độ create để thêm dịch vụ mới."),
            ("4", "Services table", "Data table", "Yes", "Danh sách hiện tại", "Hiển thị mã dịch vụ, tên, giá, khoa, giới hạn BN/ca, trạng thái và action buttons."),
            ("5", "Edit modal", "Modal form", "Conditional", "N/A", "ServiceModal cho phép sửa tên dịch vụ, giá, khoa, BHYT code, mô tả, trạng thái hoạt động và maxPatients."),
            ("6", "Lock/Unlock button", "Button", "Conditional", "N/A", "Đổi trạng thái isActive của dịch vụ bằng PATCH /services/{serviceId}/status."),
            ("7", "Delete confirmation modal", "Modal", "Conditional", "N/A", "DeleteServiceModal gọi xóa mềm dịch vụ để loại khỏi danh sách admin và luồng tạo lịch khám."),
        ],
        "diagram_note": "Current admin service management is implemented as a complete C-U-D-like flow for service catalog with soft delete and status toggle. The page requests includeInactive=true so admin can see both active and locked services, but soft-deleted items are excluded.",
        "classes": [
            {
                "name": "AdminServicesPage",
                "description": "Trang admin quản lý danh mục dịch vụ. Component này nạp đồng thời danh sách khoa và danh sách dịch vụ, cho phép tìm kiếm/lọc, mở modal thêm-sửa và xử lý khóa/mở khóa từng dịch vụ.",
                "base_class": "React function component",
                "constructor": "Không có constructor OOP; toàn bộ trạng thái trang được giữ bằng useState/useEffect.",
                "prototype": "export default function AdminServicesPage(): JSX.Element",
                "source_file": "clinic-management-fe/app/dashboard/admin/services/page.tsx",
                "namespace": "Next.js App Router page module",
                "attributes": [
                    ("services", "ServiceDetail[]", "Danh sách dịch vụ chưa bị xóa mềm đang hiển thị cho admin."),
                    ("departments", "DepartmentResponse[]", "Danh sách khoa dùng cho bộ lọc và modal chọn khoa phụ trách."),
                    ("searchWord / selectedDept", "string", "Bộ lọc tìm kiếm và bộ lọc khoa áp dụng trên bảng."),
                    ("selectedService", "ServiceDetail | null", "Dịch vụ hiện đang được sửa hoặc chuẩn bị xóa mềm."),
                    ("processingServiceId", "string | null", "Đánh dấu dòng đang khóa/mở khóa để disable button tương ứng."),
                ],
                "methods": [
                    ("fetchData", "N/A", "Promise<void>", "Nạp đồng thời departmentService.getList và serviceCatalogService.getServices(true)."),
                    ("handleToggleStatus", "service, nextIsActive", "Promise<void>", "Gọi serviceCatalogService.updateStatus rồi refresh lại bảng."),
                ],
            },
            {
                "name": "ServiceModal",
                "description": "Modal thêm/sửa dịch vụ. Component dùng react-hook-form kết hợp zod để validate dữ liệu trước khi gọi create hoặc update trên serviceCatalogService.",
                "base_class": "React function component",
                "constructor": "Khởi tạo form bằng useForm + zodResolver; reset dữ liệu theo mode create/edit qua useEffect.",
                "prototype": "export function ServiceModal(props: ServiceModalProps): JSX.Element",
                "source_file": "clinic-management-fe/app/dashboard/admin/services/ServiceModal.tsx",
                "namespace": "Next.js client component module",
                "attributes": [
                    ("loading", "boolean", "Trạng thái submit form create/update dịch vụ."),
                    ("departments", "DepartmentResponse[]", "Danh sách khoa đổ vào select deptId trong form."),
                    ("service", "ServiceDetail | null", "Dịch vụ hiện đang sửa; null khi modal ở chế độ create."),
                ],
                "methods": [
                    ("onSubmit", "ServiceFormValues", "Promise<void>", "Chuẩn hóa dữ liệu và gọi serviceCatalogService.create hoặc serviceCatalogService.update."),
                    ("reset", "default values hoặc service hiện tại", "void", "Đưa form về trạng thái create hoặc edit tương ứng."),
                ],
            },
            {
                "name": "DeleteServiceModal",
                "description": "Modal xác nhận xóa mềm dịch vụ. Mục tiêu của component là chặn thao tác xóa nhầm, cảnh báo ảnh hưởng lên luồng tạo lịch khám và gọi serviceCatalogService.delete khi người dùng xác nhận.",
                "base_class": "React function component",
                "constructor": "Không có constructor OOP; loading state được giữ bằng useState.",
                "prototype": "export function DeleteServiceModal(props: DeleteServiceModalProps): JSX.Element",
                "source_file": "clinic-management-fe/app/dashboard/admin/services/DeleteServiceModal.tsx",
                "namespace": "Next.js client component module",
                "attributes": [
                    ("loading", "boolean", "Cờ cho biết request xóa mềm đang chạy."),
                    ("service", "ServiceDetail | null", "Dịch vụ người dùng chuẩn bị xóa mềm."),
                ],
                "methods": [
                    ("handleDelete", "N/A", "Promise<void>", "Gọi serviceCatalogService.delete, refresh lại trang và đóng modal khi thành công."),
                ],
            },
            {
                "name": "serviceCatalogService",
                "description": "Singleton object phía frontend gom các REST call cho danh mục dịch vụ admin: lấy danh sách, thêm mới, cập nhật, đổi trạng thái và xóa mềm dịch vụ.",
                "base_class": "Plain object singleton export",
                "constructor": "Không có constructor.",
                "prototype": "getServices(includeInactive), create(data), update(serviceId, data), updateStatus(serviceId, isActive), delete(serviceId)",
                "source_file": "clinic-management-fe/src/services/service-catalog.service.ts",
                "namespace": "ES module @/services/service-catalog.service",
                "attributes": [("N/A", "N/A", "Không lưu state nội bộ.")],
                "methods": [
                    ("getServices", "includeInactive", "Promise<ServiceDetail[]>", "Gửi GET /services?includeInactive=true để admin thấy cả dịch vụ đang khóa."),
                    ("create", "ServiceCatalogRequest", "Promise<ServiceDetail>", "Gửi POST /services để thêm dịch vụ mới."),
                    ("update", "serviceId, ServiceCatalogRequest", "Promise<ServiceDetail>", "Gửi PUT /services/{serviceId} để sửa dịch vụ hiện có."),
                    ("updateStatus", "serviceId, isActive", "Promise<ServiceDetail>", "Gửi PATCH /services/{serviceId}/status để khóa hoặc mở khóa dịch vụ."),
                    ("delete", "serviceId", "Promise<void>", "Gửi DELETE /services/{serviceId} để xóa mềm dịch vụ."),
                ],
            },
            {
                "name": "ServiceController",
                "description": "REST controller backend cho danh mục dịch vụ. Controller này vừa phục vụ endpoint đọc danh sách dịch vụ cho nhiều vai trò, vừa cung cấp create/update/status/delete cho admin và clinic manager.",
                "base_class": "Spring @RestController",
                "constructor": "Lombok @RequiredArgsConstructor inject ServiceRepository, DepartmentRepository, ServiceManagementService.",
                "prototype": "public ApiResponse<List<Service>> getAllServices(...); public ApiResponse<Service> createService(...); ...",
                "source_file": "BE/src/main/java/g2/be/controller/ServiceController.java",
                "namespace": "g2.be.controller",
                "attributes": [
                    ("serviceManagementService", "ServiceManagementService", "Business service xử lý create/update/status/delete nghiệp vụ."),
                    ("serviceRepository", "ServiceRepository", "Hỗ trợ endpoint updateServiceConfig và truy vấn service hiện tại."),
                    ("departmentRepository", "DepartmentRepository", "Resolve deptId về Department khi cập nhật cấu hình dịch vụ."),
                ],
                "methods": [
                    ("getAllServices", "category, deptId, includeInactive", "ApiResponse<List<Service>>", "Trả danh sách dịch vụ theo category/deptId, cho phép admin xem thêm dịch vụ đang khóa."),
                    ("createService", "ServiceCatalogRequest", "ApiResponse<Service>", "Expose POST /api/services cho admin/clinic manager."),
                    ("updateService", "serviceId, ServiceCatalogRequest", "ApiResponse<Service>", "Expose PUT /api/services/{serviceId}."),
                    ("updateServiceStatus", "serviceId, UpdateServiceStatusRequest", "ApiResponse<Service>", "Expose PATCH /api/services/{serviceId}/status."),
                    ("deleteService", "serviceId", "ApiResponse<Void>", "Expose DELETE /api/services/{serviceId} để xóa mềm dịch vụ."),
                ],
            },
            {
                "name": "ServiceManagementService",
                "description": "Service backend thực hiện toàn bộ nghiệp vụ chính của admin service management: phát sinh mã dịch vụ mới, kiểm tra khoa, validate maxPatients, cập nhật trạng thái, xóa mềm và lọc danh sách dịch vụ còn hiển thị.",
                "base_class": "Spring @Service",
                "constructor": "Lombok @RequiredArgsConstructor inject ServiceRepository và DepartmentRepository.",
                "prototype": "public Service createService(ServiceCatalogRequest request)",
                "source_file": "BE/src/main/java/g2/be/service/ServiceManagementService.java",
                "namespace": "g2.be.service",
                "attributes": [
                    ("serviceRepository", "ServiceRepository", "Lưu, truy xuất và lọc service theo isDeleted/isActive."),
                    ("departmentRepository", "DepartmentRepository", "Resolve deptId sang Department khi create/update service."),
                ],
                "methods": [
                    ("createService", "ServiceCatalogRequest", "Service", "Sinh serviceId mới theo mẫu S-xxx rồi lưu service mới."),
                    ("updateService", "serviceId, ServiceCatalogRequest", "Service", "Tìm service chưa bị xóa và cập nhật đầy đủ thông tin nghiệp vụ."),
                    ("updateServiceStatus", "serviceId, isActive", "Service", "Bật/tắt trạng thái hiển thị của dịch vụ mà không xóa mềm."),
                    ("deleteService", "serviceId", "void", "Đặt isActive = false và isDeleted = true để xóa mềm khỏi danh mục hiển thị."),
                    ("getVisibleServices", "includeInactive", "List<Service>", "Trả danh sách dịch vụ chưa bị xóa; tùy cờ có thể bao gồm cả dịch vụ đang khóa."),
                ],
            },
            {
                "name": "ServiceCatalogRequest",
                "description": "DTO request dùng chung cho create và update service. Đây là payload mà ServiceModal gửi lên backend khi admin thêm hoặc sửa dịch vụ.",
                "base_class": "POJO DTO",
                "constructor": "Lombok @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder cùng bean validation annotations.",
                "prototype": "public class ServiceCatalogRequest",
                "source_file": "BE/src/main/java/g2/be/dto/request/ServiceCatalogRequest.java",
                "namespace": "g2.be.dto.request",
                "attributes": [
                    ("serviceName", "String", "Tên dịch vụ, bắt buộc không để trống."),
                    ("price", "BigDecimal", "Giá dịch vụ, không nhỏ hơn 0."),
                    ("description", "String", "Mô tả chi tiết của dịch vụ."),
                    ("isActive", "Boolean", "Trạng thái hoạt động ban đầu của dịch vụ."),
                    ("bhytServiceCode", "String", "Mã BHYT nếu dịch vụ có liên quan bảo hiểm."),
                    ("deptId", "Integer", "Khoa phụ trách dịch vụ."),
                    ("maxPatients", "Integer", "Giới hạn số bệnh nhân tối đa trên mỗi ca."),
                ],
                "methods": [("N/A", "N/A", "N/A", "DTO request thuần cho create/update service.")],
            },
            {
                "name": "UpdateServiceStatusRequest",
                "description": "DTO request tối giản cho thao tác khóa/mở khóa dịch vụ. Payload này chỉ chứa cờ isActive nhưng là điểm vào riêng cho nhánh cập nhật trạng thái dịch vụ.",
                "base_class": "POJO DTO",
                "constructor": "Lombok @Data, @NoArgsConstructor, @AllArgsConstructor, @Builder.",
                "prototype": "public class UpdateServiceStatusRequest",
                "source_file": "BE/src/main/java/g2/be/dto/request/UpdateServiceStatusRequest.java",
                "namespace": "g2.be.dto.request",
                "attributes": [("isActive", "Boolean", "Giá trị trạng thái đích cho service đang được khóa hoặc mở khóa.")],
                "methods": [("N/A", "N/A", "N/A", "DTO dữ liệu thuần cho PATCH /api/services/{serviceId}/status.")],
            },
        ],
    },
]


def set_page_size_and_margins(doc: Document) -> None:
    section = doc.sections[0]
    section.page_width = Mm(210)
    section.page_height = Mm(297)
    section.top_margin = Mm(18)
    section.bottom_margin = Mm(18)
    section.left_margin = Mm(16)
    section.right_margin = Mm(16)


def set_run_font(run, size=BODY_SIZE, bold=False, italic=False):
    run.font.name = FONT_NAME
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    run.font.color.rgb = TEXT_COLOR
    run._element.rPr.rFonts.set(qn("w:eastAsia"), FONT_NAME)
    run._element.rPr.rFonts.set(qn("w:ascii"), FONT_NAME)
    run._element.rPr.rFonts.set(qn("w:hAnsi"), FONT_NAME)


def set_paragraph_font(paragraph, size=BODY_SIZE, bold=False, italic=False, align=None, space_before=0, space_after=3):
    if align is not None:
        paragraph.alignment = align
    paragraph.paragraph_format.space_before = Pt(space_before)
    paragraph.paragraph_format.space_after = Pt(space_after)
    paragraph.paragraph_format.line_spacing = 1.15
    for run in paragraph.runs:
        set_run_font(run, size=size, bold=bold, italic=italic)


def add_text(doc: Document, text: str, size=BODY_SIZE, bold=False, italic=False, align=None, space_before=0, space_after=3):
    paragraph = doc.add_paragraph()
    run = paragraph.add_run(text)
    set_run_font(run, size=size, bold=bold, italic=italic)
    paragraph.paragraph_format.space_before = Pt(space_before)
    paragraph.paragraph_format.space_after = Pt(space_after)
    paragraph.paragraph_format.line_spacing = 1.15
    if align is not None:
        paragraph.alignment = align
    return paragraph


def shade_cell(cell, fill: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tc_pr.append(shd)


def set_cell_text(cell, text: str, *, bold=False, align=WD_ALIGN_PARAGRAPH.LEFT, size=BODY_SIZE):
    cell.text = ""
    paragraph = cell.paragraphs[0]
    paragraph.alignment = align
    paragraph.paragraph_format.space_before = Pt(0)
    paragraph.paragraph_format.space_after = Pt(0)
    paragraph.paragraph_format.line_spacing = 1.1
    run = paragraph.add_run(text)
    set_run_font(run, size=size, bold=bold)
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def format_table(table):
    table.style = "Table Grid"
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False


def add_screen_table(doc: Document, caption: str, rows):
    add_text(doc, caption, size=11, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, space_before=3, space_after=4)
    table = doc.add_table(rows=1, cols=6)
    format_table(table)
    widths = [0.45, 1.45, 1.1, 0.9, 1.05, 2.35]
    for idx, width in enumerate(widths):
        for cell in table.columns[idx].cells:
            cell.width = Inches(width)

    headers = ["No", "Object/Control Name", "Type", "Required", "Length", "Description"]
    for idx, header in enumerate(headers):
        cell = table.rows[0].cells[idx]
        set_cell_text(cell, header, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER)
        shade_cell(cell, HEADER_FILL)

    for row in rows:
        cells = table.add_row().cells
        for idx, value in enumerate(row):
            align = WD_ALIGN_PARAGRAPH.CENTER if idx in {0, 2, 3, 4} else WD_ALIGN_PARAGRAPH.LEFT
            set_cell_text(cells[idx], value, align=align)


def add_info_table(doc: Document, class_info):
    table = doc.add_table(rows=0, cols=2)
    format_table(table)
    for cell in table.columns[0].cells:
        cell.width = Inches(1.55)
    for cell in table.columns[1].cells:
        cell.width = Inches(5.4)

    info_rows = [
        ("Class", class_info["name"]),
        ("Description", class_info["description"]),
        ("Base Class", class_info["base_class"]),
        ("Constructor", class_info["constructor"]),
        ("Prototype", class_info["prototype"]),
        ("Source File", class_info["source_file"]),
        ("Namespace", class_info["namespace"]),
    ]

    for label, value in info_rows:
        row = table.add_row().cells
        set_cell_text(row[0], label, bold=True)
        shade_cell(row[0], LABEL_FILL)
        set_cell_text(row[1], value)


def add_named_table(doc: Document, title: str, headers, rows, widths):
    add_text(doc, title, size=11, bold=True, space_before=4, space_after=2)
    table = doc.add_table(rows=1, cols=len(headers))
    format_table(table)
    for idx, width in enumerate(widths):
        for cell in table.columns[idx].cells:
            cell.width = Inches(width)

    for idx, header in enumerate(headers):
        cell = table.rows[0].cells[idx]
        set_cell_text(cell, header, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER)
        shade_cell(cell, HEADER_FILL)

    for row_values in rows:
        row = table.add_row().cells
        for idx, value in enumerate(row_values):
            align = WD_ALIGN_PARAGRAPH.LEFT if idx == len(row_values) - 1 or idx == 0 else WD_ALIGN_PARAGRAPH.LEFT
            set_cell_text(row[idx], value, align=align)


def add_class_block(doc: Document, section_number: str, class_index: int, class_info):
    add_text(doc, f"{section_number}.2.2.{class_index} {class_info['name']}", size=12, bold=True, space_before=8, space_after=4)
    add_info_table(doc, class_info)
    add_named_table(
        doc,
        "Attributes",
        ["Name", "Type", "Description"],
        class_info.get("attributes", [("N/A", "N/A", "N/A")]),
        [1.55, 1.55, 3.85],
    )
    add_named_table(
        doc,
        "Methods",
        ["Name", "Input", "Output", "Description"],
        class_info.get("methods", [("N/A", "N/A", "N/A", "N/A")]),
        [1.3, 1.45, 1.25, 2.95],
    )


def build_report() -> Path:
    doc = Document()
    set_page_size_and_margins(doc)

    for idx, section in enumerate(SECTIONS, start=1):
        if idx > 1:
            doc.add_page_break()

        section_number = f"4.{idx}"
        add_text(doc, f"{section_number} {section['title']}", size=14, bold=True, space_after=6)

        add_text(doc, f"{section_number}.1 Screen Design", size=12, bold=True, space_after=4)
        add_screen_table(doc, f"Table 4-{idx}: {section['screen_name']} Definition", section["screen_rows"])

        add_text(doc, f"{section_number}.2 Class Design", size=12, bold=True, space_before=8, space_after=4)
        add_text(doc, f"{section_number}.2.1 Class diagram", size=11.5, bold=True, space_after=3)
        add_text(doc, section["diagram_note"], size=11, italic=True, space_after=6)

        add_text(doc, f"{section_number}.2.2 Class Description", size=11.5, bold=True, space_after=4)
        for class_index, class_info in enumerate(section["classes"], start=1):
            add_class_block(doc, section_number, class_index, class_info)

    save_targets = [PRIMARY_OUTPUT_PATH, FALLBACK_OUTPUT_PATH]
    last_error = None

    for target in save_targets:
        try:
            target.parent.mkdir(parents=True, exist_ok=True)
            doc.save(str(target))
            return target
        except PermissionError as exc:
            last_error = exc

    if last_error is not None:
        raise last_error

    raise RuntimeError("No writable output path was available for the generated report.")


if __name__ == "__main__":
    result = build_report()
    print(f"Generated: {result}")