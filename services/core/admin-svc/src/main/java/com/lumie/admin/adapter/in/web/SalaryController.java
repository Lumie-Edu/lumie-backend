package com.lumie.admin.adapter.in.web;

import com.lumie.admin.application.dto.SalaryRequest;
import com.lumie.admin.application.dto.SalaryResponse;
import com.lumie.admin.application.service.SalaryCommandService;
import com.lumie.admin.application.service.SalaryQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admins/{adminId}/salaries")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryCommandService salaryCommandService;
    private final SalaryQueryService salaryQueryService;

    @PostMapping
    public ResponseEntity<SalaryResponse> setSalary(
            @PathVariable Long adminId,
            @Valid @RequestBody SalaryRequest request) {
        SalaryResponse response = salaryCommandService.setSalary(adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SalaryResponse>> getSalaryHistory(@PathVariable Long adminId) {
        List<SalaryResponse> response = salaryQueryService.getSalaryHistory(adminId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current")
    public ResponseEntity<SalaryResponse> getCurrentSalary(@PathVariable Long adminId) {
        SalaryResponse response = salaryQueryService.getCurrentSalary(adminId);
        return ResponseEntity.ok(response);
    }
}
