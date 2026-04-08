package com.lumie.admin.adapter.in.web;

import com.lumie.admin.application.dto.AdminFilterRequest;
import com.lumie.admin.application.dto.AdminPermissionResponse;
import com.lumie.admin.application.dto.AdminRequest;
import com.lumie.admin.application.dto.AdminResponse;
import com.lumie.admin.application.service.AdminCommandService;
import com.lumie.admin.application.service.AdminQueryService;
import com.lumie.common.tenant.UserContextHolder;
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
        Long userId = UserContextHolder.getRequiredUserId();
        AdminResponse response = adminCommandService.registerAdmin(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminResponse> getAdmin(@PathVariable Long id) {
        AdminResponse response = adminQueryService.getAdmin(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<AdminResponse>> getAllAdmins(
            @RequestParam(required = false) Long academyId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AdminFilterRequest filter = new AdminFilterRequest(academyId, isActive, search);
        Page<AdminResponse> response = adminQueryService.getAllAdmins(filter, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminResponse> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequest request) {
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
