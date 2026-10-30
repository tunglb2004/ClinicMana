from __future__ import annotations

from copy import copy
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from openpyxl import load_workbook
from openpyxl.cell.cell import MergedCell


ROOT = Path(__file__).resolve().parents[1]
WORKBOOK_PATH = ROOT / "UnitTest.xlsx"
BACKUP_PATH = ROOT / "UnitTest.backup.before-full.xlsx"
FALLBACK_OUTPUT_PATH = ROOT / "UnitTest.full-populated.xlsx"
FUNCTIONS_SHEET = "Functions"
STATS_SHEET = "Statistics"
COVER_SHEET = "Cover"
TEMPLATE_SHEET = "Function 1"
AUTHOR = "Trường Giang"


@dataclass(frozen=True)
class FunctionSpec:
    no: int
    requirement: str
    class_name: str
    function_name: str
    function_code: str
    description: str
    precondition: str
    pattern: str
    actor: str
    entity: str
    loc: int
    context: dict[str, Any] = field(default_factory=dict)


FUNCTIONS: list[FunctionSpec] = [
    FunctionSpec(1, "3.2.1 Login (UC-01)", "AuthController / AuthService", "Login", "AUTH_LOGIN", "Authenticate by registered email or phone number and password, then return access token, refresh token, and user info.", "Registered active account exists.", "login", "All Users", "login session", 42),
    FunctionSpec(2, "3.2.2 Register (UC-72)", "AuthController / AuthService / VerificationTokenService", "Register + Verify OTP", "AUTH_REGISTER", "Validate registration data, send OTP, verify OTP, and create a new patient account.", "User is not authenticated; email and phone are not registered.", "register", "Guest User", "registration account", 78),
    FunctionSpec(3, "3.2.2.3 Forgot Password (UC-03)", "AuthController / UserService", "Forgot Password + Reset Password", "AUTH_FORGOT_RESET", "Send recovery OTP to existing email and reset password with a valid OTP and valid new password.", "Email exists in system and email delivery is available.", "forgot_reset", "User", "password reset", 64),
    FunctionSpec(4, "3.2.2.4 Change Password (UC-02)", "UserController / UserService", "Change Password", "AUTH_CHANGE_PASSWORD", "Authenticated user changes password after current-password and confirmation checks.", "Authenticated user session exists.", "change_password", "Authenticated User", "password change", 36),
    FunctionSpec(5, "3.3.1 Staff List (UC-38)", "AdminUserController / AdminUserService", "View staff accounts list", "STAFF_LIST", "Browse, search, and filter paginated staff accounts.", "Authorized back-office user is authenticated.", "view_list", "Admin/Manager/Cashier", "staff accounts", 36, {"search": "staff code, full name, email", "filters": "role/status"}),
    FunctionSpec(6, "3.3.2 Create Staff Account (UC-39)", "AdminUserController / AdminUserService", "Create Staff Account", "STAFF_CREATE", "Create a new staff account with the correct role and mandatory profile information.", "Authorized back-office user is authenticated.", "create_entity", "Admin/Manager/Cashier", "staff account", 54, {"required": "full name, email, phone, role", "unique": "email/username/CCCD", "dependency": "valid role and related profile data"}),
    FunctionSpec(7, "3.3.3 Update Staff Account (UC-40)", "AdminUserController / AdminUserService", "Update Staff Account", "STAFF_UPDATE", "Update existing staff information and role-related profile data.", "Target staff account exists.", "update_entity", "Admin/Manager/Cashier", "staff account", 46, {"required": "existing account and valid changed fields", "conflict": "duplicate email/CCCD/phone"}),
    FunctionSpec(8, "3.3.3 Delete Staff Account", "AdminUserController / AdminUserService", "Delete Staff Account", "STAFF_DELETE", "Soft-delete a staff account when business constraints allow it.", "Target staff account exists.", "delete_entity", "Admin/Manager/Cashier", "staff account", 28, {"dependency": "linked schedules or protected records"}),
    FunctionSpec(9, "3.3.3 Lock Staff Account", "AdminUserController / AdminUserService", "Lock Staff Account", "STAFF_LOCK_UNLOCK", "Lock or unlock a staff account by toggling active status.", "Target staff account exists.", "lock_toggle", "Admin/Manager/Cashier", "staff account", 24),
    FunctionSpec(10, "3.4.1 View customer accounts list (UC-43)", "PatientManagementController / PatientManagementService", "View customer accounts list", "PATIENT_LIST", "Browse, search, and filter customer accounts managed by staff.", "Authorized staff user is authenticated.", "view_list", "Receptionist/Cashier/Admin", "customer accounts", 34, {"search": "patient code, full name, email, CCCD", "filters": "active status"}),
    FunctionSpec(11, "3.4.2 Create customer account (UC-44)", "PatientManagementController / PatientManagementService", "Create Customer Account (National ID optional)", "PATIENT_CREATE", "Create a new customer/patient account with optional national ID and medical profile fields.", "Authorized staff user is authenticated.", "create_entity", "Receptionist/Cashier/Admin", "customer account", 58, {"required": "email, full name, phone number", "unique": "email/phone/CCCD", "dependency": "valid profile values"}),
    FunctionSpec(12, "3.4.3 Update customer account (UC-45)", "PatientManagementController / PatientManagementService", "Update Customer Account (National ID optional)", "PATIENT_UPDATE", "Update an existing customer account and patient profile information.", "Target customer account exists.", "update_entity", "Receptionist/Cashier/Admin", "customer account", 48, {"required": "existing account and valid changed fields", "conflict": "duplicate email/phone/CCCD"}),
    FunctionSpec(13, "3.4 Customer Account Management", "PatientManagementController / PatientManagementService", "Delete Customer Account", "PATIENT_DELETE", "Soft-delete a customer account when policy allows it.", "Target customer account exists.", "delete_entity", "Cashier/Admin", "customer account", 28, {"dependency": "appointments, invoices, or protected records"}),
    FunctionSpec(14, "3.4 Customer Account Management", "PatientManagementController / PatientManagementService", "LOCK / UNLOCK CUSTOMER", "PATIENT_LOCK_UNLOCK", "Lock or unlock a customer account by toggling active status.", "Target customer account exists.", "lock_toggle", "Cashier/Admin", "customer account", 24),
    FunctionSpec(15, "3.5.6 Update Room (UC-53)", "RoomController / RoomService", "Update Room", "ROOM_UPDATE", "Update room identity, department, type, and operational status.", "Target room exists.", "update_entity", "Clinic Manager/Admin", "room", 40, {"required": "existing room and valid changed fields", "conflict": "duplicate room code/name"}),
    FunctionSpec(16, "3.5 Room Management API", "RoomController / RoomService", "Delete Room", "ROOM_DELETE", "Soft-delete a room when no blocking dependency exists.", "Target room exists.", "delete_entity", "Clinic Manager/Admin", "room", 26, {"dependency": "assigned staff or dependent schedules"}),
    FunctionSpec(17, "3.5.1 View Clinical Department Structure List (UC-48)", "DepartmentController / DepartmentService", "View Clinical Department Structure List", "DEPT_LIST", "Browse and search the clinical department structure list.", "Authorized manager/admin is authenticated.", "view_list", "Clinic Manager/Admin", "clinical departments", 32, {"search": "department name", "filters": "include inactive"}),
    FunctionSpec(18, "3.5.2 Create Clinical Department Structure", "DepartmentController / DepartmentService", "Create Clinical Department Structure", "DEPT_CREATE", "Create a new clinical department structure with valid department data.", "Authorized manager/admin is authenticated.", "create_entity", "Clinic Manager/Admin", "clinical department", 38, {"required": "department name and valid metadata", "unique": "department name"}),
    FunctionSpec(19, "3.5.3 Update Clinical Department Structure", "DepartmentController / DepartmentService", "Update Clinical Department Structure", "DEPT_UPDATE", "Update existing department information and active status.", "Target department exists.", "update_entity", "Clinic Manager/Admin", "clinical department", 36, {"required": "existing department and valid changed fields", "conflict": "duplicate department name"}),
    FunctionSpec(20, "3.5 Clinical Department Structure Management", "DepartmentController / DepartmentService", "Delete Clinical Department Structure", "DEPT_DELETE", "Soft-delete a department when no rooms or members block deletion.", "Target department exists.", "delete_entity", "Clinic Manager/Admin", "clinical department", 26, {"dependency": "assigned members or rooms"}),
    FunctionSpec(21, "3.5.4 Create Room (UC-49)", "RoomController / RoomService", "Create Room", "ROOM_CREATE", "Create a clinical room with valid room code, type, and department assignment.", "Authorized manager/admin is authenticated.", "create_entity", "Clinic Manager/Admin", "room", 42, {"required": "room name, type, department", "unique": "room id/name"}),
    FunctionSpec(22, "3.6.1 View Working Time Slot List (UC-55)", "WorkingTimeSlotController / WorkingTimeSlotService", "View Working Time Slot List", "WORK_SLOT_LIST", "Browse active working time slot templates.", "Authorized manager/admin is authenticated.", "view_list", "Clinic Manager/Admin", "working time slots", 30, {"search": "shift name or time range", "filters": "active only"}),
    FunctionSpec(23, "3.6.2 Create Working Time Slot (UC-56)", "WorkingTimeSlotController / WorkingTimeSlotService", "Create Working Time Slot", "WORK_SLOT_CREATE", "Create a new working time slot template with valid time range and capacity.", "Authorized manager/admin is authenticated.", "create_entity", "Clinic Manager/Admin", "working time slot", 38, {"required": "shift name, start/end time", "unique": "non-overlapping time range"}),
    FunctionSpec(24, "3.6.3 Update Working Time Slot (UC-57)", "WorkingTimeSlotController / WorkingTimeSlotService", "Update Working Time Slot", "WORK_SLOT_UPDATE", "Update working time slot attributes while preserving valid time boundaries.", "Target working time slot exists.", "update_entity", "Clinic Manager/Admin", "working time slot", 36, {"required": "existing slot and valid changed fields", "conflict": "overlapping slot range"}),
    FunctionSpec(25, "3.6 Working Time Slot Management", "WorkingTimeSlotController / WorkingTimeSlotService", "Delete Working Time Slot", "WORK_SLOT_DELETE", "Soft-delete a working time slot template.", "Target working time slot exists.", "delete_entity", "Clinic Manager/Admin", "working time slot", 24, {"dependency": "referenced schedules or appointments"}),
    FunctionSpec(26, "3.6 Work Shift Management", "StaffScheduleController / StaffScheduleService", "Add Nurse Follow Doctor", "NURSE_DOCTOR_ASSIGN", "Assign a nurse to follow a doctor for operational coordination.", "Doctor and nurse exist and are eligible.", "assign_relation", "Clinic Manager/Admin", "nurse-doctor assignment", 30, {"relation": "nurse to doctor", "duplicate": "nurse already follows doctor on selected date"}),
    FunctionSpec(27, "3.5 Room Management", "RoomController / RoomService", "Assign Members to Room", "ROOM_ASSIGN_MEMBER", "Assign an eligible doctor/member to a room.", "Target room exists.", "assign_relation", "Clinic Manager/Admin", "room member assignment", 30, {"relation": "doctor/member to room", "duplicate": "member already assigned to room"}),
    FunctionSpec(28, "3.6 Staff Working Hours List (UC-54)", "StaffScheduleController / StaffScheduleService", "View Staff Working Hours List", "STAFF_SCHEDULE_LIST", "Browse staff working hours by date and department.", "Authorized manager/admin is authenticated.", "view_list", "Clinic Manager/Admin", "staff schedules", 34, {"search": "date or department filter", "filters": "department/date"}),
    FunctionSpec(29, "3.6 Staff Working Hours Detail", "StaffScheduleController / StaffScheduleService", "View Staff Working Hours Details", "STAFF_SCHEDULE_DETAIL", "View detailed working schedule for a specific staff member.", "Target staff user exists.", "view_detail", "Clinic Manager/Admin", "staff schedule detail", 26, {"detail": "schedule assignments within selected date range"}),
    FunctionSpec(30, "3.6.2 Create Work Shift", "StaffScheduleController / StaffScheduleService", "Add Staff to Shift", "STAFF_SCHEDULE_CREATE", "Assign a staff member to a shift on a specific date.", "Eligible staff and target shift exist.", "schedule_create", "Clinic Manager/Admin", "staff schedule", 38),
    FunctionSpec(31, "3.6.3 Update Staff Hours (UC-57)", "StaffScheduleController / StaffScheduleService", "Update Staff Working Hours", "STAFF_SCHEDULE_UPDATE", "Update a scheduled staff assignment.", "Target staff schedule exists.", "schedule_update", "Clinic Manager/Admin", "staff schedule", 34),
    FunctionSpec(32, "3.6 Delete Staff Hours (UC-58)", "StaffScheduleController / StaffScheduleService", "Delete Staff Working Hours", "STAFF_SCHEDULE_DELETE", "Delete a staff working-hours assignment.", "Target staff schedule exists.", "schedule_delete", "Clinic Manager/Admin", "staff schedule", 24),
    FunctionSpec(33, "3.7.1 View customer accounts list (UC-67)", "PatientManagementController / AppointmentController", "View Customer Accounts List", "SCHEDULING_CUSTOMER_LOOKUP", "Search customer accounts before creating or updating appointments.", "Receptionist is authenticated.", "view_list", "Receptionist", "customer accounts for scheduling", 30, {"search": "patient code, name, phone"}),
    FunctionSpec(34, "3.7.2 Cancel Customer Information (UC-68)", "AppointmentController / PatientManagementController", "Cancel Customer Information", "SCHEDULING_CANCEL_INFO", "Cancel an existing customer appointment/scheduling record when allowed.", "Target appointment exists.", "appointment_cancel_admin", "Receptionist", "customer appointment", 26),
    FunctionSpec(35, "3.7.2 Update appointment and customer information (UC-10)", "AppointmentController / PatientManagementController", "Update Appointment & Customer Info", "SCHEDULING_UPDATE_APPT_CUSTOMER", "Update appointment timing and linked customer information in one scheduling flow.", "Target appointment and customer exist.", "update_entity", "Receptionist", "appointment and customer information", 44, {"required": "existing appointment/customer and valid changed fields", "conflict": "slot conflict or duplicate customer data"}),
    FunctionSpec(36, "3.7.3 Create Forward Examination Request to Doctor (UC-11)", "AppointmentController / AppointmentService", "Create Appointment for Customer", "SCHEDULING_CREATE_APPOINTMENT", "Create an appointment for a customer and route it to the appropriate doctor/room.", "Receptionist is authenticated and patient exists.", "appointment_admin_create", "Receptionist", "customer appointment", 56),
    FunctionSpec(37, "3.7.4 Collect Prepayment for General Examination (UC-12)", "PaymentController / FinancialController / CitizenInsuranceController", "Collect Prepayment (Cash/QR)", "PAYMENT_PREPAYMENT", "Collect prepayment before examination using cash or QR and optionally apply insurance lookup by CCCD.", "Cashier is authenticated and appointment has unpaid payable items.", "prepayment", "Cashier", "prepayment collection", 52),
    FunctionSpec(38, "3.8 Appointment Management", "MedicalRecordController", "Appointment Management - Health Record: View Visit History", "PATIENT_VISIT_HISTORY_DETAIL", "View a patient's health-record visit history detail.", "Authenticated patient owns the requested record.", "view_detail", "Patient", "visit history detail", 28, {"detail": "visit history detail for a specific examination"}),
    FunctionSpec(39, "3.8 Appointment Management", "MedicalRecordController (requirement-driven; export endpoint gap)", "Appointment Management - Download Health Record", "PATIENT_DOWNLOAD_HEALTH_RECORD", "Download or export an owned health record document when the record is available.", "Authenticated patient owns a completed health record.", "download_document", "Patient", "health record document", 30, {"document": "health record export"}),
    FunctionSpec(40, "3.8 Appointment Management", "MedicalRecordController", "Appointment Management - View Visit History List", "PATIENT_VISIT_HISTORY_LIST", "Browse the list of examined visits and health records.", "Authenticated patient is logged in.", "view_list", "Patient", "visit history records", 32, {"search": "keyword in record code or diagnosis"}),
    FunctionSpec(41, "3.8 Appointment Management", "AppointmentController / AppointmentService", "Appointment Management - Create Appointment", "PATIENT_BOOK_APPOINTMENT", "Patient books a new appointment for self or relative.", "Authenticated patient is logged in.", "patient_book", "Patient", "patient appointment", 58),
    FunctionSpec(42, "3.8 Appointment Management", "AppointmentController / AppointmentService", "Appointment Management - Confirm Appointment Booking", "PATIENT_CONFIRM_BOOKING", "Patient confirms final appointment selection after choosing slot, room, and services.", "A valid booking draft/selection exists.", "confirm_booking", "Patient", "appointment confirmation", 40),
    FunctionSpec(43, "3.8 Appointment Management", "AppointmentController", "Appointment Management - View Appointment Details", "PATIENT_APPOINTMENT_DETAIL", "View detailed appointment information.", "Target appointment exists and belongs to requester.", "view_detail", "Patient", "appointment detail", 24, {"detail": "selected appointment detail"}),
    FunctionSpec(44, "3.8 Appointment Management", "AppointmentController", "Appointment Management - Cancel Appointment", "PATIENT_CANCEL_APPOINTMENT", "Patient cancels their own appointment under cancellation rules.", "Target appointment exists and belongs to requester.", "patient_cancel", "Patient", "patient appointment", 26),
    FunctionSpec(45, "3.2 User Profile (UC-04)", "UserController / UserService", "View User Profile", "USER_PROFILE_VIEW", "View the authenticated user's profile and role-specific details.", "Authenticated user session exists.", "view_profile", "Authenticated User", "user profile", 20),
    FunctionSpec(46, "3.2 User Profile (UC-04)", "UserController / UserService", "Update User Profile", "USER_PROFILE_UPDATE", "Update personal profile fields such as full name, gender, phone, CCCD, and patient profile details.", "Authenticated user session exists.", "update_profile", "Authenticated User", "user profile", 42),
    FunctionSpec(47, "3.9 Send General Feedback to Clinic", "ReviewController / ReviewService", "Send General Feedback to Clinic", "REVIEW_CREATE", "Patient submits a general review/feedback after a valid appointment.", "Authenticated patient with eligible appointment exists.", "create_review", "Patient", "clinic feedback", 34),
    FunctionSpec(48, "3.10 Payments & Receipts / View Financial List (UC-06)", "FinancialController / FinancialService", "View Financial List", "FINANCIAL_LIST", "Browse invoices or unpaid records with search and status filters.", "Cashier/admin is authenticated.", "view_list", "Cashier/Admin", "financial records", 32, {"search": "invoice id, patient name, patient id", "filters": "payment status"}),
    FunctionSpec(49, "3.10 Payments & Receipts / View Financial Detail (UC-07)", "FinancialController / FinancialService", "View Financial Detail", "FINANCIAL_DETAIL", "View invoice details, payment breakdown, and linked receipt information.", "Target invoice exists.", "financial_detail", "Cashier/Admin", "financial detail", 30),
    FunctionSpec(50, "3.10 Payments & Receipts", "FinancialController / FinancialService", "Issue Receipt", "FINANCIAL_ISSUE_RECEIPT", "Issue a receipt for a paid invoice.", "Target invoice exists and is already paid.", "issue_receipt", "Cashier/Admin", "receipt issuing", 30),
    FunctionSpec(51, "3.11.1 View List of Appointments for the Day (UC-24)", "AppointmentController / AppointmentService", "View List of Appointments for the Day", "DOCTOR_DAILY_APPOINTMENTS", "View the doctor's daily appointment queue.", "Authorized doctor/nurse session exists.", "view_list", "Doctor/Nurse", "daily appointments", 32, {"search": "patient name, patient id, phone", "filters": "status/date"}),
    FunctionSpec(52, "3.11.2 View examination order details", "ServiceOrderController / ServiceOrderService", "View examination order details", "EXAM_ORDER_DETAIL", "View detailed information for a specific service order/examination order.", "Target service order exists.", "view_detail", "Doctor/Nurse", "examination order detail", 26, {"detail": "service order detail by order id"}),
    FunctionSpec(53, "3.11 Daily Patient Queue Management", "ServiceOrderController / ServiceOrderService", "Upload file examination result / Update examination result", "EXAM_RESULT_UPDATE", "Upload a result file and update the textual conclusion for an examination order.", "Target service order exists and is eligible for result entry.", "upload_update_result", "Doctor/Nurse", "examination result", 44),
    FunctionSpec(54, "3.11 Daily Patient Queue Management / Create prescription", "PrescriptionController / PrescriptionService", "Create prescription", "PRESCRIPTION_CREATE", "Create a new prescription for the current examination.", "Authorized doctor/nurse session exists and target record is valid.", "create_prescription", "Doctor/Nurse", "prescription", 42),
    FunctionSpec(55, "3.11 Daily Patient Queue Management / View examined customer history list", "MedicalRecordController / MedicalRecordService", "View examined customer history list", "MEDICAL_HISTORY_LIST", "Browse the examined patient's medical history list.", "Authorized clinical user is authenticated and patient exists.", "view_list", "Doctor/Nurse", "examined patient history", 30, {"search": "record code, diagnosis, patient history keyword"}),
    FunctionSpec(56, "3.11 Daily Patient Queue Management / View medical record details", "MedicalRecordController / MedicalRecordService", "View medical record details", "MEDICAL_RECORD_DETAIL", "View full medical-record details for a selected patient examination.", "Target medical record exists.", "view_detail", "Doctor/Nurse", "medical record detail", 28, {"detail": "medical record detail by record id"}),
]


