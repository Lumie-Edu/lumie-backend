package com.lumie.exam.adapter.out.external;

import com.lumie.exam.application.port.out.ReportServicePort;
import com.lumie.exam.application.service.ReportFileFormat;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.exception.ExamErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class ReportServiceClient implements ReportServicePort {

    private final RestClient restClient;

    @Value("${lumie.services.report.url}")
    private String reportServiceUrl;

    public ReportServiceClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public byte[] generateReport(Long studentId, Long examId, String tenantSlug) {
        try {
            String url = String.format(
                    "%s/v1/reports/students/%d/exams/%d",
                    reportServiceUrl, studentId, examId
            );

            ResponseEntity<byte[]> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Tenant-Slug", tenantSlug)
                    .retrieve()
                    .toEntity(byte[].class);

            byte[] reportBytes = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && ReportFileFormat.isPdf(reportBytes)) {
                return reportBytes;
            }
            if (response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ExamErrorCode.REPORT_GENERATION_FAILED,
                        "Report service returned non-PDF bytes");
            }

            throw new BusinessException(ExamErrorCode.REPORT_GENERATION_FAILED, "Report service returned error");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Report generation failed for student {} exam {}", studentId, examId, e);
            throw new BusinessException(ExamErrorCode.REPORT_GENERATION_FAILED, e.getMessage());
        }
    }
}
