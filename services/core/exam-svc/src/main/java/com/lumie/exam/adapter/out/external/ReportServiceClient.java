package com.lumie.exam.adapter.out.external;

import com.lumie.exam.application.port.out.ReportServicePort;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportServiceClient implements ReportServicePort {

    private final RestTemplate restTemplate;

    @Value("${lumie.services.report.url}")
    private String reportServiceUrl;

    @Override
    public byte[] generateReport(Long studentId, Long examId, String tenantSlug) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Slug", tenantSlug);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = String.format(
                    "%s/api/v1/reports/students/%d/exams/%d",
                    reportServiceUrl, studentId, examId
            );

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            throw new ExamException(ExamErrorCode.REPORT_GENERATION_FAILED, "Report service returned error");

        } catch (ExamException e) {
            throw e;
        } catch (Exception e) {
            log.error("Report generation failed for student {} exam {}", studentId, examId, e);
            throw new ExamException(ExamErrorCode.REPORT_GENERATION_FAILED, e.getMessage());
        }
    }
}
