# System Test Document Analysis

Validated on: 2026-04-26  
Source workbook: `E:\DA\Report 5 _ SystemTest _Document.xlsx`

## Executive Summary

The workbook contains 332 visible system test cases across 13 functional modules. The `Test static` sheet matches the per-module case count total.

The workbook records `110 Passed` for `Authentication` Round 1, but this is not treated as verified evidence. The project owner clarified that those values were filled arbitrarily. Therefore, all 332 cases should be considered unexecuted or unverified until they are rerun in an isolated test environment.

No live E2E execution should be run against the current localhost services unless the database is isolated. The current root `docker-compose.yml` defaults the backend datasource to a remote MySQL database, so side-effectful API or browser flows can create, update, or delete real data.

## Workbook Validation

| Module | Cases | Workbook Round 1 | Round 2 | Round 3 | Verification status |
|---|---:|---:|---:|---:|---|
| Authentication | 110 | 110 Passed | Blank | Blank | Untrusted workbook entry; rerun required |
| Staff Account Management | 22 | Blank | Blank | Blank | Not executed |
| Customer Account Management | 18 | Blank | Blank | Blank | Not executed |
| Clinical Department Structure Management | 23 | Blank | Blank | Blank | Not executed |
| Working Time Slot Management | 11 | Blank | Blank | Blank | Not executed |
| Staff Working Hours Management | 14 | Blank | Blank | Blank | Not executed |
| Patient Visit Scheduling | 37 | Blank | Blank | Blank | Not executed |
| Appointment Management | 28 | Blank | Blank | Blank | Not executed |
| Feedback customer | 3 | Blank | Blank | Blank | Not executed |
| Payments & Receipts | 4 | Blank | Blank | Blank | Not executed |
| Daily Patient Queue | 13 | Blank | Blank | Blank | Not executed |
| Feedback Management | 3 | Blank | Blank | Blank | Not executed |
| Service Management | 46 | Blank | Blank | Blank | Not executed |
| **Total** | **332** | **Recorded only** | **Blank** | **Blank** | **332 require verified execution** |

Workbook defects found:

- `Clinical Department Structure M!E23` displays a formula/error value because the expected-result text is entered as a formula-like value starting with `=+ Them Khoa...` instead of plain text.
- `Authentication` has 110 rows where `Minh` is in the `Test date` column instead of the `Tester` column.
- Some modules have non-contiguous test IDs, for example `PVS-001` through `PVS-044` but only 37 visible cases. This is not automatically wrong, but it should be treated as a traceability gap unless skipped IDs are documented.

## Repo Test Readiness

Backend:

- Maven/JUnit tests exist under `BE\src\test\java`.
- `BE\mvnw.cmd test` is runnable but currently fails the baseline.
- Console summary from the latest run: `157 tests`, `0 failures`, `1 error`, `0 skipped`.
- Blocking error: `g2.be.service.AppointmentServiceTest.createAppointment_FollowUp_UsesSourceClinicalServiceAndPreferredDoctor` fails with Mockito `UnnecessaryStubbingException` at `AppointmentServiceTest.java:233` and `AppointmentServiceTest.java:235`.

Frontend:

- `clinic-management-fe\package.json` has `dev`, `build`, `start`, and `lint` scripts only.
- No Playwright/Jest/Vitest config or test files were found outside `node_modules` and `.next`.
- `npm run lint` passes.

Existing live E2E script:

- `test-e2e-flow.js` logs in, books an appointment, checks in, calls the next patient, and completes the appointment.
- It mutates backend state and should only run after local isolated data is configured.

## Testability Map

