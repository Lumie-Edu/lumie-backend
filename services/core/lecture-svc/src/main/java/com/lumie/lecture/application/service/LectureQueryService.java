package com.lumie.lecture.application.service;

import com.lumie.lecture.application.dto.LectureResponse;
import com.lumie.lecture.application.dto.WatchResponse;
import com.lumie.lecture.application.port.out.LectureStoragePort;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.exception.LectureNotFoundException;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import com.lumie.lecture.domain.vo.LectureStatus;
import com.lumie.lecture.domain.vo.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureQueryService {

    private final LectureRepository lectureRepository;
    private final LectureStoragePort storagePort;

    private static final int VIDEO_URL_EXPIRY_SECONDS = 3600; // 1 hour
    private static final int SUBTITLE_URL_EXPIRY_SECONDS = 3600;

    public Page<LectureResponse> findAll(Long classId, Long academyId, Long teacherId,
                                          String status, String processingStatus,
                                          LocalDate startDate, LocalDate endDate,
                                          String search, Pageable pageable) {
        LectureSearchCriteria criteria = new LectureSearchCriteria(
                classId, academyId, teacherId,
                status != null ? LectureStatus.valueOf(status) : null,
                processingStatus != null ? ProcessingStatus.valueOf(processingStatus) : null,
                startDate, endDate, search
        );

        return lectureRepository.findAll(criteria, pageable)
                .map(LectureResponse::from);
    }

    public LectureResponse findById(Long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(LectureNotFoundException::new);
        return LectureResponse.from(lecture);
    }

    public Page<LectureResponse> findByClassIdForStudent(Long classId, Pageable pageable) {
        return lectureRepository.findVisibleByClassId(classId, pageable)
                .map(LectureResponse::from);
    }

    public WatchResponse getWatchUrls(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(LectureNotFoundException::new);

        String videoUrl = null;
        String subtitleUrl = null;

        if (lecture.getVideoFileKey() != null && lecture.isUploadCompleted()) {
            videoUrl = storagePort.generatePresignedDownloadUrl(
                    lecture.getVideoFileKey(), VIDEO_URL_EXPIRY_SECONDS);
        }

        if (lecture.getSubtitleFileKey() != null) {
            subtitleUrl = storagePort.generatePresignedDownloadUrl(
                    lecture.getSubtitleFileKey(), SUBTITLE_URL_EXPIRY_SECONDS);
        }

        return new WatchResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getDescription(),
                videoUrl,
                subtitleUrl,
                lecture.getTranscript(),
                lecture.getAiSummary(),
                lecture.getTopicsExtracted(),
                lecture.getVideoDuration(),
                lecture.getVisibility().name()
        );
    }
}
