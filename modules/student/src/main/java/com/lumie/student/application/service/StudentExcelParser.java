package com.lumie.student.application.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses bulk-import Excel sheets for {@link StudentCommandService#bulkImportStudents}.
 *
 * <p>Apache POI lives ONLY here so the application service stays domain-focused
 * and unit-testable without an Excel fixture. Returns raw rows; business
 * validation (name required, quota, user creation) is the caller's job.
 */
final class StudentExcelParser {

    private StudentExcelParser() {}

    /** One parsed row. {@code rowNumber} is 1-indexed and matches the Excel row label. */
    record StudentRow(
            int rowNumber,
            String name,
            String phone,
            String studentHighschool,
            Integer studentBirthYear,
            String parentPhone,
            String studentMemo
    ) {}

    static List<StudentRow> parse(InputStream input) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null ||
                !"학생 이름".equals(getCellStringValue(headerRow.getCell(0))) ||
                !"학생 연락처".equals(getCellStringValue(headerRow.getCell(1)))) {
                throw new com.lumie.common.exception.BusinessException(
                        com.lumie.student.domain.exception.StudentErrorCode.INVALID_EXCEL_FILE,
                        "업로드된 엑셀 파일의 양식이 올바르지 않습니다. 정해진 템플릿을 사용해주세요."
                );
            }

            List<StudentRow> rows = new ArrayList<>(Math.max(0, lastRowNum));
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                rows.add(new StudentRow(
                        i + 1,
                        getCellStringValue(row.getCell(0)),
                        getCellStringValue(row.getCell(1)),
                        getCellStringValue(row.getCell(2)),
                        getCellIntegerValue(row.getCell(3)),
                        getCellStringValue(row.getCell(4)),
                        getCellStringValue(row.getCell(5))
                ));
            }
            return rows;
        }
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private static Integer getCellIntegerValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }
}
