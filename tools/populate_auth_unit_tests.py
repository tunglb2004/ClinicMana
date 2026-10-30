from __future__ import annotations

from pathlib import Path

from openpyxl import load_workbook
from openpyxl.cell.cell import MergedCell


ROOT = Path(__file__).resolve().parents[1]
WORKBOOK_PATH = ROOT / "UnitTest.xlsx"
BACKUP_PATH = ROOT / "UnitTest.backup.before-auth.xlsx"
FALLBACK_OUTPUT_PATH = ROOT / "UnitTest.auth-start.xlsx"
AUTHOR = "Trường Giang"


FUNCTION_ROWS = {
    "Function 1": 11,
    "Function 2": 12,
    "Function3": 13,
    "Function 4": 14,
}


SHEETS = {
    "Function 1": {
        "loc": 42,
        "requirement": (
            "SRS 3.2.1 Login. Authenticate user by registered email or phone number and password, "
            "then return access token, refresh token, and role-aware user info."
        ),
        "cells": {
            "A10": "Condition",
            "B10": "Precondition",
            "D11": "Registered active account exists",
            "D12": "Account locked/inactive",
            "B14": "Identifier",
            "D15": "Registered email",
            "D16": "Registered phone number",
            "D17": "Registered phone with spaces/+84 normalized",
            "D18": "Unregistered email/phone",
            "D19": "Invalid identifier format",
            "D20": "Blank identifier",
            "B22": "Password",
            "D23": "Correct password",
            "D24": "Incorrect password",
            "D25": "Blank password",
            "A31": "Confirm",
            "B31": "Return",
            "D32": "Access token + refresh token + user info returned",
            "D33": "No token returned",
            "B35": "Log message",
            "D36": "Dang nhap thanh cong.",
            "D37": "Dang nhap that bai. Vui long kiem tra lai email/ten dang nhap hoac mat khau.",
            "D38": "Tai khoan cua ban da bi khoa. Vui long lien he quan tri he thong.",
            "A39": "Result",
            "B39": "Type(N : Normal, A : Abnormal, B : Boundary)",
            "B40": "Passed/Failed",
            "B41": "Executed Date",
            "B42": "Defect ID",
        },
        "cases": [
            {"type": "N", "rows": [11, 15, 23, 32, 36]},
            {"type": "N", "rows": [11, 16, 23, 32, 36]},
            {"type": "B", "rows": [11, 17, 23, 32, 36]},
            {"type": "A", "rows": [11, 15, 24, 33, 37]},
            {"type": "A", "rows": [18, 23, 33, 37]},
            {"type": "A", "rows": [19, 23, 33, 37]},
            {"type": "A", "rows": [20, 23, 33, 37]},
            {"type": "A", "rows": [11, 15, 25, 33, 37]},
            {"type": "A", "rows": [12, 15, 23, 33, 38]},
        ],
    },
    "Function 2": {
        "loc": 78,
        "requirement": (
            "SRS 3.2.2 Register. Validate registration data, send OTP, verify OTP, and create a new "
            "patient account after successful OTP confirmation."
        ),
        "cells": {
            "A10": "Condition",
            "B10": "Precondition",
            "D11": "User is not authenticated",
            "D12": "Email/phone are not registered",
            "D13": "Duplicate email exists",
            "D14": "Duplicate phone exists",
            "B16": "Full name",
            "D16": "Valid alphabetic full name",
            "D17": "Full name contains digits/special chars",
            "B18": "Email",
            "D18": "Valid email format",
            "D19": "Invalid email format",
            "B20": "Phone number",
            "D20": "Valid Vietnamese phone format",
            "D21": "Invalid phone format",
            "D22": "Boundary-valid phone length (10-11 digits)",
            "B23": "Password",
            "D23": "Strong password",
            "D24": "Weak password",
            "B25": "Confirm password",
            "D25": "Confirmation matches password",
            "D26": "Confirmation mismatch",
            "B27": "OTP",
            "D27": "Valid unused OTP",
            "D28": "Invalid OTP",
            "D29": "Expired OTP",
            "D30": "Resent OTP valid",
            "A31": "Confirm",
            "B31": "Return",
            "D32": "OTP send accepted (email + expiresInMinutes returned)",
            "D33": "Account created with ROLE_PATIENT and patient profile initialized",
            "D34": "Registration blocked; no account created",
            "B36": "Log message",
            "D37": "Duplicate or validation error displayed",
            "D38": "OTP invalid or expired; user must re-enter or resend",
            "A39": "Result",
            "B39": "Type(N : Normal, A : Abnormal, B : Boundary)",
            "B40": "Passed/Failed",
            "B41": "Executed Date",
            "B42": "Defect ID",
        },
        "cases": [
            {"type": "N", "rows": [11, 12, 16, 18, 20, 23, 25, 27, 32, 33]},
            {"type": "B", "rows": [11, 12, 16, 18, 22, 23, 25, 27, 32, 33]},
            {"type": "A", "rows": [11, 13, 16, 18, 20, 23, 25, 34, 37]},
            {"type": "A", "rows": [11, 14, 16, 18, 20, 23, 25, 34, 37]},
            {"type": "A", "rows": [11, 12, 17, 18, 20, 23, 25, 34, 37]},
            {"type": "A", "rows": [11, 12, 16, 19, 20, 23, 25, 34, 37]},
            {"type": "A", "rows": [11, 12, 16, 18, 21, 23, 25, 34, 37]},
            {"type": "A", "rows": [11, 12, 16, 18, 20, 24, 25, 34, 37]},
            {"type": "A", "rows": [11, 12, 16, 18, 20, 23, 26, 34, 37]},
            {"type": "A", "rows": [11, 12, 16, 18, 20, 23, 25, 28, 32, 34, 38]},
            {"type": "A", "rows": [11, 12, 16, 18, 20, 23, 25, 29, 32, 34, 38]},
            {"type": "B", "rows": [11, 12, 16, 18, 20, 23, 25, 30, 32, 33]},
        ],
    },
    "Function3": {
        "loc": 64,
        "requirement": (
            "SRS 3.2.2.3 Forgot Password. Validate email, send recovery OTP, then allow password reset "
            "with a valid OTP and valid new password."
        ),
        "cells": {
            "A10": "Condition",
            "B10": "Precondition",
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
            "A31": "Confirm",
            "B31": "Return",
            "D32": "OTP sent to email",
            "D33": "Password updated successfully",
            "D34": "Password not updated",
            "B36": "Log message",
            "D37": "Ma OTP da duoc gui den email cua ban.",
            "D38": "Email khong ton tai / OTP khong hop le hoac da het han / validation error.",
            "A39": "Result",
            "B39": "Type(N : Normal, A : Abnormal, B : Boundary)",
            "B40": "Passed/Failed",
            "B41": "Executed Date",
            "B42": "Defect ID",
        },
        "cases": [
            {"type": "N", "rows": [11, 15, 32, 37]},
            {"type": "N", "rows": [11, 15, 19, 25, 27, 32, 33]},
            {"type": "A", "rows": [12, 15, 34, 38]},
            {"type": "A", "rows": [16, 34, 38]},
            {"type": "A", "rows": [11, 15, 20, 25, 27, 32, 34, 38]},
            {"type": "A", "rows": [11, 15, 21, 25, 27, 32, 34, 38]},
            {"type": "A", "rows": [11, 15, 19, 26, 27, 32, 34, 38]},
            {"type": "A", "rows": [11, 15, 19, 25, 28, 32, 34, 38]},
            {"type": "B", "rows": [11, 15, 22, 25, 27, 32, 33]},
        ],
    },
    "Function 4": {
        "loc": 36,
        "requirement": (
            "SRS 3.2.2.4 Change Password. For an authenticated user, validate current password, new "
            "password policy, confirmation matching, then update password."
        ),
        "cells": {
            "A10": "Condition",
            "B10": "Precondition",
            "D11": "Authenticated user session exists",
            "D12": "Current password is correct",
            "D13": "Current password is incorrect",
            "B15": "New password",
            "D16": "Strong password",
            "D17": "Weak password",
            "D18": "Boundary-valid minimum length (8 chars)",
            "D19": "Boundary-valid maximum length (50 chars)",
            "B21": "Confirmation",
            "D22": "Confirmation matches new password",
            "D23": "Confirmation mismatch",
            "B25": "Current password",
            "D25": "Blank current password",
            "A31": "Confirm",
            "B31": "Return",
            "D32": "Password updated successfully",
            "D33": "Password not updated",
            "B35": "Log message",
            "D36": "Cap nhat mat khau thanh cong.",
            "D37": "Mat khau hien tai khong dung.",
            "D38": "Mat khau moi va xac nhan mat khau khong khop / validation error.",
            "A39": "Result",
            "B39": "Type(N : Normal, A : Abnormal, B : Boundary)",
            "B40": "Passed/Failed",
            "B41": "Executed Date",
            "B42": "Defect ID",
        },
        "cases": [
            {"type": "N", "rows": [11, 12, 16, 22, 32, 36]},
            {"type": "B", "rows": [11, 12, 18, 22, 32, 36]},
            {"type": "B", "rows": [11, 12, 19, 22, 32, 36]},
            {"type": "A", "rows": [11, 13, 16, 22, 33, 37]},
            {"type": "A", "rows": [11, 12, 17, 22, 33, 38]},
            {"type": "A", "rows": [11, 12, 16, 23, 33, 38]},
            {"type": "A", "rows": [11, 25, 16, 22, 33, 38]},
        ],
    },
}


