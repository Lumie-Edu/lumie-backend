package com.lumie.academy.application.dto;

import java.util.List;

public record BulkImportResult(
    int totalRows,
    int successCount,
    int failureCount,
    List<RowError> errors
) {
    public record RowError(int rowNumber, String field, String message) {
    }

    public static BulkImportResult success(int totalRows) {
        return new BulkImportResult(totalRows, totalRows, 0, List.of());
    }

    public static BulkImportResult partial(int totalRows, int successCount, List<RowError> errors) {
        return new BulkImportResult(totalRows, successCount, totalRows - successCount, errors);
    }
}
