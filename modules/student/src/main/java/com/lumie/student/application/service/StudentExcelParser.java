package com.lumie.student.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.student.domain.exception.StudentErrorCode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses bulk-import spreadsheet files for {@link StudentCommandService#bulkImportStudents}.
 *
 * <p>Apache POI and CSV parsing live ONLY here so the application service stays
 * domain-focused and unit-testable without a file-format fixture. Returns raw
 * rows; business validation (name required, quota, user creation) is the
 * caller's job.
 */
final class StudentExcelParser {

    private static final int MAX_DATA_ROWS = 1_000;
    private static final int MAX_CELLS_PER_ROW = 32;
    private static final int MAX_CSV_BYTES = 5 * 1024 * 1024;
    private static final List<Charset> CSV_CHARSETS = List.of(
            StandardCharsets.UTF_8,
            Charset.forName("x-windows-949"),
            Charset.forName("EUC-KR")
    );

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
        return parseXlsx(input);
    }

    static List<StudentRow> parse(InputStream input, String filename) throws IOException {
        if (isCsv(filename)) {
            return parseCsv(input);
        }
        return parseXlsx(input);
    }

    private static List<StudentRow> parseXlsx(InputStream input) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null ||
                !"학생 이름".equals(getCellStringValue(headerRow.getCell(0))) ||
                !"학생 연락처".equals(getCellStringValue(headerRow.getCell(1)))) {
                throw invalidTemplate();
            }

            if (lastRowNum > MAX_DATA_ROWS) {
                throw tooManyRows();
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

    private static List<StudentRow> parseCsv(InputStream input) throws IOException {
        List<List<String>> records = parseCsvRecords(input);
        if (records.isEmpty() ||
            !"학생 이름".equals(getCsvCell(records.get(0), 0)) ||
            !"학생 연락처".equals(getCsvCell(records.get(0), 1))) {
            throw invalidTemplate();
        }

        List<StudentRow> rows = new ArrayList<>(Math.max(0, records.size() - 1));
        for (int i = 1; i < records.size(); i++) {
            List<String> record = records.get(i);
            if (isBlankRecord(record)) continue;
            rows.add(new StudentRow(
                    i + 1,
                    getCsvCell(record, 0),
                    getCsvCell(record, 1),
                    getCsvCell(record, 2),
                    getCsvIntegerValue(record, 3),
                    getCsvCell(record, 4),
                    getCsvCell(record, 5)
            ));
            if (rows.size() > MAX_DATA_ROWS) {
                throw tooManyRows();
            }
        }
        return rows;
    }

    private static List<List<String>> parseCsvRecords(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(MAX_CSV_BYTES + 1);
        if (bytes.length > MAX_CSV_BYTES) {
            throw invalidTemplate();
        }

        for (Charset charset : CSV_CHARSETS) {
            String text;
            try {
                text = decodeCsv(bytes, charset);
            } catch (CharacterCodingException e) {
                continue;
            }

            List<List<String>> records = parseCsvText(text);
            if (hasExpectedCsvHeader(records)) {
                return records;
            }
        }
        throw invalidTemplate();
    }

    private static String decodeCsv(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private static List<List<String>> parseCsvText(String text) throws IOException {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        boolean isFirstCharacter = true;

        try (Reader reader = new BufferedReader(new StringReader(text))) {
            int raw;
            while ((raw = reader.read()) != -1) {
                char ch = (char) raw;

                if (isFirstCharacter) {
                    isFirstCharacter = false;
                    if (ch == '\uFEFF') {
                        continue;
                    }
                }

                if (inQuotes) {
                    if (ch == '"') {
                        reader.mark(1);
                        int nextRaw = reader.read();
                        if (nextRaw == '"') {
                            cell.append('"');
                        } else {
                            inQuotes = false;
                            if (nextRaw != -1) {
                                reader.reset();
                            }
                        }
                    } else {
                        cell.append(ch);
                    }
                    continue;
                }

                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    addCsvCell(record, cell);
                    cell.setLength(0);
                } else if (ch == '\r' || ch == '\n') {
                    addCsvCell(record, cell);
                    cell.setLength(0);
                    addCsvRecord(records, record);
                    record = new ArrayList<>();
                    if (ch == '\r') {
                        reader.mark(1);
                        int nextRaw = reader.read();
                        if (nextRaw != '\n' && nextRaw != -1) {
                            reader.reset();
                        }
                    }
                } else {
                    cell.append(ch);
                }
            }
        }

        if (inQuotes) {
            throw invalidTemplate();
        }

        if (!record.isEmpty() || !cell.isEmpty()) {
            addCsvCell(record, cell);
            addCsvRecord(records, record);
        }
        return records;
    }

    private static boolean hasExpectedCsvHeader(List<List<String>> records) {
        return !records.isEmpty() &&
                "학생 이름".equals(getCsvCell(records.get(0), 0)) &&
                "학생 연락처".equals(getCsvCell(records.get(0), 1));
    }

    private static void addCsvRecord(List<List<String>> records, List<String> record) throws IOException {
        if (records.size() > MAX_DATA_ROWS) {
            throw tooManyRows();
        }
        records.add(record);
    }

    private static void addCsvCell(List<String> record, StringBuilder cell) throws IOException {
        if (record.size() >= MAX_CELLS_PER_ROW) {
            throw tooManyColumns();
        }
        record.add(cell.toString());
    }

    private static String getCsvCell(List<String> record, int index) {
        if (index >= record.size()) return null;
        return record.get(index);
    }

    private static Integer getCsvIntegerValue(List<String> record, int index) {
        String value = getCsvCell(record, index);
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlankRecord(List<String> record) {
        return record.stream().allMatch(value -> value == null || value.isBlank());
    }

    private static boolean isCsv(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private static BusinessException invalidTemplate() {
        return new BusinessException(
                StudentErrorCode.INVALID_EXCEL_FILE,
                "업로드된 파일의 양식이 올바르지 않습니다. 정해진 템플릿을 사용해주세요."
        );
    }

    private static BusinessException tooManyRows() {
        return new BusinessException(
                StudentErrorCode.INVALID_EXCEL_FILE,
                "학생 일괄 등록은 최대 1,000행까지 업로드할 수 있습니다."
        );
    }

    private static BusinessException tooManyColumns() {
        return new BusinessException(
                StudentErrorCode.INVALID_EXCEL_FILE,
                "CSV 행은 최대 32개 컬럼까지 업로드할 수 있습니다."
        );
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