def sheet_name_for_no(no: int) -> str:
    if no == 3:
        return "Function3"
    return f"Function {no}"


def quote_sheet(sheet_name: str) -> str:
    return f"'{sheet_name}'"


def ensure_sheet(workbook, sheet_name: str):
    if sheet_name in workbook.sheetnames:
        return workbook[sheet_name]
    sheet = workbook.copy_worksheet(workbook[TEMPLATE_SHEET])
    sheet.title = sheet_name
    return sheet


def clear_real_cells(ws, min_row: int, max_row: int, min_col: int, max_col: int) -> None:
    for row in ws.iter_rows(min_row=min_row, max_row=max_row, min_col=min_col, max_col=max_col):
        for cell in row:
            if isinstance(cell, MergedCell):
                continue
            cell.value = None


def copy_row_style(ws, source_row: int, target_row: int, start_col: int, end_col: int) -> None:
    ws.row_dimensions[target_row].height = ws.row_dimensions[source_row].height
    for col in range(start_col, end_col + 1):
        target = ws.cell(target_row, col)
        if isinstance(target, MergedCell):
            continue
        source = ws.cell(source_row, col)
        target._style = copy(source._style)
        target.font = copy(source.font)
        target.fill = copy(source.fill)
        target.border = copy(source.border)
        target.alignment = copy(source.alignment)
        target.number_format = source.number_format
        target.protection = copy(source.protection)


