package com.lumie.staff.adapter.in.web;

import com.lumie.staff.application.dto.request.StaffFilterRequest;
import com.lumie.staff.application.dto.request.StaffRequest;
import com.lumie.staff.application.dto.request.StaffUpdateRequest;
import com.lumie.staff.application.dto.request.ChangeLoginIdRequest;
import com.lumie.staff.application.dto.request.SetPermissionsRequest;
import com.lumie.staff.application.dto.response.StaffPermissionResponse;
import com.lumie.staff.application.dto.response.StaffResponse;
import com.lumie.staff.application.dto.response.ChangeLoginIdResponse;
import com.lumie.staff.application.service.StaffCommandService;
import com.lumie.staff.application.service.StaffQueryService;
import com.lumie.common.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffCommandService staffCommandService;
    private final StaffQueryService staffQueryService;

    @PostMapping
    public ResponseEntity<StaffResponse> registerStaff(@Valid @RequestBody StaffRequest request) {
        StaffResponse response = staffCommandService.registerStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffResponse> getStaff(@PathVariable Long id) {
        StaffResponse response = staffQueryService.getStaff(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<StaffResponse>> getAllStaff(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        StaffFilterRequest filter = new StaffFilterRequest(isActive, search);
        Page<StaffResponse> response = staffQueryService.getAllStaff(filter, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StaffResponse> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody StaffUpdateRequest request) {
        StaffResponse response = staffCommandService.updateStaff(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{staffId}/permissions")
    public ResponseEntity<List<StaffPermissionResponse>> getStaffPermissions(@PathVariable Long staffId) {
        List<StaffPermissionResponse> response = staffQueryService.getStaffPermissions(staffId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{staffId}/permissions")
    public ResponseEntity<Void> setStaffPermissions(
            @PathVariable Long staffId,
            @Valid @RequestBody SetPermissionsRequest request) {
        staffCommandService.setStaffPermissions(staffId, request.permissions());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetStaffPassword(@PathVariable Long id) {
        staffCommandService.resetStaffPassword(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/login-id")
    public ResponseEntity<ChangeLoginIdResponse> changeStaffLoginId(
            @PathVariable Long id,
            @Valid @RequestBody ChangeLoginIdRequest request) {
        String result = staffCommandService.changeStaffLoginId(id, request.loginId());
        return ResponseEntity.ok(new ChangeLoginIdResponse(result));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateStaff(@PathVariable Long id) {
        staffCommandService.deactivateStaff(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateStaff(@PathVariable Long id) {
        staffCommandService.reactivateStaff(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<Void> terminateStaff(@PathVariable Long id) {
        staffCommandService.terminateStaff(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        staffCommandService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }
}
