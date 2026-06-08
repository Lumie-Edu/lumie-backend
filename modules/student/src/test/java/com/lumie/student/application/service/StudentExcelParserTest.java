package com.lumie.student.application.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudentExcelParserTest {

    @Test
    void skipsHeaderRowAndIndexesRowNumbersOneBased() throws IOException {
        byte[] bytes = workbook(sheet -> {
            writeRow(sheet, 0, "학생 이름", "학생 연락처", "학교명", "출생 연도", "학부모 연락처", "메모");
            writeRow(sheet, 1, "김철수", "01012340001", "한빛고", 2008, "01012340002", "재원");
        });

        List<StudentExcelParser.StudentRow> rows = StudentExcelParser.parse(new ByteArrayInputStream(bytes));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).rowNumber()).isEqualTo(2);
        assertThat(rows.get(0).name()).isEqualTo("김철수");
        assertThat(rows.get(0).studentBirthYear()).isEqualTo(2008);
    }

    @Test
    void skipsNullRowsButContinuesIteration() throws IOException {
        byte[] bytes = workbook(sheet -> {
            writeRow(sheet, 0, "학생 이름", "학생 연락처", "학교명", "출생 연도", "학부모 연락처", "메모");
            writeRow(sheet, 1, "이영희", "01099990001", null, null, null, null);
            writeRow(sheet, 3, "박민수", "01099990002", null, null, null, null);
        });

        List<StudentExcelParser.StudentRow> rows = StudentExcelParser.parse(new ByteArrayInputStream(bytes));

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(StudentExcelParser.StudentRow::rowNumber).containsExactly(2, 4);
        assertThat(rows).extracting(StudentExcelParser.StudentRow::name).containsExactly("이영희", "박민수");
    }

    @Test
    void coercesNumericPhoneCellToStringWithoutScientificNotation() throws IOException {
        byte[] bytes = workbook(sheet -> {
            writeRow(sheet, 0, "학생 이름", "학생 연락처", "학교명", "출생 연도", "학부모 연락처", "메모");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("최지훈");
            row.createCell(1).setCellValue(1012345678d);
        });

        List<StudentExcelParser.StudentRow> rows = StudentExcelParser.parse(new ByteArrayInputStream(bytes));

        assertThat(rows.get(0).phone()).isEqualTo("1012345678");
    }

    @Test
    void parsesIntegerStringBirthYearAndReturnsNullOnGarbage() throws IOException {
        byte[] bytes = workbook(sheet -> {
            writeRow(sheet, 0, "학생 이름", "학생 연락처", "학교명", "출생 연도", "학부모 연락처", "메모");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("정유진");
            row.createCell(3).setCellValue("2007");
            Row badRow = sheet.createRow(2);
            badRow.createCell(0).setCellValue("강하늘");
            badRow.createCell(3).setCellValue("not-a-year");
        });

        List<StudentExcelParser.StudentRow> rows = StudentExcelParser.parse(new ByteArrayInputStream(bytes));

        assertThat(rows.get(0).studentBirthYear()).isEqualTo(2007);
        assertThat(rows.get(1).studentBirthYear()).isNull();
    }

    @Test
    void emptySheetReturnsEmptyList() throws IOException {
        byte[] bytes = workbook(sheet -> writeRow(sheet, 0, "학생 이름", "학생 연락처", "학교명", "출생 연도", "학부모 연락처", "메모"));

        List<StudentExcelParser.StudentRow> rows = StudentExcelParser.parse(new ByteArrayInputStream(bytes));

        assertThat(rows).isEmpty();
    }

    @Test
    void corruptStreamPropagatesIOException() {
        InputStream notAnXlsx = new ByteArrayInputStream("definitely not a workbook".getBytes());

        assertThatThrownByParse(notAnXlsx);
    }

    private static void assertThatThrownByParse(InputStream input) {
        assertThat(catchThrowable(() -> StudentExcelParser.parse(input))).isNotNull();
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }

    private static Throwable catchThrowable(ThrowingRunnable r) {
        try { r.run(); return null; } catch (Throwable t) { return t; }
    }

    private static byte[] workbook(WorkbookBuilder builder) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            builder.build(sheet);
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void writeRow(Sheet sheet, int rowIdx, Object... values) {
        Row row = sheet.createRow(rowIdx);
        for (int c = 0; c < values.length; c++) {
            Object v = values[c];
            if (v == null) continue;
            if (v instanceof Number n) row.createCell(c).setCellValue(n.doubleValue());
            else row.createCell(c).setCellValue(v.toString());
        }
    }

    @FunctionalInterface
    private interface WorkbookBuilder { void build(Sheet sheet); }
}
