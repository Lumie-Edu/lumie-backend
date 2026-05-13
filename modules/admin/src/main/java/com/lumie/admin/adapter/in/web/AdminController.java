package com.lumie.admin.adapter.in.web;

import com.lumie.admin.application.dto.AdminFilterRequest;
import com.lumie.admin.application.dto.AdminPermissionResponse;
import com.lumie.admin.application.dto.AdminRequest;
import com.lumie.admin.application.dto.AdminResponse;
import com.lumie.admin.application.dto.AdminUpdateRequest;
import com.lumie.admin.application.dto.ChangeLoginIdRequest;
import com.lumie.admin.application.dto.ChangeLoginIdResponse;
import com.lumie.admin.application.dto.SetPermissionsRequest;
import com.lumie.admin.application.service.AdminCommandService;
import com.lumie.admin.application.service.AdminQueryService;
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
@RequestMapping("/v1/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminCommandService adminCommandService;
    private final AdminQueryService adminQueryService;

    @PostMapping
    public ResponseEntity<AdminResponse> registerAdmin(@Valid @RequestBody AdminRequest request) {
        AdminResponse response = adminCommandService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminResponse> getAdmin(@PathVariable Long id) {
        AdminResponse response = adminQueryService.getAdmin(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<AdminResponse>> getAllAdmins(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AdminFilterRequest filter = new AdminFilterRequest(isActive, search);
        Page<AdminResponse> response = adminQueryService.getAllAdmins(filter, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AdminResponse> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRequest request) {
        AdminResponse response = adminCommandService.updateAdmin(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{adminId}/permissions")
    public ResponseEntity<List<AdminPermissionResponse>> getAdminPermissions(@PathVariable Long adminId) {
        List<AdminPermissionResponse> response = adminQueryService.getAdminPermissions(adminId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{adminId}/permissions")
    public ResponseEntity<Void> setAdminPermissions(
            @PathVariable Long adminId,
            @Valid @RequestBody SetPermissionsRequest request) {
        adminCommandService.setAdminPermissions(adminId, request.permissions());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetAdminPassword(@PathVariable Long id) {
        adminCommandService.resetAdminPassword(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/login-id")
    public ResponseEntity<ChangeLoginIdResponse> changeAdminLoginId(
            @PathVariable Long id,
            @Valid @RequestBody ChangeLoginIdRequest request) {
        String result = adminCommandService.changeAdminLoginId(id, request.loginId());
        return ResponseEntity.ok(new ChangeLoginIdResponse(result));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateAdmin(@PathVariable Long id) {
        adminCommandService.deactivateAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateAdmin(@PathVariable Long id) {
        adminCommandService.reactivateAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<Void> terminateAdmin(@PathVariable Long id) {
        adminCommandService.terminateAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        adminCommandService.deleteAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
