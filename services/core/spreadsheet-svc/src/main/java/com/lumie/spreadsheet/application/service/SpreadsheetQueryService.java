package com.lumie.spreadsheet.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.spreadsheet.application.dto.response.SpreadsheetDetailResponse;
import com.lumie.spreadsheet.application.dto.response.SpreadsheetResponse;
import com.lumie.spreadsheet.domain.entity.Spreadsheet;
import com.lumie.spreadsheet.domain.exception.SpreadsheetErrorCode;
import com.lumie.spreadsheet.domain.exception.SpreadsheetException;
import com.lumie.spreadsheet.domain.repository.SpreadsheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SpreadsheetQueryService {

    private final SpreadsheetRepository spreadsheetRepository;

    public Page<SpreadsheetResponse> listSpreadsheets(Pageable pageable) {
        String tenantSlug = TenantContextHolder.getTenantSlug();
        log.debug("Listing spreadsheets for tenant: {}", tenantSlug);
        return spreadsheetRepository.findAll(pageable)
                .map(SpreadsheetResponse::from);
    }

    public Page<SpreadsheetResponse> listSpreadsheetsByOwner(Long ownerId, Pageable pageable) {
        String tenantSlug = TenantContextHolder.getTenantSlug();
        log.debug("Listing spreadsheets for owner: {} in tenant: {}", ownerId, tenantSlug);
        return spreadsheetRepository.findByOwnerId(ownerId, pageable)
                .map(SpreadsheetResponse::from);
    }

    public SpreadsheetResponse getSpreadsheet(Long id) {
        String tenantSlug = TenantContextHolder.getTenantSlug();
        log.debug("Getting spreadsheet: {} in tenant: {}", id, tenantSlug);
        Spreadsheet spreadsheet = spreadsheetRepository.findById(id)
                .orElseThrow(() -> new SpreadsheetException(SpreadsheetErrorCode.SPREADSHEET_NOT_FOUND));
        return SpreadsheetResponse.from(spreadsheet);
    }

    public SpreadsheetDetailResponse getSpreadsheetDetail(Long id) {
        String tenantSlug = TenantContextHolder.getTenantSlug();
        log.debug("Getting spreadsheet detail: {} in tenant: {}", id, tenantSlug);
        Spreadsheet spreadsheet = spreadsheetRepository.findById(id)
                .orElseThrow(() -> new SpreadsheetException(SpreadsheetErrorCode.SPREADSHEET_NOT_FOUND));
        return SpreadsheetDetailResponse.from(spreadsheet);
    }
}
