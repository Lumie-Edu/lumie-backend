package com.lumie.lecture.application.service;

import com.lumie.common.auth.CurrentStudentResolver;
import com.lumie.lecture.domain.vo.LectureTargets;
import com.lumie.lecture.application.dto.response.LectureResponse;
import com.lumie.lecture.application.dto.response.WatchResponse;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.common.exception.BusinessException;
import com.lumie.lecture.domain.exception.LectureErrorCode;
import com.lumie.lecture.domain.repository.LectureClassTargetRepository;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import com.lumie.lecture.domain.vo.LectureVisibility;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureQueryService {

    private final LectureRepository lectureRepository;
    private final LectureClassTargetRepository targetRepository;
    private final StudentService studentService;
    private final ClassService classService;

    public Page<LectureResponse> findAll(Long classId, Long teacherId,
                                          LocalDate startDate, LocalDate endDate,
                                          String search, Pageable pageable) {
        LectureSearchCriteria criteria = new LectureSearchCriteria(
                classId, teacherId,
                startDate, endDate, search
        );

        Page<Lecture> lectures = lectureRepository.findAll(criteria, pageable);
        Map<Long, List<Long>> classIdsByLecture = loadClassIds(lectures.getContent());
        return lectures.map(lecture -> LectureResponse.from(
                lecture,
                LectureTargets.primaryFirst(
                        classIdsByLecture.getOrDefault(lecture.getId(), List.of()),
                        lecture.getClassId()
                )
        ));
    }

    public LectureResponse findById(Long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));
        return LectureResponse.from(lecture, classIdsForResponse(lecture));
    }

    public Page<LectureResponse> findByClassIdForStudent(Long classId, Pageable pageable) {
        validateCurrentStudentClassAccess(classId);
        Page<Lecture> lectures = lectureRepository.findVisibleByTargetClassId(classId, pageable);
        return lectures.map(lecture -> LectureResponse.from(lecture, List.of(classId)));
    }

    public WatchResponse getWatchUrls(Long lectureId, Long classId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));
        List<Long> classIds = classIdsForResponse(lecture);
        Long visibleClassId = resolveVisibleClassId(classId, classIds);
        if (lecture.getVisibility() != LectureVisibility.STUDENT
                || visibleClassId == null) {
            throw new BusinessException(LectureErrorCode.LECTURE_NOT_VISIBLE_TO_CLASS);
        }

        return new WatchResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getDescription(),
                lecture.getYoutubeUrl(),
                lecture.getVisibility()
        );
    }

    private void validateCurrentStudentClassAccess(Long classId) {
        if (classId == null) {
            throw new BusinessException(LectureErrorCode.LECTURE_NOT_VISIBLE_TO_CLASS);
        }
        Long studentId = currentStudentId();
        if (!classService.isStudentEnrolledInClass(TenantContextHolder.getRequiredTenant(), studentId, classId)) {
            throw new BusinessException(LectureErrorCode.LECTURE_NOT_VISIBLE_TO_CLASS);
        }
    }

    private Long resolveVisibleClassId(Long requestedClassId, List<Long> targetClassIds) {
        if (requestedClassId != null) {
            validateCurrentStudentClassAccess(requestedClassId);
            return targetClassIds.contains(requestedClassId) ? requestedClassId : null;
        }
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Long studentId = currentStudentId();
        return targetClassIds.stream()
                .filter(classId -> classService.isStudentEnrolledInClass(tenantSlug, studentId, classId))
                .findFirst()
                .orElse(null);
    }

    private Long currentStudentId() {
        return CurrentStudentResolver.requireStudentId(studentService, CommonErrorCode.FORBIDDEN);
    }

    private Map<Long, List<Long>> loadClassIds(List<Lecture> lectures) {
        if (lectures.isEmpty()) {
            return Map.of();
        }
        List<Long> lectureIds = lectures.stream().map(Lecture::getId).toList();
        Map<Long, List<Long>> grouped = new HashMap<>();
        targetRepository.findRowsByLectureIds(lectureIds).forEach(row ->
                grouped.computeIfAbsent(row.getLectureId(), ignored -> new java.util.ArrayList<>())
                        .add(row.getClassId())
        );
        return grouped;
    }

    private List<Long> classIdsForResponse(Lecture lecture) {
        List<Long> classIds = targetRepository.findByLectureIdOrderByClassIdAsc(lecture.getId()).stream()
                .map(target -> target.getClassId())
                .toList();
        return LectureTargets.primaryFirst(classIds, lecture.getClassId());
    }


}
