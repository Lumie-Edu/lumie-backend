package com.lumie.content.adapter.out.messaging;

import com.lumie.content.application.port.out.ContentEventPublisherPort;
import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.entity.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqContentEventPublisher implements ContentEventPublisherPort {

    private static final String EXCHANGE = "lumie.events";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishQnaReplied(QnaBoard qnaBoard, String tenantSlug) {
        String routingKey = "qna.replied." + tenantSlug;

        Map<String, Object> event = Map.of(
                "eventType", "QNA_REPLIED",
                "tenantSlug", tenantSlug,
                "qnaId", qnaBoard.getId(),
                "qnaTitle", qnaBoard.getTitle(),
                "studentId", qnaBoard.getStudentId(),
                "timestamp", LocalDateTime.now().toString()
        );

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published qna.replied event for Q&A: {}", qnaBoard.getId());
        } catch (Exception e) {
            log.error("Failed to publish qna.replied event", e);
        }
    }

    @Override
    public void publishReservationConfirmed(Reservation reservation, String tenantSlug) {
        String routingKey = "reservation.confirmed." + tenantSlug;

        Map<String, Object> event = Map.of(
                "eventType", "RESERVATION_CONFIRMED",
                "tenantSlug", tenantSlug,
                "reservationId", reservation.getId(),
                "scheduleId", reservation.getSchedule().getId(),
                "studentId", reservation.getStudentId(),
                "reservationTime", reservation.getReservationTime().toString(),
                "scheduleDate", reservation.getSchedule().getScheduleDate().toString(),
                "startTime", reservation.getSchedule().getStartTime().toString(),
                "endTime", reservation.getSchedule().getEndTime().toString(),
                "timestamp", LocalDateTime.now().toString()
        );

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published reservation.confirmed event for reservation: {}", reservation.getId());
        } catch (Exception e) {
            log.error("Failed to publish reservation.confirmed event", e);
        }
    }
}
