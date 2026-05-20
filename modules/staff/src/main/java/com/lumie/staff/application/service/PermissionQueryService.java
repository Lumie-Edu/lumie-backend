package com.lumie.staff.application.service;

import com.lumie.staff.application.dto.response.PermissionResponse;
import com.lumie.staff.domain.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionQueryService {

    private final PermissionRepository permissionRepository;

    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(PermissionResponse::from)
                .toList();
    }

    public Map<String, List<PermissionResponse>> getPermissionsByCategory() {
        return permissionRepository.findAll().stream()
                .map(PermissionResponse::from)
                .collect(Collectors.groupingBy(PermissionResponse::category));
    }
}
