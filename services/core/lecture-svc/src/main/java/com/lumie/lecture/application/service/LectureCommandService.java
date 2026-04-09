package com.lumie.lecture.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.lecture.application.dto.*;
import com.lumie.lecture.application.port.out.LectureStoragePort;
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

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LectureCommandService {

    private final LectureRepository lectureRepository;
    private final LectureStoragePort storagePort;
    private final RabbitTemplate rabbitTemplate;

    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024; // 2GB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo"
    );

    public LectureResponse create(CreateLectureRequest request) {
        Long teacherId = request.teacherId() != null
                ? request.teacherId()
                : UserContextHolder.getUserId();

        LectureVisibility visibility = request.visibility() != null
                ? LectureVisibility.valueOf(request.visibility())
                : LectureVisibility.TEACHER_ONLY;

        Lecture lecture = Lecture.create(
                request.classId(),
                request.academyId(),
                teacherId,
                request.title(),
                request.description(),
                visibility,
                request.lectureDate()
        );

        return LectureResponse.from(lectureRepository.save(lecture));
    }

    public InitiateUploadResponse initiateUpload(InitiateUploadRequest request) {
        if (!ALLOWED_MIME_TYPES.contains(request.contentType())) {
            throw new BusinessException(LectureErrorCode.INVALID_FILE_TYPE);
        }
        if (request.fileSize() > MAX_FILE_SIZE) {
            throw new BusinessException(LectureErrorCode.FILE_TOO_LARGE);
        }

        Lecture lecture = lectureRepository.findById(request.lectureId())
                .orElseThrow(LectureNotFoundException::new);

        String objectKey = generateObjectKey(lecture, request.filename());

        String presignedUrl = storagePort.generatePresignedUploadUrl(objectKey, request.contentType());

        lecture.initiateUpload(null, objectKey, request.filename(),
                request.fileSize(), request.contentType());
        lectureRepository.save(lecture);

        return new InitiateUploadResponse(lecture.getId(), presignedUrl, objectKey);
    }

    public LectureResponse completeUpload(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(LectureNotFoundException::new);

        if (!storagePort.objectExists(lecture.getVideoFileKey())) {
            throw new BusinessException(LectureErrorCode.UPLOAD_NOT_COMPLETED);
        }

        lecture.completeUpload();
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

        if (!lecture.isUploadCompleted()) {
            throw new BusinessException(LectureErrorCode.UPLOAD_NOT_COMPLETED);
        }

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
                lecture.getVideoFileKey(),
                lecture.getVideoMimeType(),
                lecture.getVideoFileSize()
        );

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.LUMIE_COMMANDS_EXCHANGE,
                RabbitMqConstants.LECTURE_PROCESS_ROUTING_KEY,
                message
        );

        log.info("Published lecture processing message for lecture {} (tenant: {})",
                lecture.getId(), tenantSlug);
    }

    private String generateObjectKey(Lecture lecture, String filename) {
        String tenantSlug = TenantContextHolder.getTenantSlug();
        String extension = filename.contains(".")
                ? filename.substring(filename.lastIndexOf("."))
                : "";
        return String.format("lectures/%s/%d/%s%s",
                tenantSlug, lecture.getId(), UUID.randomUUID(), extension);
    }
}
