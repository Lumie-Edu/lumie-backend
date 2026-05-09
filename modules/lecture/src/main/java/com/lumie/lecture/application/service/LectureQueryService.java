package com.lumie.lecture.application.service;

import com.lumie.lecture.application.dto.LectureResponse;
import com.lumie.lecture.application.dto.WatchResponse;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.exception.LectureNotFoundException;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import com.lumie.lecture.domain.vo.LectureStatus;
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

    public Page<LectureResponse> findAll(Long classId, Long teacherId,
                                          String status,
                                          LocalDate startDate, LocalDate endDate,
                                          String search, Pageable pageable) {
        LectureSearchCriteria criteria = new LectureSearchCriteria(
                classId, teacherId,
                status != null ? LectureStatus.valueOf(status) : null,
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

        return new WatchResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getDescription(),
                lecture.getYoutubeUrl(),
                lecture.getVisibility().name()
        );
    }
}