def apply_standard_headers(cells: dict[str, Any], row_type: int = 39) -> None:
    cells.setdefault("A10", "Condition")
    cells.setdefault("B10", "Precondition")
    cells.setdefault("A31", "Confirm")
    cells.setdefault("B31", "Return")
    cells.setdefault(f"A{row_type}", "Result")
    cells.setdefault(f"B{row_type}", "Type(N : Normal, A : Abnormal, B : Boundary)")
    cells.setdefault(f"B{row_type + 1}", "Passed/Failed")
    cells.setdefault(f"B{row_type + 2}", "Executed Date")
    cells.setdefault(f"B{row_type + 3}", "Defect ID")


def build_login(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Registered active account exists",
        "D12": "Account locked or inactive",
        "B14": "Identifier",
        "D15": "Registered email",
        "D16": "Registered phone number",
        "D17": "Boundary-valid phone format after normalization",
        "D18": "Unregistered email or phone",
        "D19": "Blank or malformed identifier",
        "B21": "Password",
        "D22": "Correct password",
        "D23": "Incorrect password",
        "D24": "Blank password",
        "D32": "Access token, refresh token, and user info returned",
        "D33": "No token returned",
        "D34": "Account access rejected",
        "B36": "Log message",
        "D36": "Login successful.",
        "D37": "Login failed because credentials are invalid.",
        "D38": "Account is locked or inactive.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 15, 22, 32, 36]},
        {"type": "N", "rows": [11, 16, 22, 32, 36]},
        {"type": "B", "rows": [11, 17, 22, 32, 36]},
        {"type": "A", "rows": [11, 15, 23, 33, 37]},
        {"type": "A", "rows": [18, 22, 33, 37]},
        {"type": "A", "rows": [19, 22, 33, 37]},
        {"type": "A", "rows": [11, 15, 24, 33, 37]},
        {"type": "A", "rows": [12, 15, 22, 34, 38]},
    ]
    return cells, cases


