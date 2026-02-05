package com.lumie.spreadsheet.application.dto.request;

import com.lumie.spreadsheet.domain.vo.SpreadsheetPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSpreadsheetRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 1, max = 200, message = "Name must be between 1 and 200 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        Integer rowCount,

        Integer columnCount,

        SpreadsheetPermission permission
) {
}
