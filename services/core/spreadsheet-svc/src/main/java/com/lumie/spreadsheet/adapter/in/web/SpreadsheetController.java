package com.lumie.spreadsheet.adapter.in.web;

import com.lumie.common.tenant.UserContextHolder;
import com.lumie.spreadsheet.application.dto.request.CreateSpreadsheetRequest;
import com.lumie.spreadsheet.application.dto.request.UpdateCellRequest;
import com.lumie.spreadsheet.application.dto.request.UpdateSpreadsheetRequest;
import com.lumie.spreadsheet.application.dto.response.CellResponse;
import com.lumie.spreadsheet.application.dto.response.SpreadsheetDetailResponse;
import com.lumie.spreadsheet.application.dto.response.SpreadsheetResponse;
import com.lumie.spreadsheet.application.service.SpreadsheetCommandService;
import com.lumie.spreadsheet.application.service.SpreadsheetQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/spreadsheets")
@RequiredArgsConstructor
public class SpreadsheetController {

    private final SpreadsheetCommandService commandService;
    private final SpreadsheetQueryService queryService;

    @GetMapping
    public ResponseEntity<Page<SpreadsheetResponse>> listSpreadsheets(
            @RequestParam(required = false) Long ownerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<SpreadsheetResponse> response = ownerId != null
                ? queryService.listSpreadsheetsByOwner(ownerId, pageable)
                : queryService.listSpreadsheets(pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SpreadsheetResponse> createSpreadsheet(
            @Valid @RequestBody CreateSpreadsheetRequest request) {
        Long userId = UserContextHolder.getRequiredUserId();
        SpreadsheetResponse response = commandService.createSpreadsheet(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpreadsheetResponse> getSpreadsheet(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getSpreadsheet(id));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<SpreadsheetDetailResponse> getSpreadsheetDetail(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getSpreadsheetDetail(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SpreadsheetResponse> updateSpreadsheet(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSpreadsheetRequest request) {
        return ResponseEntity.ok(commandService.updateSpreadsheet(id, request));
    }

    @PatchMapping("/{id}/cells/{address}")
    public ResponseEntity<CellResponse> updateCell(
            @PathVariable Long id,
            @PathVariable String address,
            @Valid @RequestBody UpdateCellRequest request) {
        UpdateCellRequest cellRequest = new UpdateCellRequest(
                address,
                request.value(),
                request.formula(),
                request.style()
        );
        return ResponseEntity.ok(commandService.updateCell(id, cellRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpreadsheet(@PathVariable Long id) {
        commandService.deleteSpreadsheet(id);
        return ResponseEntity.noContent().build();
    }
}
