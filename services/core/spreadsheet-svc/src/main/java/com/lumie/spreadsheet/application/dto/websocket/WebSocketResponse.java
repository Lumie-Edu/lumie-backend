package com.lumie.spreadsheet.application.dto.websocket;

public record WebSocketResponse(
        String type,
        Object payload
) {
    public static WebSocketResponse cellLockAcquired(Object lock) {
        return new WebSocketResponse("CELL_LOCK_ACQUIRED", lock);
    }

    public static WebSocketResponse cellLockDenied(String cellAddress, String lockedByUser) {
        return new WebSocketResponse("CELL_LOCK_DENIED", new CellLockDeniedPayload(cellAddress, lockedByUser));
    }

    public static WebSocketResponse cellUpdated(Object cellData) {
        return new WebSocketResponse("CELL_UPDATED", cellData);
    }

    public static WebSocketResponse cellUnlocked(String cellAddress, String userId) {
        return new WebSocketResponse("CELL_UNLOCKED", new CellUnlockedPayload(cellAddress, userId));
    }

    public static WebSocketResponse userJoined(String userId, String userName) {
        return new WebSocketResponse("USER_JOINED", new UserPayload(userId, userName));
    }

    public static WebSocketResponse userLeft(String userId, String userName) {
        return new WebSocketResponse("USER_LEFT", new UserPayload(userId, userName));
    }

    public static WebSocketResponse error(String message) {
        return new WebSocketResponse("ERROR", new ErrorPayload(message));
    }

    public record CellLockDeniedPayload(String cellAddress, String lockedByUser) {}
    public record CellUnlockedPayload(String cellAddress, String userId) {}
    public record UserPayload(String userId, String userName) {}
    public record ErrorPayload(String message) {}
}
