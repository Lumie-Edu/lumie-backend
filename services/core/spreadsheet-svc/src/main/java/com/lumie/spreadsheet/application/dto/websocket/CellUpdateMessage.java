package com.lumie.spreadsheet.application.dto.websocket;

import com.lumie.spreadsheet.domain.vo.CellStyle;

public record CellUpdateMessage(
        String cellAddress,
        String value,
        String formula,
        String displayValue,
        CellStyle style,
        String userId,
        String userName
) {
}