def ensure_function4(workbook):
    if "Function 4" in workbook.sheetnames:
        return workbook["Function 4"]

    template = workbook["Function 1"]
    new_sheet = workbook.copy_worksheet(template)
    new_sheet.title = "Function 4"
    return new_sheet


def clear_real_cells(ws, min_row: int, max_row: int, min_col: int, max_col: int) -> None:
    for row in ws.iter_rows(min_row=min_row, max_row=max_row, min_col=min_col, max_col=max_col):
        for cell in row:
            if isinstance(cell, MergedCell):
                continue
            cell.value = None


def populate_function_sheet(ws, function_row: int, sheet_data: dict) -> None:
    clear_real_cells(ws, 9, 42, 1, 20)

    ws["C2"] = f"=Functions!E{function_row}"
    ws["L2"] = f"=Functions!D{function_row}"
    ws["C3"] = AUTHOR
    ws["L3"] = ""
    ws["C4"] = sheet_data["loc"]
    ws["C5"] = sheet_data["requirement"]

    for coord, value in sheet_data["cells"].items():
        ws[coord] = value

    for index, case in enumerate(sheet_data["cases"], start=1):
        col = 5 + index
        ws.cell(9, col).value = f"UTCID{index:02d}"
        ws.cell(39, col).value = case["type"]
        for row_number in case["rows"]:
            ws.cell(row_number, col).value = "O"


