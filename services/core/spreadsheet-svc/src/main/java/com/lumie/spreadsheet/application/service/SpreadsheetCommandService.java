package com.lumie.spreadsheet.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.spreadsheet.application.dto.request.CreateSpreadsheetRequest;
import com.lumie.spreadsheet.application.dto.request.UpdateCellRequest;
import com.lumie.spreadsheet.application.dto.request.UpdateSpreadsheetRequest;
import com.lumie.spreadsheet.application.dto.response.CellResponse;
import com.lumie.spreadsheet.application.dto.response.SpreadsheetResponse;
import com.lumie.spreadsheet.domain.entity.Spreadsheet;
import com.lumie.spreadsheet.domain.exception.SpreadsheetErrorCode;
import com.lumie.spreadsheet.domain.exception.SpreadsheetException;
import com.lumie.spreadsheet.domain.repository.SpreadsheetRepository;
import com.lumie.spreadsheet.domain.vo.CellData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SpreadsheetCommandService {

    private final SpreadsheetRepository spreadsheetRepository;

    public SpreadsheetResponse createSpreadsheet(CreateSpreadsheetRequest request, Long ownerId) {
        String tenantSlug = TenantContextHolder.getTenantSlug();
        log.info("Creating spreadsheet: {} for owner: {} in tenant: {}", request.name(), ownerId, tenantSlug);

        Spreadsheet spreadsheet = Spreadsheet.builder()
                .name(request.name())
                .description(request.description())
                .rowCount(request.rowCount())
                .columnCount(request.columnCount())
                .ownerId(ownerId)
                .permission(request.permission())
                .build();

        Spreadsheet saved = spreadsheetRepository.save(spreadsheet);
        log.info("Spreadsheet created with id: {}", saved.getId());
        return SpreadsheetResponse.from(saved);
    }

    public SpreadsheetResponse updateSpreadsheet(Long id, UpdateSpreadsheetRequest request) {
        String tenantSlug = TenantContextHolder.getTenantSlug();
        log.info("Updating spreadsheet: {} in tenant: {}", id, tenantSlug);

        Spreadsheet spreadsheet = spreadsheetRepository.findById(id)
                .orElseThrow(() -> new SpreadsheetException(SpreadsheetErrorCode.SPREADSHEET_NOT_FOUND));

        spreadsheet.update(request.name(), request.description(), request.permission());
        Spreadsheet updated = spreadsheetRepository.save(spreadsheet);

        log.info("Spreadsheet updated: {}", id);
        return SpreadsheetResponse.from(updated);
    }

    public CellResponse updateCell(Long spreadsheetId, UpdateCellRequest request) {
        String tenantSlug = TenantContextHolder.getTenantSlug();
        log.debug("Updating cell {} in spreadsheet: {} in tenant: {}", request.address(), spreadsheetId, tenantSlug);

        Spreadsheet spreadsheet = spreadsheetRepository.findById(spreadsheetId)
                .orElseThrow(() -> new SpreadsheetException(SpreadsheetErrorCode.SPREADSHEET_NOT_FOUND));

        CellData cellData = new CellData(
                request.value(),
                calculateDisplayValue(request.value(), request.formula()),
                request.formula(),
                request.style()
        );

        spreadsheet.updateCell(request.address(), cellData);
        spreadsheetRepository.save(spreadsheet);

        log.debug("Cell updated: {} in spreadsheet: {}", request.address(), spreadsheetId);
        return CellResponse.from(request.address(), cellData);
    }

    public void deleteSpreadsheet(Long id) {
        String tenantSlug = TenantContextHolder.getTenantSlug();
        log.info("Deleting spreadsheet: {} in tenant: {}", id, tenantSlug);

        if (!spreadsheetRepository.existsById(id)) {
            throw new SpreadsheetException(SpreadsheetErrorCode.SPREADSHEET_NOT_FOUND);
        }

        spreadsheetRepository.deleteById(id);
        log.info("Spreadsheet deleted: {}", id);
    }

    private String calculateDisplayValue(String value, String formula) {
        if (formula != null && !formula.isBlank()) {
            return value;
        }
        return value;
    }
}
