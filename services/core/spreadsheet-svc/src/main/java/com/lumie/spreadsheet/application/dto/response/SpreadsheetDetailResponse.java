package com.lumie.spreadsheet.application.dto.response;

import com.lumie.spreadsheet.domain.entity.Spreadsheet;
import com.lumie.spreadsheet.domain.vo.CellData;
import com.lumie.spreadsheet.domain.vo.SpreadsheetPermission;

import java.time.LocalDateTime;
import java.util.Map;

public record SpreadsheetDetailResponse(
        Long id,
        String name,
        String description,
        Integer rowCount,
        Integer columnCount,
        Map<String, Integer> columnWidths,
        Map<String, Integer> rowHeights,
        Map<String, CellData> cells,
        Long ownerId,
        SpreadsheetPermission permission,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SpreadsheetDetailResponse from(Spreadsheet spreadsheet) {
        return new SpreadsheetDetailResponse(
                spreadsheet.getId(),
                spreadsheet.getName(),
                spreadsheet.getDescription(),
                spreadsheet.getRowCount(),
                spreadsheet.getColumnCount(),
                spreadsheet.getColumnWidths(),
                spreadsheet.getRowHeights(),
                spreadsheet.getCells(),
                spreadsheet.getOwnerId(),
                spreadsheet.getPermission(),
                spreadsheet.getCreatedAt(),
                spreadsheet.getUpdatedAt()
        );
    }
}
