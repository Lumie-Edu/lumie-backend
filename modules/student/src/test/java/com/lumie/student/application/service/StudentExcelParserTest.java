package com.lumie.student.application.service;

import com.lumie.common.exception.BusinessException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void parsesCsvWhenFilenameHasCsvExtension() throws IOException {
        String csv = """
                \uFEFF"학생 이름","학생 연락처","학교명","출생 연도","학부모 연락처","메모"
                "김철수","010-1234-0001","한빛고","2008","010-1234-0002","쉼표, 포함"
                "이영희","010-9999-0001","","not-a-year","",""
                """;

        List<StudentExcelParser.StudentRow> rows = StudentExcelParser.parse(
                new ByteArrayInputStream(csv.getBytes(UTF_8)),
                "students.csv"
        );

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).rowNumber()).isEqualTo(2);
        assertThat(rows.get(0).studentMemo()).isEqualTo("쉼표, 포함");
        assertThat(rows.get(1).rowNumber()).isEqualTo(3);
        assertThat(rows.get(1).studentBirthYear()).isNull();
    }

    @Test
    void parsesCp949CsvExportedFromKoreanExcel() throws IOException {
        String csv = """
                학생 이름,학생 연락처,학교명,출생 연도,학부모 연락처,메모
                김루미,010-1234-5678,루미고,2008,010-9876-5432,신규 등록
                """;

        List<StudentExcelParser.StudentRow> rows = StudentExcelParser.parse(
                new ByteArrayInputStream(csv.getBytes(Charset.forName("x-windows-949"))),
                "students.csv"
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).name()).isEqualTo("김루미");
        assertThat(rows.get(0).phone()).isEqualTo("010-1234-5678");
        assertThat(rows.get(0).studentHighschool()).isEqualTo("루미고");
        assertThat(rows.get(0).parentPhone()).isEqualTo("010-9876-5432");
        assertThat(rows.get(0).studentMemo()).isEqualTo("신규 등록");
    }

    @Test
    void skipsBlankCsvRowsButKeepsFileRowNumbers() throws IOException {
        String csv = """
                학생 이름,학생 연락처,학교명,출생 연도,학부모 연락처,메모
                김철수,010-1234-0001,,,,
                ,,,,,
                이영희,010-9999-0001,,,,
                """;

        List<StudentExcelParser.StudentRow> rows = StudentExcelParser.parse(
                new ByteArrayInputStream(csv.getBytes(UTF_8)),
                "students.csv"
        );

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(StudentExcelParser.StudentRow::rowNumber).containsExactly(2, 4);
    }

    @Test
    void rejectsCsvWithUnclosedQuote() {
        String csv = """
                학생 이름,학생 연락처,학교명,출생 연도,학부모 연락처,메모
                "김철수,010-1234-0001,,,,
                """;

        assertThatThrownBy(() -> StudentExcelParser.parse(
                new ByteArrayInputStream(csv.getBytes(UTF_8)),
                "students.csv"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("양식이 올바르지 않습니다");
    }

    @Test
    void rejectsCsvWithTooManyDataRows() {
        StringBuilder csv = new StringBuilder("학생 이름,학생 연락처,학교명,출생 연도,학부모 연락처,메모\n");
        for (int i = 0; i < 1_001; i++) {
            csv.append("학생").append(i).append(",010-1234-0001,,,,\n");
        }

        assertThatThrownBy(() -> StudentExcelParser.parse(
                new ByteArrayInputStream(csv.toString().getBytes(UTF_8)),
                "students.csv"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("최대 1,000행");
    }

    @Test
    void corruptStreamPropagatesIOException() {
        InputStream notAnXlsx = new ByteArrayInputStream("definitely not a workbook".getBytes(UTF_8));

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
