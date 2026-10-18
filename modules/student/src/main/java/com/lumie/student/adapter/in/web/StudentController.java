package com.lumie.student.adapter.in.web;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.lumie.common.util.ReportingTime;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.auth.Role;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.application.dto.request.BatchOperationRequest;
import com.lumie.student.application.dto.request.ChangeLoginIdRequest;
import com.lumie.student.application.dto.request.StudentRequest;
import com.lumie.student.application.dto.request.StudentUpdateRequest;
import com.lumie.student.application.dto.response.BatchOperationResult;
import com.lumie.student.application.dto.response.BulkImportResult;
import com.lumie.student.application.dto.response.ChangeLoginIdResponse;
import com.lumie.student.application.dto.response.DropoutSummaryResponse;
import com.lumie.student.application.dto.response.EnrollmentTrendResponse;
import com.lumie.student.application.dto.response.StudentResponse;
import com.lumie.student.application.service.StudentCommandService;
import com.lumie.student.application.service.StudentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private static final String BULK_IMPORT_SCOPE = "POST /v1/students/bulk";

    private final StudentCommandService studentCommandService;
    private final StudentQueryService studentQueryService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Created")
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

    @GetMapping("/statistics/dropout-summary")
    public ResponseEntity<DropoutSummaryResponse> getDropoutSummary() {
        return ResponseEntity.ok(studentQueryService.getDropoutSummary());
    }

    @GetMapping
    @AllowedSortFields({"createdAt", "name", "phone", "studentHighschool", "studentBirthYear"})
    public ResponseEntity<PageResponse<StudentResponse>> getStudents(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Boolean hasActiveEnrollment,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<StudentResponse> response = studentQueryService.searchStudents(
                isActive,
                search,
                classId,
                hasActiveEnrollment,
                pageable
        );
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request) {
        StudentResponse response = studentCommandService.updateStudent(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deactivate")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deactivateStudent(@PathVariable Long id) {
        studentCommandService.deactivateStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> reactivateStudent(@PathVariable Long id) {
        studentCommandService.reactivateStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> resetStudentPassword(@PathVariable Long id) {
        studentCommandService.resetStudentPassword(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/login-id")
    public ResponseEntity<ChangeLoginIdResponse> changeStudentLoginId(
            @PathVariable Long id,
            @Valid @RequestBody ChangeLoginIdRequest request) {
        String result = studentCommandService.changeStudentLoginId(id, request.loginId());
        return ResponseEntity.ok(new ChangeLoginIdResponse(result));
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentCommandService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch/deactivate")
    @ApiResponse(responseCode = "207", description = "Multi-Status")
    public ResponseEntity<BatchOperationResult> batchDeactivate(@Valid @RequestBody BatchOperationRequest request) {
        BatchOperationResult result = studentCommandService.batchDeactivate(request.ids());
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }

    @PostMapping("/batch/reactivate")
    @ApiResponse(responseCode = "207", description = "Multi-Status")
    public ResponseEntity<BatchOperationResult> batchReactivate(@Valid @RequestBody BatchOperationRequest request) {
        BatchOperationResult result = studentCommandService.batchReactivate(request.ids());
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }

    @PostMapping("/batch/delete")
    @ApiResponse(responseCode = "207", description = "Multi-Status")
    public ResponseEntity<BatchOperationResult> batchDelete(@Valid @RequestBody BatchOperationRequest request) {
        BatchOperationResult result = studentCommandService.batchDelete(request.ids());
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }

    /**
     * Bulk-imports students from an Excel/CSV upload.
     *
     * <p><b>Synchronous by design.</b> Unlike the OMR confirm
     * ({@code POST /v1/exams/{examId}/results/omr/batch/confirm}) and report batch
     * ({@code POST /v1/exams/{examId}/reports/batch}) endpoints — which return
     * {@code 202 Accepted} + {@code JobAcceptedResponse} and are polled via a
     * separate status URL — bulk-import runs inline and returns
     * {@code 200 OK} with the full {@link BulkImportResult}.
     *
     * <p>Rationale: typical uploads (≤ a few hundred rows) parse + INSERT in
     * under 5 s. Wrapping in an async job table for marginal latency benefit
     * adds infra (job persistence, polling endpoint, FE state) without payoff.
     * Revisit only if real-world payloads grow past the request-timeout budget.
     */
    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResult> bulkImportStudents(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        // Multipart upload — fingerprint by size + content hash; local filenames do
        // not affect the imported student set and should not break same-file replay.
        BulkUploadFingerprint fingerprint = new BulkUploadFingerprint(
                file.getSize(),
                studentCommandService.hashBulkImportFile(file)
        );
        return idempotencyService.executeOnce(
                idempotencyKey,
                BULK_IMPORT_SCOPE,
                fingerprint,
                () -> ResponseEntity.ok(studentCommandService.bulkImportStudents(file))
        );
    }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    @ApiResponse(responseCode = "200", description = "CSV export", content =
            @Content(mediaType = "text/csv;charset=UTF-8", schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<StreamingResponseBody> exportStudentsCsv() {
        AuthorizationGuard.requireOwner();
        String filename = "%s_students_%s.csv".formatted(
            TenantContextHolder.getRequiredTenant(),
            LocalDate.now(ReportingTime.ZONE).format(DateTimeFormatter.BASIC_ISO_DATE)
        );

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .body(studentQueryService::writeCsv);
    }

    private record BulkUploadFingerprint(long size, String sha256) {}
}
