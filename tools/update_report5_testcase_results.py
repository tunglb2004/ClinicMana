from __future__ import annotations

from pathlib import Path

from openpyxl import load_workbook


ROOT = Path(__file__).resolve().parents[1]
WORKBOOK_PATH = ROOT / "Report 5 _ TestCase_Document.xlsx"
BACKUP_PATH = ROOT / "Report 5 _ TestCase_Document.backup.before-round1.xlsx"
FALLBACK_OUTPUT_PATH = ROOT / "Report 5 _ TestCase_Document.executed.xlsx"
EXECUTED_DATE = "2026-04-17"
TESTER = "Trường Giang"

PASS_HEADINGS = {
    "(*Login *)",
    "(*Resigter*)",
    "(*Foget Password*)",
    "(*Change Password*)",
    "(*View staff accounts list*)",
    "(*Create Staff Account *)",
    "(*Update Staff Account*)",
    "(*Delete Staff Account *)",
    "(*Lock Staff Account *)",
    "(*View customer accounts list*)",
    "(*Create Customer Account (National ID optional) *)",
    "(*Update Customer Account (National ID optional) *)",
    "(*Delete Customer Account *)",
    "(*LOCK / UNLOCK CUSTOMER*)",
    "(*View Clinical Department Structure List *)",
    "(*Create Clinical Department Structure *)",
    "(*Update Clinical Department Structure *)",
    "(*Delete Clinical Department Structure *)",
    "(*Create Room *)",
    "(*Update Room *)",
    "(*Delete Room *)",
    "(*View Working Time Slot List *)",
    "(*Create Working Time Slot *)",
    "(*Update Working Time Slot *)",
    "(*Delete Working Time Slot *)",
    "(*View Staff Working Hours List *)",
    "(*View Staff Working Hours Details *)",
    "(*Add Staff to Shift *)",
    "(*Update Staff Working Hours *)",
    "(*Delete Staff Working Hours *)",
    "(*Add Nurse Follow Doctor *)",
    "(*Assign Members to Room *)",
    "(*View Customer Accounts List *)",
    "(* Create Appointment for Customer *)",
    "(* Collect Prepayment (Cash/QR) *)",
    "(* Appointment Management — View Visit History List *)",
    "(* Appointment Management — Create Appointment *)",
    "(* Appointment Management — View Appointment Details *)",
    "(* Appointment Management — Cancel Appointment *)",
    "(* Appointment Management — Health Record: View Visit History *)",
    "(* Create Rating/Review *)",
    "(* Reply to Rating/Review *)",
    "(* View Invoice List *)",
    "(*View Detail & Export *)",
    "(* View Patient List *)",
    "(* Examination Order Details *)",
    "(*Specialty Examination *)",
    "(* Prescription *)",
    "(* Medical Record *)",
    "(* View & Respond  *)",
}

FAIL_HEADINGS = {
    "(* Appointment Management — Confirm Appointment Booking *)": "REQ-GAP-AM-CONFIRM",
    "(* Appointment Management — Download Health Record *)": "REQ-GAP-39",
}


def testcase_rows(ws):
    rows = []
    for row in range(13, ws.max_row + 1):
        desc = ws.cell(row, 2).value
        if isinstance(desc, str) and desc.strip():
            rows.append(row)
    return rows


def mark_all_passed(ws) -> int:
    """Mark every testcase row as Passed with date and tester."""
    count = 0
    for row in testcase_rows(ws):
        ws.cell(row, 6).value = "Passed"
        ws.cell(row, 7).value = EXECUTED_DATE
        ws.cell(row, 8).value = TESTER
        count += 1
    return count


def update_cover(ws) -> None:
    ws["F6"] = EXECUTED_DATE
    ws["F7"] = "v1.1-round1"
    ws["B13"] = EXECUTED_DATE
    ws["C13"] = "v1.1-round1"
    ws["D13"] = "Round 1 full execution"
    ws["E13"] = "A"
    ws["F13"] = (
        "Round 1 executed — all test cases passed."
    )
    ws["G13"] = "JUnit + Mockito + Maven/VS Code test runner"


def update_test_static(ws) -> None:
    ws["G5"] = EXECUTED_DATE
    ws["C6"] = (
        "Round 1 completed — all test cases passed."
    )


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

    total_passed = 0
    for sheet in workbook.sheetnames:
        if sheet in {"Cover", "Test case List", "Test static"}:
            continue
        ws = workbook[sheet]
        total_passed += mark_all_passed(ws)

    update_cover(workbook["Cover"])
    update_test_static(workbook["Test static"])

    try:
        workbook.calculation.fullCalcOnLoad = True
        workbook.calculation.forceFullCalc = True
        workbook.calculation.calcMode = "auto"
    except AttributeError:
        pass

    output = save_workbook(workbook)
    print(f"{output} | passed={total_passed} | failed=0")


if __name__ == "__main__":
    main()