def build_register(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "User is not authenticated",
        "D12": "Email and phone are not registered",
        "D13": "Duplicate email exists",
        "D14": "Duplicate phone exists",
        "B16": "Registration data",
        "D16": "Valid full name",
        "D17": "Invalid full-name format",
        "D18": "Valid email format",
        "D19": "Invalid email format",
        "D20": "Valid Vietnamese phone format",
        "D21": "Invalid phone format",
        "D22": "Boundary-valid phone length",
        "D23": "Strong password",
        "D24": "Weak password",
        "D25": "Confirm password matches",
        "D26": "Confirm password mismatch",
        "B28": "OTP",
        "D28": "Valid unused OTP",
        "D29": "Invalid OTP",
        "D30": "Expired or resent OTP scenario",
        "D32": "OTP send accepted and email returned",
        "D33": "Patient account created successfully",
        "D34": "Registration blocked; no account created",
        "B36": "Log message",
        "D36": "Registration step completed successfully.",
        "D37": "Validation or duplicate warning displayed.",
        "D38": "OTP invalid or expired; user must retry or resend.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 16, 18, 20, 23, 25, 28, 32, 33, 36]},
        {"type": "B", "rows": [11, 12, 16, 18, 22, 23, 25, 28, 32, 33, 36]},
        {"type": "A", "rows": [11, 13, 16, 18, 20, 23, 25, 34, 37]},
        {"type": "A", "rows": [11, 14, 16, 18, 20, 23, 25, 34, 37]},
        {"type": "A", "rows": [11, 12, 17, 18, 20, 23, 25, 34, 37]},
        {"type": "A", "rows": [11, 12, 16, 19, 20, 23, 25, 34, 37]},
        {"type": "A", "rows": [11, 12, 16, 18, 21, 23, 25, 34, 37]},
        {"type": "A", "rows": [11, 12, 16, 18, 20, 24, 25, 34, 37]},
        {"type": "A", "rows": [11, 12, 16, 18, 20, 23, 26, 34, 37]},
        {"type": "A", "rows": [11, 12, 16, 18, 20, 23, 25, 29, 34, 38]},
        {"type": "B", "rows": [11, 12, 16, 18, 20, 23, 25, 30, 32, 33, 36]},
    ]
    return cells, cases


def build_forgot_reset(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Existing account email",
        "D12": "Non-existing email",
        "B14": "Email",
        "D15": "Valid email format",
        "D16": "Invalid email format",
        "B18": "OTP",
        "D19": "Valid OTP",
        "D20": "Invalid OTP",
        "D21": "Expired OTP",
        "D22": "Resent OTP valid",
        "B24": "New password",
        "D25": "Strong password",
        "D26": "Weak password",
        "B27": "Confirm password",
        "D27": "Confirmation matches new password",
        "D28": "Confirmation mismatch",
        "D32": "Recovery OTP sent to email",
        "D33": "Password updated successfully",
        "D34": "Password not updated",
        "B36": "Log message",
        "D36": "Recovery step completed successfully.",
        "D37": "Email not found or validation failed.",
        "D38": "OTP or password validation failed.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 15, 32, 36]},
        {"type": "N", "rows": [11, 15, 19, 25, 27, 33, 36]},
        {"type": "A", "rows": [12, 15, 34, 37]},
        {"type": "A", "rows": [16, 34, 37]},
        {"type": "A", "rows": [11, 15, 20, 25, 27, 34, 38]},
        {"type": "A", "rows": [11, 15, 21, 25, 27, 34, 38]},
        {"type": "A", "rows": [11, 15, 19, 26, 27, 34, 38]},
        {"type": "A", "rows": [11, 15, 19, 25, 28, 34, 38]},
        {"type": "B", "rows": [11, 15, 22, 25, 27, 33, 36]},
    ]
    return cells, cases


