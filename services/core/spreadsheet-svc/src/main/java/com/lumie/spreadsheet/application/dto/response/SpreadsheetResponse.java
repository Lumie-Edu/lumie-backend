package com.lumie.spreadsheet.application.dto.response;

import com.lumie.spreadsheet.domain.entity.Spreadsheet;
import com.lumie.spreadsheet.domain.vo.SpreadsheetPermission;

import java.time.LocalDateTime;

public record SpreadsheetResponse(
        Long id,
        String name,
        String description,
        Integer rowCount,
        Integer columnCount,
        Long ownerId,
        SpreadsheetPermission permission,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SpreadsheetResponse from(Spreadsheet spreadsheet) {
        return new SpreadsheetResponse(
                spreadsheet.getId(),
                spreadsheet.getName(),
                spreadsheet.getDescription(),
                spreadsheet.getRowCount(),
                spreadsheet.getColumnCount(),
                spreadsheet.getOwnerId(),
                spreadsheet.getPermission(),
                spreadsheet.getCreatedAt(),
                spreadsheet.getUpdatedAt()
        );
    }
}
