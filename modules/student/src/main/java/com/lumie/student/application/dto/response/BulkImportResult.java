package com.lumie.student.application.dto.response;

import java.util.List;

public record BulkImportResult(
    int totalRows,
    int successCount,
    int failureCount,
    List<RowError> errors
) {
    /**
     * Per-row, per-column error detail returned by the bulk import endpoint.
     *
     * @param rowNumber 1-indexed (header = row 1, first data row = row 2)
     * @param column    offending column name ({@code null} for whole-row errors
     *                  such as duplicates)
     * @param value     raw value the teacher typed — lets them grep their spreadsheet;
     *                  {@code null} when not applicable (e.g. missing required field)
     * @param code      stable error code for FE i18n; reuses {@code StudentErrorCode.code}
     * @param message   Korean user-facing description
     */
    public record RowError(
        int rowNumber,
        String column,
        String value,
        String code,
        String message
    ) {}

    public static BulkImportResult partial(int totalRows, int successCount, List<RowError> errors) {
        return new BulkImportResult(totalRows, successCount, totalRows - successCount, errors);
    }
}