| Module | Primary testability | Existing backend coverage signals | Required roles/accounts | Required seed data |
|---|---|---|---|---|
| Authentication | UI/E2E-automatable, with backend unit coverage | `AuthControllerTest`, `AuthServiceTest`, `RefreshTokenServiceTest`, `VerificationTokenServiceTest` | Patient, Doctor, Nurse, Admin, inactive/locked user | Active users, locked users, OTP/mail mock, reset-token data |
| Staff Account Management | UI/E2E-automatable | `UserControllerTest`, `AdminUserServiceTest` | Admin or Manager | Staff users for all roles/statuses, duplicate email/phone cases |
| Customer Account Management | UI/E2E-automatable | `PatientControllerTest`, `PatientManagementControllerTest` | Admin or Receptionist/Manager depending route permissions | Patients/customers with and without national ID, duplicate phone/email cases |
| Clinical Department Structure Management | API and UI/E2E-automatable | `DepartmentServiceTest`, `RoomServiceTest`, `ClinicStructureIntegrationTest` | Admin, Manager/non-admin negative case | Departments, rooms, duplicate department/room names, locked/soft-deleted records |
| Working Time Slot Management | API and UI/E2E-automatable | `WorkingTimeSlotControllerTest` | Admin or Manager | Slots with overlapping and non-overlapping times, max-patient limits |
| Staff Working Hours Management | UI/E2E-automatable but seed-dependent | `StaffScheduleControllerTest` | Manager/Admin, Doctor, Nurse | Staff, departments, rooms, working slots, existing schedules for conflict checks |
| Patient Visit Scheduling | Blocked by missing isolated seed/data, then UI/E2E | `AppointmentControllerTest`, `AppointmentServiceTest` | Patient, Receptionist, Cashier | Available slots, doctors, patient profiles, appointment status lifecycle, payment/prepayment fixtures |
| Appointment Management | Blocked by missing isolated seed/data, then UI/E2E | `AppointmentControllerTest`, `MedicalRecordControllerTest`, `MedicalRecordServiceTest` | Patient, Receptionist, Doctor | Appointments in booked/confirmed/cancelled/completed states, health records, downloadable document data |
| Feedback customer | API/UI-automatable but depends on completed visit data | `ReviewControllerTest`, `ReviewServiceTest` | Patient | Completed appointments/services, existing review and no-review states |
| Payments & Receipts | API-automatable with external-service mocks; UI checks needed for export | `PaymentControllerTest`, `FinancialControllerTest`, `ReceiptIssuanceServiceTest`, `SupplementPaymentScopeTest` | Cashier, Patient | Invoices, paid/unpaid states, QR/cash scenarios, e-invoice mock responses |
| Daily Patient Queue | Blocked by missing isolated seed/data, then UI/E2E | `AppointmentControllerTest`, `ServiceOrderControllerTest`, `PrescriptionServiceTest` | Doctor, Nurse, Specialist Doctor | Checked-in appointments, queue ordering, examination orders, prescriptions, medical records |
| Feedback Management | API/UI-automatable | `ReviewControllerTest`, `ReviewServiceTest` | Manager/Admin | Reviews in pending/replied states, service/appointment references |
| Service Management | API and UI/E2E-automatable | `ServiceManagementServiceTest`, `ServiceOrderControllerTest` | Admin or Manager | Services by type/status, duplicate code/name, linked rooms, soft-delete/lock states |

## Isolated System Test Setup

Use an isolated environment before executing workbook cases:

1. Create a local test database or H2-compatible profile. Do not use the root `docker-compose.yml` default datasource because it points to the remote `database-clinic...rds.amazonaws.com` MySQL host.
2. Start backend with explicit local test datasource variables:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001`
3. Start frontend with `NEXT_PUBLIC_API_URL=http://localhost:8080/api`.
4. Seed deterministic accounts for Admin, Manager, Doctor, Specialist Doctor, Nurse, Receptionist, Cashier, and Patient.
5. Seed deterministic domain data for departments, rooms, services, working time slots, schedules, patients, appointments, invoices, reviews, prescriptions, and medical records.
6. Mock or sandbox external services: email/OTP, payment/QR callback, e-invoice provider, Cloudinary/file upload.
7. Reset data before each E2E run by recreating the local schema or running a cleanup script scoped to test-created IDs.

## Recommended Next Steps

1. Fix the backend test baseline by removing or making lenient the unused stubbings in `AppointmentServiceTest`.
2. Fix workbook defects before using the spreadsheet as execution evidence, especially `Clinical Department Structure M!E23` and the misplaced Authentication tester values.
3. Build an isolated seed/reset script and only then run the side-effectful system flows.
4. Add a Playwright setup for frontend system tests and map test IDs from the workbook to automated specs where practical.
5. After each verified run, write actual evidence back to a copy of the workbook, not the original source file.
