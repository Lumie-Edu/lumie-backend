package com.lumie.admin.adapter.in.web;

import com.lumie.admin.application.dto.AdminFilterRequest;
import com.lumie.admin.application.dto.AdminPermissionResponse;
import com.lumie.admin.application.dto.AdminRequest;
import com.lumie.admin.application.dto.AdminResponse;
import com.lumie.admin.application.dto.AdminUpdateRequest;
import com.lumie.admin.application.service.AdminCommandService;
import com.lumie.admin.application.service.AdminQueryService;
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
import java.util.Map;

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
    public ResponseEntity<Page<AdminResponse>> getAllAdmins(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AdminFilterRequest filter = new AdminFilterRequest(isActive, search);
        Page<AdminResponse> response = adminQueryService.getAllAdmins(filter, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
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
            @RequestBody Map<String, String> permissions) {
        adminCommandService.setAdminPermissions(adminId, permissions);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetAdminPassword(@PathVariable Long id) {
        adminCommandService.resetAdminPassword(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/login-id")
    public ResponseEntity<Map<String, String>> changeAdminLoginId(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newLoginId = body.get("loginId");
        if (newLoginId == null || newLoginId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String result = adminCommandService.changeAdminLoginId(id, newLoginId);
        return ResponseEntity.ok(Map.of("loginId", result));
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
