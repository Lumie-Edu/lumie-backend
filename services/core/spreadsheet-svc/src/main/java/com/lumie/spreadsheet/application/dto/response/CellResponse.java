package com.lumie.spreadsheet.application.dto.response;

import com.lumie.spreadsheet.domain.vo.CellData;
import com.lumie.spreadsheet.domain.vo.CellStyle;

public record CellResponse(
        String address,
        String value,
        String displayValue,
        String formula,
        CellStyle style
) {
    public static CellResponse from(String address, CellData data) {
        return new CellResponse(
                address,
                data.value(),
                data.displayValue(),
                data.formula(),
                data.style()
        );
    }
}