def populate_functions_sheet(ws) -> None:
    ws["E4"] = "Clinic_management-ISP490G5"
    ws["E5"] = "ISP490G5"
    ws["E6"] = 100
    ws["E7"] = (
        "Backend: Spring Boot 4.0.1 + Spring Security + JPA + H2/MySQL; "
        "Frontend: Next.js; Swagger UI: /swagger-ui.html"
    )

    rows = {
        11: {
            "no": 1,
            "requirement": "3.2.1 Login",
            "class_name": "AuthController / AuthService",
            "function_name": "Login",
            "function_code": "AUTH_LOGIN",
            "sheet_name": "Function 1",
            "description": "Authenticate by registered email/phone and password, then return JWT tokens and user info.",
            "precondition": "Registered account exists; account should be active.",
        },
        12: {
            "no": 2,
            "requirement": "3.2.2 Register",
            "class_name": "AuthController / AuthService / VerificationTokenService",
            "function_name": "Register + Verify OTP",
            "function_code": "AUTH_REGISTER",
            "sheet_name": "Function 2",
            "description": "Validate registration data, send OTP, verify OTP, and create patient account.",
            "precondition": "User unauthenticated; email/phone not registered; OTP service available.",
        },
        13: {
            "no": 3,
            "requirement": "3.2.2.3 Forgot Password",
            "class_name": "AuthController / UserService",
            "function_name": "Forgot Password + Reset Password",
            "function_code": "AUTH_FORGOT_RESET",
            "sheet_name": "Function3",
            "description": "Send recovery OTP to existing email and reset password with valid OTP.",
            "precondition": "Email exists in system; email delivery available.",
        },
        14: {
            "no": 4,
            "requirement": "3.2.2.4 Change Password",
            "class_name": "UserController / UserService",
            "function_name": "Change Password",
            "function_code": "AUTH_CHANGE_PASSWORD",
            "sheet_name": "Function 4",
            "description": "Authenticated user changes password after current-password and confirmation checks.",
            "precondition": "Authenticated user session exists.",
        },
    }

    for row_number, data in rows.items():
        ws.cell(row_number, 1).value = data["no"]
        ws.cell(row_number, 2).value = data["requirement"]
        ws.cell(row_number, 3).value = data["class_name"]
        ws.cell(row_number, 4).value = data["function_name"]
        ws.cell(row_number, 5).value = data["function_code"]
        ws.cell(row_number, 6).value = data["sheet_name"]
        ws.cell(row_number, 7).value = data["description"]
        ws.cell(row_number, 8).value = data["precondition"]


