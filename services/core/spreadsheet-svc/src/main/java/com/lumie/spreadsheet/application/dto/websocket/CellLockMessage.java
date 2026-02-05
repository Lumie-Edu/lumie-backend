package com.lumie.spreadsheet.application.dto.websocket;

public record CellLockMessage(
        String cellAddress,
        String userId,
        String userName
) {
}
