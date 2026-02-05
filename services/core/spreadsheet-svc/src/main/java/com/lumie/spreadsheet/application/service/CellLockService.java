package com.lumie.spreadsheet.application.service;

import com.lumie.spreadsheet.application.port.out.CellLockPort;
import com.lumie.spreadsheet.domain.exception.SpreadsheetErrorCode;
import com.lumie.spreadsheet.domain.exception.SpreadsheetException;
import com.lumie.spreadsheet.domain.vo.CellLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CellLockService {

    private static final String[] USER_COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
            "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F",
            "#BB8FCE", "#85C1E9"
    };

    private final CellLockPort cellLockPort;

    public CellLock acquireLock(String spreadsheetId, String cellAddress, String userId, String userName) {
        String userColor = getUserColor(userId);

        Optional<CellLock> lock = cellLockPort.tryAcquireLock(
                spreadsheetId, cellAddress, userId, userName, userColor);

        if (lock.isEmpty()) {
            Optional<CellLock> existingLock = cellLockPort.getLock(spreadsheetId, cellAddress);
            if (existingLock.isPresent() && !existingLock.get().userId().equals(userId)) {
                log.warn("Cell lock denied: spreadsheet={}, cell={}, user={}, lockedBy={}",
                        spreadsheetId, cellAddress, userId, existingLock.get().userId());
                throw new SpreadsheetException(SpreadsheetErrorCode.CELL_LOCKED,
                        "Cell is locked by " + existingLock.get().userName());
            }
        }

        return lock.orElseThrow(() -> new SpreadsheetException(SpreadsheetErrorCode.CELL_LOCK_CONFLICT));
    }

    public void releaseLock(String spreadsheetId, String cellAddress, String userId) {
        cellLockPort.releaseLock(spreadsheetId, cellAddress, userId);
    }

    public void releaseAllLocksForUser(String userId) {
        cellLockPort.releaseAllLocksForUser(userId);
    }

    public void refreshLock(String spreadsheetId, String cellAddress, String userId) {
        cellLockPort.refreshLock(spreadsheetId, cellAddress, userId);
    }

    public List<CellLock> getLocksForSpreadsheet(String spreadsheetId) {
        return cellLockPort.getLocksForSpreadsheet(spreadsheetId);
    }

    public Optional<CellLock> getLock(String spreadsheetId, String cellAddress) {
        return cellLockPort.getLock(spreadsheetId, cellAddress);
    }

    private String getUserColor(String userId) {
        int hash = Math.abs(userId.hashCode());
        return USER_COLORS[hash % USER_COLORS.length];
    }
}
