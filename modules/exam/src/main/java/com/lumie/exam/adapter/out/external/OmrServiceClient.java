package com.lumie.exam.adapter.out.external;

import com.lumie.exam.application.port.out.OmrServicePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.exception.ExamErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OmrServiceClient implements OmrServicePort {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration OMR_READ_TIMEOUT = Duration.ofSeconds(90);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${lumie.services.grading.url}")
    private String gradingServiceUrl;

    public OmrServiceClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // OMR grading is long-running (image processing); override default 30s read timeout to 90s.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(OMR_READ_TIMEOUT);
        this.restClient = restClientBuilder.requestFactory(factory).build();
    }

    @Override
    public OmrGradingResult gradeOmrImage(byte[] imageData,
                                           String tenantSlug,
                                           Map<String, String> correctAnswers,
                                           Map<String, Integer> questionScores,
                                           Map<String, String> questionTypes) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            ByteArrayResource imageResource = new ByteArrayResource(imageData) {
                @Override
                public String getFilename() {
                    return "omr.jpg";
                }
            };
            body.add("image", imageResource);
            body.add("correct_answers", toJson(correctAnswers));
            body.add("question_scores", toJson(questionScores));
            body.add("question_types", toJson(questionTypes));

            ResponseEntity<OmrResponse> response = restClient.post()
                    .uri(gradingServiceUrl + "/api/temp/grading")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("X-Tenant-Slug", tenantSlug)
                    .body(body)
                    .retrieve()
                    .toEntity(OmrResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return mapToResult(response.getBody());
            }

            throw new BusinessException(ExamErrorCode.OMR_GRADING_FAILED, "OMR service returned error");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("OMR grading failed", e);
            throw new BusinessException(ExamErrorCode.OMR_GRADING_FAILED, e);
        }
    }

    private String toJson(Map<?, ?> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert map to JSON for OMR grading", e);
            throw new BusinessException(ExamErrorCode.OMR_GRADING_FAILED, e);
        }
    }

    private OmrGradingResult mapToResult(OmrResponse response) {
        List<OmrQuestionResult> results = response.results().stream()
                .map(r -> new OmrQuestionResult(
                        r.questionNumber(),
                        r.studentAnswer(),
                        r.correctAnswer(),
                        r.score(),
                        r.earnedScore(),
                        r.questionType()
                ))
                .toList();

        return new OmrGradingResult(
                response.totalScore(),
                response.grade(),
                response.phoneNumber(),
                results
        );
    }

    private record OmrResponse(
            int totalScore,
            int grade,
            String phoneNumber,
            List<QuestionResultItem> results,
            Object imageInfo
    ) {
    }

    private record QuestionResultItem(
            int questionNumber,
            String studentAnswer,
            String correctAnswer,
            int score,
            int earnedScore,
            String questionType
    ) {
    }
}
