package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.port.out.ReportServicePort;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportServicePort reportServicePort;

    /**
     * Generate exam report for a student
     *
     * @param studentId Student ID
     * @param examId Exam ID
     * @return JPG image of the report
     */
    @PostMapping("/students/{studentId}/exams/{examId}")
    public ResponseEntity<byte[]> generateReport(
            @PathVariable Long studentId,
            @PathVariable Long examId
    ) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        byte[] jpgBytes = reportServicePort.generateReport(studentId, examId, tenantSlug);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentDispositionFormData(
                "inline",
                String.format("report_%d_%d.jpg", studentId, examId)
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(jpgBytes);
    }
}