def build_change_password(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Authenticated user session exists",
        "D12": "Current password is correct",
        "D13": "Current password is incorrect",
        "B15": "New password",
        "D16": "Strong password",
        "D17": "Weak password",
        "D18": "Boundary-valid minimum length",
        "D19": "Boundary-valid maximum length",
        "B21": "Confirmation",
        "D22": "Confirmation matches new password",
        "D23": "Confirmation mismatch",
        "B25": "Current password field",
        "D25": "Blank current password",
        "D32": "Password updated successfully",
        "D33": "Password not updated",
        "B35": "Log message",
        "D36": "Password changed successfully.",
        "D37": "Current password is incorrect.",
        "D38": "New-password validation or confirmation failed.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 16, 22, 32, 36]},
        {"type": "B", "rows": [11, 12, 18, 22, 32, 36]},
        {"type": "B", "rows": [11, 12, 19, 22, 32, 36]},
        {"type": "A", "rows": [11, 13, 16, 22, 33, 37]},
        {"type": "A", "rows": [11, 12, 17, 22, 33, 38]},
        {"type": "A", "rows": [11, 12, 16, 23, 33, 38]},
        {"type": "A", "rows": [11, 25, 16, 22, 33, 38]},
    ]
    return cells, cases


def build_view_list(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    search = spec.context.get("search", "keyword")
    filters = spec.context.get("filters", "filter values")
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": f"{spec.entity.capitalize()} data exists in system",
        "B14": "Search / Filter",
        "D15": "Default listing with no keyword",
        "D16": f"Matching {search}",
        "D17": f"No matching {search}",
        "D18": f"Valid {filters}",
        "D19": "Boundary pagination or page size",
        "D20": "Unauthorized role or request",
        "D32": f"Paginated {spec.entity} list returned",
        "D33": "Empty list returned",
        "D34": "Access denied or request rejected",
        "B36": "Log message",
        "D36": "List loaded successfully.",
        "D37": "No matching data found.",
        "D38": "Access denied or invalid filter/paging request.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 15, 32, 36]},
        {"type": "N", "rows": [11, 12, 16, 32, 36]},
        {"type": "A", "rows": [11, 17, 33, 37]},
        {"type": "B", "rows": [11, 12, 18, 32, 36]},
        {"type": "B", "rows": [11, 12, 19, 32, 36]},
        {"type": "A", "rows": [20, 34, 38]},
    ]
    return cells, cases


def build_view_detail(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    detail = spec.context.get("detail", spec.entity)
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": f"Target {detail} exists",
        "D13": "Requester has access to target data",
        "D14": f"Target {detail} not found",
        "D15": "Requester has no access to target data",
        "D32": f"{detail.capitalize()} returned successfully",
        "D33": "No detail returned",
        "D34": "Access denied or not-found error returned",
        "B36": "Log message",
        "D36": "Detail loaded successfully.",
        "D37": "Requested detail was not found.",
        "D38": "Access denied for requested detail.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 14, 33, 37]},
        {"type": "A", "rows": [11, 12, 15, 34, 38]},
    ]
    return cells, cases


def build_create_entity(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    required_fields = spec.context.get("required", "mandatory fields")
    unique_fields = spec.context.get("unique", "unique business fields")
    dependency = spec.context.get("dependency")
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": f"Valid {required_fields}",
        "D13": "Optional fields omitted or defaulted correctly",
        "D14": f"Duplicate {unique_fields}",
        "D15": "Missing mandatory fields or invalid format",
        "D16": "Boundary-valid maximum input length",
        "D32": f"{spec.entity.capitalize()} created successfully",
        "D33": f"{spec.entity.capitalize()} not created",
        "D34": "Validation or duplicate-data error returned",
        "B36": "Log message",
        "D36": "Create action completed successfully.",
        "D37": "Duplicate or validation warning returned.",
        "D38": "Related resource is missing or invalid.",
    }
    if dependency:
        cells["D17"] = f"Invalid or missing {dependency}"
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "B", "rows": [11, 12, 16, 32, 36]},
        {"type": "A", "rows": [11, 14, 33, 37]},
        {"type": "A", "rows": [11, 15, 33, 37]},
    ]
    if dependency:
        cases.append({"type": "A", "rows": [11, 17, 33, 38]})
    return cells, cases


def build_update_entity(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    required_fields = spec.context.get("required", "valid changed fields")
    conflict = spec.context.get("conflict", "duplicate or conflicting data")
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": f"Target {spec.entity} exists",
        "D13": f"Valid {required_fields}",
        "D14": "Boundary-valid optional/unchanged fields",
        "D15": conflict,
        "D16": "Invalid input format or missing mandatory data",
        "D17": f"Target {spec.entity} not found",
        "D32": f"{spec.entity.capitalize()} updated successfully",
        "D33": f"{spec.entity.capitalize()} not updated",
        "D34": "Validation, conflict, or not-found error returned",
        "B36": "Log message",
        "D36": "Update action completed successfully.",
        "D37": "Conflict or validation warning returned.",
        "D38": "Requested item was not found.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "B", "rows": [11, 12, 14, 32, 36]},
        {"type": "A", "rows": [11, 12, 15, 33, 37]},
        {"type": "A", "rows": [11, 12, 16, 33, 37]},
        {"type": "A", "rows": [11, 17, 33, 38]},
    ]
    return cells, cases


def build_delete_entity(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    dependency = spec.context.get("dependency", "blocking business dependency")
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": f"Target {spec.entity} exists",
        "D13": "No blocking dependency",
        "D14": dependency,
        "D15": f"Target {spec.entity} not found",
        "D32": f"{spec.entity.capitalize()} deleted or soft-deleted successfully",
        "D33": f"{spec.entity.capitalize()} not deleted",
        "D34": "Dependency or not-found error returned",
        "B36": "Log message",
        "D36": "Delete action completed successfully.",
        "D37": "Delete blocked by business dependency.",
        "D38": "Requested item was not found.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 12, 14, 33, 37]},
        {"type": "A", "rows": [11, 15, 33, 38]},
    ]
    return cells, cases


def build_lock_toggle(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": f"Active {spec.entity} exists",
        "D13": f"Inactive {spec.entity} exists",
        "D14": f"Target {spec.entity} not found",
        "D15": "Repeated request keeps same state",
        "D32": "Status toggled successfully",
        "D33": "No status change applied",
        "D34": "Not-found or invalid transition returned",
        "B36": "Log message",
        "D36": "Status updated successfully.",
        "D37": "No state change was needed.",
        "D38": "Requested item was not found.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 32, 36]},
        {"type": "N", "rows": [11, 13, 32, 36]},
        {"type": "B", "rows": [11, 13, 15, 33, 37]},
        {"type": "A", "rows": [11, 14, 34, 38]},
    ]
    return cells, cases


def build_assign_relation(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    relation = spec.context.get("relation", spec.entity)
    duplicate = spec.context.get("duplicate", "duplicate assignment")
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": f"Target data for {relation} exists",
        "D13": f"Eligible relation target for {relation}",
        "D14": duplicate,
        "D15": "Related member or container not found / invalid role",
        "D32": f"{relation.capitalize()} saved successfully",
        "D33": f"{relation.capitalize()} not saved",
        "D34": "Duplicate or invalid-assignment error returned",
        "B36": "Log message",
        "D36": "Assignment completed successfully.",
        "D37": "Duplicate assignment detected.",
        "D38": "Target member or owner is invalid.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 12, 14, 33, 37]},
        {"type": "A", "rows": [11, 15, 34, 38]},
    ]
    return cells, cases


