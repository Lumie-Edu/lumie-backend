package com.lumie.spreadsheet.domain.vo;

import java.time.Instant;

public record CellLock(
        String spreadsheetId,
        String cellAddress,
        String userId,
        String userName,
        String userColor,
        Instant acquiredAt,
        Instant expiresAt
) {
    public static CellLock create(String spreadsheetId, String cellAddress,
                                   String userId, String userName, String userColor) {
        Instant now = Instant.now();
        return new CellLock(
                spreadsheetId,
                cellAddress,
                userId,
                userName,
                userColor,
                now,
                now.plusSeconds(30)
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public CellLock refresh() {
        return new CellLock(
                spreadsheetId,
                cellAddress,
                userId,
                userName,
                userColor,
                acquiredAt,
                Instant.now().plusSeconds(30)
        );
    }
}
