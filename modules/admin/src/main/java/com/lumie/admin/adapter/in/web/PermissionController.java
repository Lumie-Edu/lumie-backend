package com.lumie.admin.adapter.in.web;

import com.lumie.admin.application.dto.PermissionResponse;
import com.lumie.admin.application.service.PermissionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionQueryService permissionQueryService;

    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        return ResponseEntity.ok(permissionQueryService.getAllPermissions());
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, List<PermissionResponse>>> getPermissionsByCategory() {
        return ResponseEntity.ok(permissionQueryService.getPermissionsByCategory());
    }
}