def build_schedule_create(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": "Target staff and shift/date exist",
        "D13": "Selected shift/date is available",
        "D14": "Duplicate or overlapping schedule exists",
        "D15": "Missing mandatory fields or invalid date/shift",
        "D32": "Staff schedule created successfully",
        "D33": "Staff schedule not created",
        "D34": "Conflict or validation error returned",
        "B36": "Log message",
        "D36": "Schedule created successfully.",
        "D37": "Schedule conflict detected.",
        "D38": "Schedule request is invalid.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "B", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 12, 14, 33, 37]},
        {"type": "A", "rows": [11, 15, 34, 38]},
    ]
    return cells, cases


def build_schedule_update(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": "Target schedule exists",
        "D13": "New shift/date is valid",
        "D14": "Updated shift/date conflicts with another schedule",
        "D15": "Missing or invalid changed fields",
        "D16": "Target schedule not found",
        "D32": "Staff schedule updated successfully",
        "D33": "Staff schedule not updated",
        "D34": "Conflict, validation, or not-found error returned",
        "B36": "Log message",
        "D36": "Schedule updated successfully.",
        "D37": "Updated schedule conflicts with existing data.",
        "D38": "Target schedule was not found.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 12, 14, 33, 37]},
        {"type": "A", "rows": [11, 12, 15, 33, 37]},
        {"type": "A", "rows": [11, 16, 34, 38]},
    ]
    return cells, cases


def build_schedule_delete(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": "Target schedule exists",
        "D13": "Schedule is deletable",
        "D14": "Schedule has blocking dependency",
        "D15": "Target schedule not found",
        "D32": "Staff schedule deleted successfully",
        "D33": "Staff schedule not deleted",
        "D34": "Dependency or not-found error returned",
        "B36": "Log message",
        "D36": "Schedule deleted successfully.",
        "D37": "Schedule cannot be deleted because of dependency.",
        "D38": "Target schedule was not found.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 12, 14, 33, 37]},
        {"type": "A", "rows": [11, 15, 34, 38]},
    ]
    return cells, cases


def build_appointment_admin_create(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": "Target patient exists",
        "D13": "Selected doctor, room, services, and slot are available",
        "D14": "Selected slot is full or unavailable",
        "D15": "Invalid patient, doctor, room, or service selection",
        "D16": "Boundary-valid same-day booking",
        "D32": "Appointment created successfully",
        "D33": "Appointment not created",
        "D34": "Availability or validation error returned",
        "B36": "Log message",
        "D36": "Appointment created successfully.",
        "D37": "Selected slot is unavailable.",
        "D38": "Appointment request is invalid.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "B", "rows": [11, 12, 16, 32, 36]},
        {"type": "A", "rows": [11, 12, 14, 33, 37]},
        {"type": "A", "rows": [11, 15, 34, 38]},
    ]
    return cells, cases


def build_patient_book(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Authenticated patient session exists",
        "D12": "Selected services, room, and time slot are available",
        "D13": "Booking for self with valid data",
        "D14": "Booking for relative with valid relative info",
        "D15": "Selected slot is full or unavailable",
        "D16": "Invalid date/time or missing service selection",
        "D17": "Boundary-valid near-term appointment date",
        "D32": "Appointment booked successfully",
        "D33": "Appointment not booked",
        "D34": "Availability or validation error returned",
        "B36": "Log message",
        "D36": "Appointment booked successfully.",
        "D37": "Selected slot is unavailable.",
        "D38": "Booking request is invalid.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "N", "rows": [11, 12, 14, 32, 36]},
        {"type": "B", "rows": [11, 12, 17, 32, 36]},
        {"type": "A", "rows": [11, 12, 15, 33, 37]},
        {"type": "A", "rows": [11, 16, 34, 38]},
    ]
    return cells, cases


def build_confirm_booking(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Authenticated patient session exists",
        "D12": "A valid booking draft/selection exists",
        "D13": "Selected slot remains available at confirmation time",
        "D14": "Selected slot expires or becomes unavailable before confirmation",
        "D15": "Incomplete booking data at confirmation step",
        "D32": "Booking confirmation saved successfully",
        "D33": "Booking confirmation not saved",
        "D34": "Availability or validation error returned",
        "B36": "Log message",
        "D36": "Booking confirmed successfully.",
        "D37": "Selected slot is no longer available.",
        "D38": "Booking confirmation data is incomplete.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 12, 14, 33, 37]},
        {"type": "A", "rows": [11, 15, 34, 38]},
    ]
    return cells, cases


def build_appointment_cancel(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": "Target appointment exists",
        "D13": "Cancellation policy allows cancellation",
        "D14": "Cancellation policy blocks cancellation",
        "D15": "Target appointment not found or not owned by requester",
        "D32": "Appointment cancelled successfully",
        "D33": "Appointment not cancelled",
        "D34": "Policy or ownership error returned",
        "B36": "Log message",
        "D36": "Appointment cancelled successfully.",
        "D37": "Cancellation blocked by policy.",
        "D38": "Appointment was not found or access is denied.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 12, 14, 33, 37]},
        {"type": "A", "rows": [11, 15, 34, 38]},
    ]
    return cells, cases


def build_prepayment(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Cashier session exists",
        "D12": "Appointment has unpaid initial services",
        "D13": "Payment method = CASH",
        "D14": "Payment method = QR",
        "D15": "Valid CCCD with insurance data available",
        "D16": "CCCD omitted; full-price payment applies",
        "D17": "Invoice already paid or no payable items",
        "D18": "Invalid appointment id or invalid payment method",
        "D32": "Cash payment recorded and invoice marked PAID",
        "D33": "QR invoice created in pending state",
        "D34": "Payment rejected or no payable items",
        "B36": "Log message",
        "D36": "Payment processed successfully.",
        "D37": "QR payment created and waiting for settlement.",
        "D38": "Payment request rejected.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "N", "rows": [11, 12, 14, 33, 37]},
        {"type": "B", "rows": [11, 12, 15, 32, 36]},
        {"type": "B", "rows": [11, 12, 16, 32, 36]},
        {"type": "A", "rows": [11, 17, 34, 38]},
        {"type": "A", "rows": [11, 18, 34, 38]},
    ]
    return cells, cases


