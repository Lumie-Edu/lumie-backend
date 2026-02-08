package com.lumie.admin.adapter.in.web;

import com.lumie.admin.application.dto.PayrollRequest;
import com.lumie.admin.application.dto.PayrollResponse;
import com.lumie.admin.application.service.PayrollCommandService;
import com.lumie.admin.application.service.PayrollQueryService;
import com.lumie.admin.domain.vo.PayrollStatus;
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
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollCommandService payrollCommandService;
    private final PayrollQueryService payrollQueryService;

    @PostMapping("/generate")
    public ResponseEntity<PayrollResponse> generatePayroll(@Valid @RequestBody PayrollRequest request) {
        PayrollResponse response = payrollCommandService.generateMonthlyPayroll(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PayrollResponse>> getPayrolls(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) PayrollStatus status,
            @PageableDefault(size = 20, sort = "payYear", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PayrollResponse> response = payrollQueryService.getPayrolls(year, month, adminId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayrollResponse> getPayroll(@PathVariable Long id) {
        PayrollResponse response = payrollQueryService.getPayroll(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PayrollResponse> updatePayroll(
            @PathVariable Long id,
            @Valid @RequestBody PayrollRequest request) {
        PayrollResponse response = payrollCommandService.updatePayroll(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<Void> finalizePayroll(@PathVariable Long id) {
        payrollCommandService.finalizePayroll(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<Void> markPaid(@PathVariable Long id) {
        payrollCommandService.markPaid(id);
        return ResponseEntity.noContent().build();
    }
}
