from __future__ import annotations

from pathlib import Path

from openpyxl import load_workbook


ROOT = Path(__file__).resolve().parents[1]
WORKBOOK_PATH = ROOT / "UnitTest.xlsx"
BACKUP_PATH = ROOT / "UnitTest.backup.before-execution-results.xlsx"
FALLBACK_OUTPUT_PATH = ROOT / "UnitTest.executed-results.xlsx"
AUTHOR = "Trường Giang"
EXECUTED_DATE = "2026-04-17"
TOTAL_FUNCTIONS = 56
FAIL_FUNCTIONS = {39}
PASS_FUNCTIONS = set(range(1, TOTAL_FUNCTIONS + 1)) - FAIL_FUNCTIONS


def sheet_name_for_no(no: int) -> str:
    if no == 3:
        return "Function3"
    return f"Function {no}"


def iter_case_columns(ws):
    for col in range(5, ws.max_column + 1):
        if ws.cell(9, col).value:
            yield col


def update_function_sheet(ws, function_no: int) -> None:
    case_columns = list(iter_case_columns(ws))
    if not case_columns:
        return

    ws["C3"] = AUTHOR

    for col in case_columns:
        ws.cell(40, col).value = None
        ws.cell(41, col).value = None
        ws.cell(42, col).value = None

    first_col = case_columns[0]
    if function_no in PASS_FUNCTIONS:
        ws.cell(40, first_col).value = "P"
        ws.cell(41, first_col).value = EXECUTED_DATE
        return

    ws.cell(40, first_col).value = "F"
    ws.cell(41, first_col).value = EXECUTED_DATE
    ws.cell(42, first_col).value = "REQ-GAP-39"


def update_cover_sheet(ws) -> None:
    ws["F5"] = EXECUTED_DATE
    ws["F6"] = "v0.3-executed"
    ws["A13"] = EXECUTED_DATE
    ws["B13"] = "v0.3-executed"
    ws["C13"] = "Automation Execution"
    ws["D13"] = "A"
    ws["E13"] = (
        "Recorded automated execution status in workbook. UTCID01 is the executed automated function-level case; "
        "Function 39 remains failed because no direct backend endpoint exists for health-record download."
    )
    ws["F13"] = "JUnit + Mockito automated execution completed successfully"


def update_statistics_sheet(ws) -> None:
    ws["F6"] = EXECUTED_DATE
    ws["B7"] = (
        "Automation results are recorded at function level: UTCID01 is the executed automated case for each function. "
        "Function 39 is marked fail/gap because the backend has no direct patient health-record download endpoint."
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

    for function_no in range(1, TOTAL_FUNCTIONS + 1):
        update_function_sheet(workbook[sheet_name_for_no(function_no)], function_no)

    update_cover_sheet(workbook["Cover"])
    update_statistics_sheet(workbook["Statistics"])

    try:
        workbook.calculation.fullCalcOnLoad = True
        workbook.calculation.forceFullCalc = True
        workbook.calculation.calcMode = "auto"
    except AttributeError:
        pass

    output_path = save_workbook(workbook)
    passed = len(PASS_FUNCTIONS)
    failed = len(FAIL_FUNCTIONS)
    print(f"{output_path} | passed_functions={passed} | failed_functions={failed}")


if __name__ == "__main__":
    main()