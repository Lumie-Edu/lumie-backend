package com.lumie.lecture.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.lecture.application.dto.*;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.exception.LectureErrorCode;
import com.lumie.lecture.domain.exception.LectureNotFoundException;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureStatus;
import com.lumie.lecture.domain.vo.LectureVisibility;
import com.lumie.lecture.domain.vo.ProcessingStatus;
import com.lumie.messaging.config.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LectureCommandService {

    private final LectureRepository lectureRepository;
    private final RabbitTemplate rabbitTemplate;

    public LectureResponse create(CreateLectureRequest request) {
        Long teacherId = request.teacherId() != null
                ? request.teacherId()
                : UserContextHolder.getUserId();

        LectureVisibility visibility = request.visibility() != null
                ? LectureVisibility.valueOf(request.visibility())
                : LectureVisibility.TEACHER_ONLY;

        Lecture lecture = Lecture.create(
                request.classId(),
                teacherId,
                request.title(),
                request.description(),
                request.youtubeUrl(),
                visibility,
                request.lectureDate()
        );

        Lecture saved = lectureRepository.save(lecture);
        publishProcessingMessage(saved);

        return LectureResponse.from(saved);
    }

    public LectureResponse update(Long id, UpdateLectureRequest request) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(LectureNotFoundException::new);

        LectureVisibility visibility = request.visibility() != null
                ? LectureVisibility.valueOf(request.visibility()) : null;
        LectureStatus status = request.status() != null
                ? LectureStatus.valueOf(request.status()) : null;

        lecture.update(request.title(), request.description(),
                visibility, status, request.lectureDate());

        return LectureResponse.from(lectureRepository.save(lecture));
    }

    public void delete(Long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(LectureNotFoundException::new);
        lecture.close();
        lectureRepository.save(lecture);
    }

    public LectureResponse reprocess(Long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(LectureNotFoundException::new);

        ProcessingStatus current = lecture.getProcessingStatus();
        if (current != ProcessingStatus.FAILED && current != ProcessingStatus.COMPLETED) {
            throw new BusinessException(LectureErrorCode.PROCESSING_IN_PROGRESS);
        }

        lecture.requestReprocessing();
        Lecture saved = lectureRepository.save(lecture);
        publishProcessingMessage(saved);

        return LectureResponse.from(saved);
    }

    public void handleProcessingCallback(ProcessingCallbackRequest request) {
        Lecture lecture = lectureRepository.findById(request.lectureId())
                .orElseThrow(LectureNotFoundException::new);

        if (request.success()) {
            lecture.completeProcessing(
                    request.transcript(),
                    request.subtitleFileKey(),
                    request.aiSummary(),
                    request.topicsExtracted(),
                    request.videoDuration()
            );
        } else {
            lecture.failProcessing(request.errorMessage());
        }

        lectureRepository.save(lecture);
        log.info("Processing callback received for lecture {}: success={}",
                request.lectureId(), request.success());
    }

    public void updateProcessingStatus(Long id, ProcessingStatusUpdate update) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(LectureNotFoundException::new);

        ProcessingStatus status = ProcessingStatus.valueOf(update.status());
        lecture.updateProcessingStatus(status);
        lectureRepository.save(lecture);
    }

    private void publishProcessingMessage(Lecture lecture) {
        String tenantSlug = TenantContextHolder.getTenantSlug();

        LectureProcessingMessage message = new LectureProcessingMessage(
                lecture.getId(),
                tenantSlug,
                lecture.getYoutubeUrl()
        );

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.LUMIE_COMMANDS_EXCHANGE,
                RabbitMqConstants.LECTURE_PROCESS_ROUTING_KEY,
                message
        );

        log.info("Published lecture processing message for lecture {} (tenant: {})",
                lecture.getId(), tenantSlug);
    }
}
