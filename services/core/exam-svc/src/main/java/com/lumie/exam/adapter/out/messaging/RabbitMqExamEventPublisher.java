package com.lumie.exam.adapter.out.messaging;

import com.lumie.exam.application.port.out.ExamEventPublisherPort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqExamEventPublisher implements ExamEventPublisherPort {

    private static final String EXCHANGE = "lumie.events";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishExamCreated(Exam exam, String tenantSlug) {
        String routingKey = "exam.created." + tenantSlug;

        Map<String, Object> event = Map.of(
                "eventType", "EXAM_CREATED",
                "tenantSlug", tenantSlug,
                "examId", exam.getId(),
                "examName", exam.getName(),
                "category", exam.getCategory().name(),
                "totalQuestions", exam.getTotalQuestions(),
                "timestamp", LocalDateTime.now().toString()
        );

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published exam.created event for exam: {}", exam.getId());
        } catch (Exception e) {
            log.error("Failed to publish exam.created event", e);
        }
    }

    @Override
    public void publishResultSubmitted(ExamResult result, String tenantSlug) {
        String routingKey = "result.submitted." + tenantSlug;

        Map<String, Object> event = Map.of(
                "eventType", "RESULT_SUBMITTED",
                "tenantSlug", tenantSlug,
                "resultId", result.getId(),
                "examId", result.getExam().getId(),
                "examName", result.getExam().getName(),
                "studentId", result.getStudentId(),
                "totalScore", result.getTotalScore(),
                "grade", result.getGrade() != null ? result.getGrade() : 0,
                "timestamp", LocalDateTime.now().toString()
        );

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published result.submitted event for result: {}", result.getId());
        } catch (Exception e) {
            log.error("Failed to publish result.submitted event", e);
        }
    }
}
