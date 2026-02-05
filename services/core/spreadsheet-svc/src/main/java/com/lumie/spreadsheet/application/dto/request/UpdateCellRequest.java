package com.lumie.spreadsheet.application.dto.request;

import com.lumie.spreadsheet.domain.vo.CellStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateCellRequest(
        @NotBlank(message = "Cell address is required")
        @Pattern(regexp = "^[A-Z]+[0-9]+$", message = "Invalid cell address format (e.g., A1, B2, AA10)")
        String address,

        String value,

        String formula,

        CellStyle style
) {
}
