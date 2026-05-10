package com.lumie.student.adapter.in.web;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.application.dto.*;
import com.lumie.student.application.service.StudentCommandService;
import com.lumie.student.application.service.StudentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private static final String BULK_IMPORT_SCOPE = "POST /v1/students/bulk";

    private final StudentCommandService studentCommandService;
    private final StudentQueryService studentQueryService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<StudentResponse> registerStudent(@Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentCommandService.registerStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudent(@PathVariable Long id) {
        StudentResponse response = studentQueryService.getStudent(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics/enrollment-trend")
    public ResponseEntity<EnrollmentTrendResponse> getEnrollmentTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(studentQueryService.getEnrollmentTrend(months));
    }

    @GetMapping
    public ResponseEntity<Page<StudentResponse>> getStudents(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<StudentResponse> response = studentQueryService.searchStudents(isActive, search, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request) {
        StudentResponse response = studentCommandService.updateStudent(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateStudent(@PathVariable Long id) {
        studentCommandService.deactivateStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateStudent(@PathVariable Long id) {
        studentCommandService.reactivateStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetStudentPassword(@PathVariable Long id) {
        studentCommandService.resetStudentPassword(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/login-id")
    public ResponseEntity<Map<String, String>> changeStudentLoginId(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newLoginId = body.get("loginId");
        if (newLoginId == null || newLoginId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String result = studentCommandService.changeStudentLoginId(id, newLoginId);
        return ResponseEntity.ok(Map.of("loginId", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentCommandService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/deactivate")
    public ResponseEntity<BatchOperationResult> batchDeactivate(@RequestBody BatchOperationRequest request) {
        BatchOperationResult result = studentCommandService.batchDeactivate(request.ids());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch/reactivate")
    public ResponseEntity<BatchOperationResult> batchReactivate(@RequestBody BatchOperationRequest request) {
        BatchOperationResult result = studentCommandService.batchReactivate(request.ids());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch/delete")
    public ResponseEntity<BatchOperationResult> batchDelete(@RequestBody BatchOperationRequest request) {
        BatchOperationResult result = studentCommandService.batchDelete(request.ids());
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResult> bulkImportStudents(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            // Multipart upload — fingerprint by filename + size; the IdempotencyService
            // SHA-256s this descriptor as its canonical body.
            BulkUploadFingerprint fingerprint = new BulkUploadFingerprint(
                    file.getOriginalFilename() == null ? "" : file.getOriginalFilename(),
                    file.getSize()
            );
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    BULK_IMPORT_SCOPE,
                    fingerprint,
                    () -> ResponseEntity.ok(studentCommandService.bulkImportStudents(file))
            );
        }
        BulkImportResult result = studentCommandService.bulkImportStudents(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportStudentsCsv() {
        AuthorizationGuard.requireOwner();
        String filename = "%s_students_%s.csv".formatted(
            TenantContextHolder.getRequiredTenant(),
            LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        );

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        studentQueryService.writeCsv(buffer);
        byte[] body = buffer.toByteArray();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .contentLength(body.length)
            .body(body);
    }

    private record BulkUploadFingerprint(String filename, long size) {}
}