def build_download_document(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    document = spec.context.get("document", spec.entity)
    cells = {
        "D11": f"Authorized {spec.actor} session exists",
        "D12": f"Owned {document} exists and is downloadable",
        "D13": "Export or document file is generated successfully",
        "D14": f"Target {document} not found",
        "D15": "Requester has no access to target document",
        "D32": "Document download/export generated successfully",
        "D33": "No document returned",
        "D34": "Not-found or access-denied error returned",
        "B36": "Log message",
        "D36": "Document generated successfully.",
        "D37": "Requested document was not found.",
        "D38": "Document access is denied.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 14, 33, 37]},
        {"type": "A", "rows": [11, 12, 15, 34, 38]},
    ]
    return cells, cases


def build_view_profile(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Authenticated user session exists",
        "D12": "User profile data exists",
        "D13": "Role-specific profile data exists",
        "D14": "User record not found",
        "D32": "User profile returned successfully",
        "D33": "No profile returned",
        "D34": "Not-found or unauthorized error returned",
        "B36": "Log message",
        "D36": "Profile loaded successfully.",
        "D37": "Requested user profile was not found.",
        "D38": "Unauthorized profile access.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 14, 33, 37]},
    ]
    return cells, cases


def build_update_profile(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Authenticated user session exists",
        "D12": "Valid profile data (name, gender, phone, avatar, patient fields)",
        "D13": "Boundary-valid optional fields omitted or blank",
        "D14": "Duplicate CCCD or phone number exists",
        "D15": "Invalid phone or CCCD format",
        "D16": "Target user/profile not found",
        "D32": "Profile updated successfully",
        "D33": "Profile not updated",
        "D34": "Validation, conflict, or not-found error returned",
        "B36": "Log message",
        "D36": "Profile updated successfully.",
        "D37": "Profile update validation failed.",
        "D38": "Profile target was not found.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 32, 36]},
        {"type": "B", "rows": [11, 13, 32, 36]},
        {"type": "A", "rows": [11, 14, 33, 37]},
        {"type": "A", "rows": [11, 15, 33, 37]},
        {"type": "A", "rows": [11, 16, 34, 38]},
    ]
    return cells, cases


def build_create_review(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Authenticated patient session exists",
        "D12": "Completed appointment eligible for review exists",
        "D13": "Valid rating from 1 to 5",
        "D14": "Comment omitted or within allowed length",
        "D15": "Duplicate review for the same appointment exists",
        "D16": "Invalid rating or ineligible appointment",
        "D32": "Review created successfully",
        "D33": "Review not created",
        "D34": "Validation or business-rule error returned",
        "B36": "Log message",
        "D36": "Review submitted successfully.",
        "D37": "Duplicate review detected.",
        "D38": "Review request is invalid or appointment is ineligible.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 14, 32, 36]},
        {"type": "B", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 12, 15, 33, 37]},
        {"type": "A", "rows": [11, 12, 16, 34, 38]},
    ]
    return cells, cases


def build_financial_detail(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Authorized cashier/admin session exists",
        "D12": "Target invoice exists",
        "D13": "Linked financial detail and payment data exist",
        "D14": "Target invoice not found",
        "D15": "Requester has no access to financial detail",
        "D32": "Financial detail returned successfully",
        "D33": "No detail returned",
        "D34": "Not-found or access-denied error returned",
        "B36": "Log message",
        "D36": "Financial detail loaded successfully.",
        "D37": "Invoice was not found.",
        "D38": "Financial detail access is denied.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 14, 33, 37]},
        {"type": "A", "rows": [11, 12, 15, 34, 38]},
    ]
    return cells, cases


def build_issue_receipt(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Authorized cashier/admin session exists",
        "D12": "Target invoice exists and payment status = PAID",
        "D13": "Receipt has not been issued yet",
        "D14": "Invoice is unpaid",
        "D15": "Receipt already exists",
        "D16": "Invoice not found",
        "D32": "Receipt created successfully with receipt code",
        "D33": "Receipt not created",
        "D34": "Payment-status or not-found error returned",
        "B36": "Log message",
        "D36": "Receipt issued successfully.",
        "D37": "Receipt cannot be issued for current invoice state.",
        "D38": "Invoice was not found.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "A", "rows": [11, 14, 33, 37]},
        {"type": "A", "rows": [11, 12, 15, 33, 37]},
        {"type": "A", "rows": [11, 16, 34, 38]},
    ]
    return cells, cases


def build_upload_update_result(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Authorized doctor/nurse session exists",
        "D12": "Target service order exists and is eligible for result input",
        "D13": "Valid textual result note",
        "D14": "Valid PDF/image result file",
        "D15": "Unsupported file type or oversize file",
        "D16": "Target service order not found or wrong status",
        "D17": "Boundary update of result note after file already exists",
        "D32": "Result file uploaded successfully",
        "D33": "Result note updated successfully",
        "D34": "Upload/update rejected",
        "B36": "Log message",
        "D36": "Result upload or update completed successfully.",
        "D37": "Result upload blocked because file is invalid.",
        "D38": "Target order is invalid or not eligible.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 14, 32, 36]},
        {"type": "N", "rows": [11, 12, 13, 33, 36]},
        {"type": "B", "rows": [11, 12, 17, 33, 36]},
        {"type": "A", "rows": [11, 12, 15, 34, 37]},
        {"type": "A", "rows": [11, 16, 34, 38]},
    ]
    return cells, cases