def populate_cover_sheet(ws) -> None:
    ws["B4"] = "Clinic_management-ISP490G5"
    ws["B5"] = "ISP490G5"
    ws["F5"] = "2026-04-17"
    ws["F6"] = "v0.1-auth"
    ws["A11"] = "2026-04-17"
    ws["B11"] = "v0.1-auth"
    ws["C11"] = "Authentication"
    ws["D11"] = "A"
    ws["E11"] = "Initial workbook population for Authentication module"
    ws["F11"] = "SRS + BE API/AuthService/UserService"


def populate_statistics_sheet(ws) -> None:
    ws["B4"] = "=Cover!B4"
    ws["B5"] = "=Cover!B5"
    ws["F6"] = "2026-04-17"
    ws["B7"] = "Phase 1 includes Authentication module only: Login, Register, Forgot/Reset Password, Change Password."

    rows = {
        12: (1, "=Functions!E11", "Function 1"),
        13: (2, "=Functions!E12", "Function 2"),
        14: (3, "=Functions!E13", "Function3"),
        15: (4, "=Functions!E14", "Function 4"),
    }

    for row_number, (index, code_formula, sheet_name) in rows.items():
        ws[f"A{row_number}"] = index
        ws[f"B{row_number}"] = code_formula
        ws[f"C{row_number}"] = f"='{sheet_name}'!A7"
        ws[f"D{row_number}"] = f"='{sheet_name}'!C7"
        ws[f"E{row_number}"] = f"='{sheet_name}'!F7"
        ws[f"F{row_number}"] = f"='{sheet_name}'!L7"
        ws[f"G{row_number}"] = f"='{sheet_name}'!M7"
        ws[f"H{row_number}"] = f"='{sheet_name}'!N7"
        ws[f"I{row_number}"] = f"='{sheet_name}'!O7"

    for row_number in (16,):
        for col in range(1, 10):
            ws.cell(row_number, col).value = None

    for col in "CDEFGHI":
        ws[f"{col}17"] = f"=SUM({col}12:{col}15)"


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

    ensure_function4(workbook)
    populate_functions_sheet(workbook["Functions"])
    populate_cover_sheet(workbook["Cover"])
    populate_statistics_sheet(workbook["Statistics"])

    for sheet_name, function_row in FUNCTION_ROWS.items():
        populate_function_sheet(workbook[sheet_name], function_row, SHEETS[sheet_name])

    try:
        workbook.calculation.fullCalcOnLoad = True
        workbook.calculation.forceFullCalc = True
        workbook.calculation.calcMode = "auto"
    except AttributeError:
        pass

    output_path = save_workbook(workbook)
    print(output_path)


if __name__ == "__main__":
    main()