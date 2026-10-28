package com.lumie.exam.adapter.out.external;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.exam.application.dto.request.ReportBatchRequest;
import com.lumie.exam.application.dto.request.UpdateAiCommentaryRequest;
import com.lumie.exam.application.dto.response.AiCommentaryResponse;
import com.lumie.exam.domain.exception.ExamErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

@Slf4j
@Component
public class JoossamengAiReportClient {

    // Keep the long timeout scoped to this temporary joossameng-only batch path.
    private static final Duration AI_REPORT_READ_TIMEOUT = Duration.ofMinutes(15);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final RestClient aiReportRestClient;
    private final String analysisServiceUrl;

    public JoossamengAiReportClient(
            RestClient.Builder restClientBuilder,
            @Value("${lumie.services.analysis.url}") String analysisServiceUrl
    ) {
        this.restClient = restClientBuilder.build();
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(AI_REPORT_READ_TIMEOUT);
        this.aiReportRestClient = restClientBuilder.clone()
                .requestFactory(requestFactory)
                .build();
        this.analysisServiceUrl = analysisServiceUrl;
    }

    public AiCommentaryResponse getAiCommentary(Long examId, String tenantSlug) {
        try {
            return Objects.requireNonNull(restClient.get()
                    .uri("%s/api/joossameng/ai-commentary/exams/%d".formatted(analysisServiceUrl, examId))
                    .header("X-Tenant-Slug", tenantSlug)
                    .retrieve()
                    .body(AiCommentaryResponse.class));
        } catch (Exception e) {
            log.error("Joossameng AI commentary fetch failed: examId={}", examId, e);
            throw new BusinessException(ExamErrorCode.REPORT_GENERATION_FAILED, e.getMessage());
        }
    }

    public AiCommentaryResponse generateAiCommentary(Long examId, String tenantSlug) {
        try {
            return Objects.requireNonNull(restClient.post()
                    .uri("%s/api/joossameng/ai-commentary/exams/%d/generate".formatted(analysisServiceUrl, examId))
                    .header("X-Tenant-Slug", tenantSlug)
                    .retrieve()
                    .body(AiCommentaryResponse.class));
        } catch (Exception e) {
            log.error("Joossameng AI commentary generation failed: examId={}", examId, e);
            throw new BusinessException(ExamErrorCode.REPORT_GENERATION_FAILED, e.getMessage());
        }
    }

    public AiCommentaryResponse updateAiCommentary(
            Long examId,
            UpdateAiCommentaryRequest request,
            String tenantSlug
    ) {
        try {
            return Objects.requireNonNull(restClient.put()
                    .uri("%s/api/joossameng/ai-commentary/exams/%d".formatted(analysisServiceUrl, examId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Tenant-Slug", tenantSlug)
                    .body(request)
                    .retrieve()
                    .body(AiCommentaryResponse.class));
        } catch (HttpClientErrorException.Conflict e) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("AI commentary", examId);
        } catch (Exception e) {
            log.error("Joossameng AI commentary update failed: examId={}", examId, e);
            throw new BusinessException(ExamErrorCode.REPORT_GENERATION_FAILED, e.getMessage());
        }
    }

    public byte[] generateBatch(Long examId, ReportBatchRequest request, String tenantSlug) {
        try {
            ResponseEntity<byte[]> response = aiReportRestClient.post()
                    .uri("%s/api/joossameng/ai-reports/exams/%d/batch".formatted(analysisServiceUrl, examId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Tenant-Slug", tenantSlug)
                    .body(request)
                    .retrieve()
                    .toEntity(byte[].class);
            return Objects.requireNonNull(response.getBody());
        } catch (Exception e) {
            log.error("Joossameng AI report generation failed: examId={}", examId, e);
            throw new BusinessException(ExamErrorCode.REPORT_GENERATION_FAILED, e.getMessage());
        }
    }
}