def build_create_prescription(_: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    cells = {
        "D11": "Authorized doctor/nurse session exists",
        "D12": "Target medical record or appointment exists",
        "D13": "At least one valid medication item exists",
        "D14": "Boundary-valid prescription with multiple medication items",
        "D15": "Missing medication data or invalid dosage/instruction",
        "D16": "Target medical record is not found or not eligible",
        "D32": "Prescription created successfully",
        "D33": "Prescription not created",
        "D34": "Validation or business-rule error returned",
        "B36": "Log message",
        "D36": "Prescription created successfully.",
        "D37": "Prescription validation failed.",
        "D38": "Target record is invalid or not eligible.",
    }
    apply_standard_headers(cells)
    cases = [
        {"type": "N", "rows": [11, 12, 13, 32, 36]},
        {"type": "B", "rows": [11, 12, 14, 32, 36]},
        {"type": "A", "rows": [11, 12, 15, 33, 37]},
        {"type": "A", "rows": [11, 16, 34, 38]},
    ]
    return cells, cases


BUILDERS = {
    "login": build_login,
    "register": build_register,
    "forgot_reset": build_forgot_reset,
    "change_password": build_change_password,
    "view_list": build_view_list,
    "view_detail": build_view_detail,
    "create_entity": build_create_entity,
    "update_entity": build_update_entity,
    "delete_entity": build_delete_entity,
    "lock_toggle": build_lock_toggle,
    "assign_relation": build_assign_relation,
    "schedule_create": build_schedule_create,
    "schedule_update": build_schedule_update,
    "schedule_delete": build_schedule_delete,
    "appointment_admin_create": build_appointment_admin_create,
    "patient_book": build_patient_book,
    "confirm_booking": build_confirm_booking,
    "appointment_cancel_admin": build_appointment_cancel,
    "patient_cancel": build_appointment_cancel,
    "prepayment": build_prepayment,
    "download_document": build_download_document,
    "view_profile": build_view_profile,
    "update_profile": build_update_profile,
    "create_review": build_create_review,
    "financial_detail": build_financial_detail,
    "issue_receipt": build_issue_receipt,
    "upload_update_result": build_upload_update_result,
    "create_prescription": build_create_prescription,
}


def build_sheet_content(spec: FunctionSpec) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    return BUILDERS[spec.pattern](spec)


def populate_function_sheet(ws, spec: FunctionSpec) -> None:
    clear_real_cells(ws, 9, 42, 1, 20)

    function_row = spec.no + 10
    ws["C2"] = f"={FUNCTIONS_SHEET}!E{function_row}"
    ws["L2"] = f"={FUNCTIONS_SHEET}!D{function_row}"
    ws["C3"] = AUTHOR
    ws["L3"] = ""
    ws["C4"] = spec.loc
    ws["C5"] = f"{spec.requirement}. {spec.description}"

    cells, cases = build_sheet_content(spec)
    for coord, value in cells.items():
        ws[coord] = value

    for index, case in enumerate(cases, start=1):
        col = 5 + index
        ws.cell(9, col).value = f"UTCID{index:02d}"
        ws.cell(39, col).value = case["type"]
        for row_number in case["rows"]:
            ws.cell(row_number, col).value = "O"


def populate_functions_sheet(ws) -> None:
    clear_real_cells(ws, 11, 120, 1, 8)

    ws["E4"] = "Clinic_management-ISP490G5"
    ws["E5"] = "ISP490G5"
    ws["E6"] = 100
    ws["E7"] = (
        "Backend: Spring Boot 4.0.1 + Spring Security + JPA + H2/MySQL; "
        "Frontend: Next.js; Swagger UI: /swagger-ui.html"
    )

    for row_number in range(11, 11 + len(FUNCTIONS)):
        if row_number > 54:
            copy_row_style(ws, 54, row_number, 1, 8)

    for spec in FUNCTIONS:
        row_number = spec.no + 10
        sheet_name = sheet_name_for_no(spec.no)

        ws.cell(row_number, 1).value = spec.no
        ws.cell(row_number, 2).value = spec.requirement
        ws.cell(row_number, 3).value = spec.class_name
        ws.cell(row_number, 4).value = spec.function_name
        ws.cell(row_number, 5).value = spec.function_code
        ws.cell(row_number, 6).value = sheet_name
        ws.cell(row_number, 6).hyperlink = f"#{quote_sheet(sheet_name)}!A1"
        ws.cell(row_number, 6).style = "Hyperlink"
        ws.cell(row_number, 7).value = spec.description
        ws.cell(row_number, 8).value = spec.precondition


def populate_cover_sheet(ws) -> None:
    ws["B4"] = "Clinic_management-ISP490G5"
    ws["B5"] = "ISP490G5"
    ws["F5"] = "2026-04-17"
    ws["F6"] = "v0.2-full"

    ws["A12"] = "2026-04-17"
    ws["B12"] = "v0.2-full"
    ws["C12"] = "Full SRS Workbook"
    ws["D12"] = "A"
    ws["E12"] = "Expanded workbook to all mapped SRS/API functions with design-time unit test cases"
    ws["F12"] = "SRS + Swagger/API controllers + selected DTO validations"


def populate_statistics_sheet(ws) -> None:
    clear_real_cells(ws, 12, 120, 1, 9)

    ws["B4"] = "=Cover!B4"
    ws["B5"] = "=Cover!B5"
    ws["F6"] = "2026-04-17"
    ws["B7"] = (
        "Workbook covers Authentication, Staff, Customer, Department/Room, Work Shift, Scheduling, "
        "Appointment Management, Feedback, Financial, Queue, Medical Record, and Prescription flows."
    )

    data_start_row = 12
    data_end_row = data_start_row + len(FUNCTIONS) - 1
    subtotal_row = data_end_row + 2
    coverage_start_row = subtotal_row + 2

    for row_number in range(data_start_row, data_end_row + 1):
        copy_row_style(ws, 15, row_number, 1, 9)

    copy_row_style(ws, 17, subtotal_row, 1, 9)
    copy_row_style(ws, 19, coverage_start_row, 1, 5)
    copy_row_style(ws, 20, coverage_start_row + 1, 1, 5)
    copy_row_style(ws, 21, coverage_start_row + 2, 1, 5)
    copy_row_style(ws, 22, coverage_start_row + 3, 1, 5)
    copy_row_style(ws, 23, coverage_start_row + 4, 1, 5)

    for index, spec in enumerate(FUNCTIONS, start=0):
        row_number = data_start_row + index
        sheet_name = sheet_name_for_no(spec.no)
        ws[f"A{row_number}"] = spec.no
        ws[f"B{row_number}"] = f"={FUNCTIONS_SHEET}!E{spec.no + 10}"
        ws[f"C{row_number}"] = f"={quote_sheet(sheet_name)}!A7"
        ws[f"D{row_number}"] = f"={quote_sheet(sheet_name)}!C7"
        ws[f"E{row_number}"] = f"={quote_sheet(sheet_name)}!F7"
        ws[f"F{row_number}"] = f"={quote_sheet(sheet_name)}!L7"
        ws[f"G{row_number}"] = f"={quote_sheet(sheet_name)}!M7"
        ws[f"H{row_number}"] = f"={quote_sheet(sheet_name)}!N7"
        ws[f"I{row_number}"] = f"={quote_sheet(sheet_name)}!O7"

    ws[f"B{subtotal_row}"] = "Sub total"
    for col in "CDEFGHI":
        ws[f"{col}{subtotal_row}"] = f"=SUM({col}{data_start_row}:{col}{data_end_row})"

    ws[f"B{coverage_start_row}"] = "Test coverage"
    ws[f"D{coverage_start_row}"] = f"=IF(I{subtotal_row}>0,(C{subtotal_row}+D{subtotal_row})*100/I{subtotal_row},0)"
    ws[f"E{coverage_start_row}"] = "%"

    ws[f"B{coverage_start_row + 1}"] = "Test successful coverage"
    ws[f"D{coverage_start_row + 1}"] = f"=IF(I{subtotal_row}>0,C{subtotal_row}*100/I{subtotal_row},0)"
    ws[f"E{coverage_start_row + 1}"] = "%"

    ws[f"B{coverage_start_row + 2}"] = "Normal case"
    ws[f"D{coverage_start_row + 2}"] = f"=IF(I{subtotal_row}>0,F{subtotal_row}*100/I{subtotal_row},0)"
    ws[f"E{coverage_start_row + 2}"] = "%"

    ws[f"B{coverage_start_row + 3}"] = "Abnormal case"
    ws[f"D{coverage_start_row + 3}"] = f"=IF(I{subtotal_row}>0,G{subtotal_row}*100/I{subtotal_row},0)"
    ws[f"E{coverage_start_row + 3}"] = "%"

    ws[f"B{coverage_start_row + 4}"] = "Boundary case"
    ws[f"D{coverage_start_row + 4}"] = f"=IF(I{subtotal_row}>0,H{subtotal_row}*100/I{subtotal_row},0)"
    ws[f"E{coverage_start_row + 4}"] = "%"


def save_workbook(workbook) -> Path:
    workbook.save(BACKUP_PATH)
    try:
        workbook.save(WORKBOOK_PATH)
        return WORKBOOK_PATH
    except PermissionError:
        workbook.save(FALLBACK_OUTPUT_PATH)
        return FALLBACK_OUTPUT_PATH


def main() -> None:
    workbook = load_workbook(WORKBOOK_PATH)

    for spec in FUNCTIONS:
        ensure_sheet(workbook, sheet_name_for_no(spec.no))

    populate_functions_sheet(workbook[FUNCTIONS_SHEET])
    populate_cover_sheet(workbook[COVER_SHEET])
    populate_statistics_sheet(workbook[STATS_SHEET])

    for spec in FUNCTIONS:
        populate_function_sheet(workbook[sheet_name_for_no(spec.no)], spec)

    try:
        workbook.calculation.fullCalcOnLoad = True
        workbook.calculation.forceFullCalc = True
        workbook.calculation.calcMode = "auto"
    except AttributeError:
        pass

    output_path = save_workbook(workbook)
    print(f"{output_path} | functions={len(FUNCTIONS)}")


if __name__ == "__main__":
    main()