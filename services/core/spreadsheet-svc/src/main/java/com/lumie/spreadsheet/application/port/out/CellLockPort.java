package com.lumie.spreadsheet.application.port.out;

import com.lumie.spreadsheet.domain.vo.CellLock;

import java.util.List;
import java.util.Optional;

public interface CellLockPort {

    Optional<CellLock> tryAcquireLock(String spreadsheetId, String cellAddress, String userId, String userName, String userColor);

    void releaseLock(String spreadsheetId, String cellAddress, String userId);

    void releaseAllLocksForUser(String userId);

    void refreshLock(String spreadsheetId, String cellAddress, String userId);

    List<CellLock> getLocksForSpreadsheet(String spreadsheetId);

    Optional<CellLock> getLock(String spreadsheetId, String cellAddress);
}
