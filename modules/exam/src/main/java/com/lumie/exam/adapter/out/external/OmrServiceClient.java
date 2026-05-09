package com.lumie.exam.adapter.out.external;

import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OmrServiceClient implements OmrServicePort {

    private final RestTemplate restTemplate;

    @Value("${lumie.services.grading.url}")
    private String gradingServiceUrl;

    @Override
    public OmrGradingResult gradeOmrImage(byte[] imageData,
                                           Map<String, String> correctAnswers,
                                           Map<String, Integer> questionScores,
                                           Map<String, String> questionTypes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

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

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<OmrResponse> response = restTemplate.postForEntity(
                    gradingServiceUrl + "/api/omr/grade",
                    request,
                    OmrResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return mapToResult(response.getBody());
            }

            throw new ExamException(ExamErrorCode.OMR_GRADING_FAILED, "OMR service returned error");

        } catch (ExamException e) {
            throw e;
        } catch (Exception e) {
            log.error("OMR grading failed", e);
            throw new ExamException(ExamErrorCode.OMR_GRADING_FAILED, e.getMessage());
        }
    }

    private String toJson(Map<?, ?> map) {
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                if (entry.getValue() instanceof String) {
                    sb.append("\"").append(entry.getValue()).append("\"");
                } else {
                    sb.append(entry.getValue());
                }
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert map to JSON", e);
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
