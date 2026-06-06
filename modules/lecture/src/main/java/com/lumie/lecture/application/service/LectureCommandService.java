package com.lumie.lecture.application.service;

import com.lumie.lecture.application.dto.request.CreateLectureRequest;
import com.lumie.lecture.application.dto.request.UpdateLectureRequest;
import com.lumie.lecture.application.dto.response.LectureResponse;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.auth.Role;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.entity.LectureClassTarget;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.lecture.domain.exception.LectureErrorCode;
import com.lumie.lecture.domain.repository.LectureClassTargetRepository;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureVisibility;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.staff.api.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LectureCommandService {

    private final LectureRepository lectureRepository;
    private final LectureClassTargetRepository targetRepository;
    private final StaffService staffService;
    private final ClassService classService;

    public LectureResponse create(CreateLectureRequest request) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Long teacherId = request.teacherId();
        if (teacherId == null) {
            teacherId = staffService.getStaffByUserId(tenantSlug, UserContextHolder.getRequiredUserId())
                    .map(StaffService.StaffData::id)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
        }

        LectureVisibility visibility = request.visibility() != null
                ? LectureVisibility.valueOf(request.visibility())
                : LectureVisibility.TEACHER_ONLY;
        List<Long> classIds = normalizeClassIds(request.classId(), request.classIds());
        requireCanManageLecture(tenantSlug, teacherId);
        validateTargets(tenantSlug, visibility, classIds, teacherId);

        Lecture lecture = Lecture.create(
                firstClassId(classIds),
                teacherId,
                request.title(),
                request.description(),
                request.youtubeUrl(),
                visibility,
                request.lectureDate()
        );

        Lecture saved = lectureRepository.save(lecture);
        syncTargets(saved, classIds);

        return LectureResponse.from(saved, classIdsForResponse(saved.getId(), saved.getClassId()));
    }

    public LectureResponse update(Long id, UpdateLectureRequest request) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));

        LectureVisibility visibility = request.visibility() != null
                ? LectureVisibility.valueOf(request.visibility()) : null;
        LectureVisibility effectiveVisibility = visibility != null ? visibility : lecture.getVisibility();
        boolean targetsProvided = request.classId() != null || request.classIds() != null;
        List<Long> classIds = targetsProvided
                ? normalizeClassIds(request.classId(), request.classIds())
                : classIdsForResponse(lecture.getId(), lecture.getClassId());
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        requireCanManageLecture(tenantSlug, lecture.getTeacherId());
        if (targetsProvided) {
            validateTargets(tenantSlug, effectiveVisibility, classIds, lecture.getTeacherId());
        } else {
            validateTargetPresence(effectiveVisibility, classIds);
        }

        lecture.update(request.title(), request.description(), request.youtubeUrl(), visibility, request.lectureDate());

        if (targetsProvided) {
            lecture.updatePrimaryClassId(firstClassId(classIds));
            syncTargets(lecture, classIds);
        }

        Lecture saved = lectureRepository.save(lecture);
        return LectureResponse.from(saved, classIdsForResponse(saved.getId(), saved.getClassId()));
    }

    public void delete(Long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));
        requireCanManageLecture(TenantContextHolder.getRequiredTenant(), lecture.getTeacherId());
        targetRepository.deleteByLectureId(lecture.getId());
        lectureRepository.delete(lecture);
    }

    private void syncTargets(Lecture lecture, List<Long> classIds) {
        targetRepository.deleteByLectureId(lecture.getId());
        for (Long classId : classIds) {
            targetRepository.save(LectureClassTarget.create(lecture, classId));
        }
    }

    private void validateTargets(String tenantSlug, LectureVisibility visibility, List<Long> classIds, Long teacherId) {
        validateTargetPresence(visibility, classIds);
        Long actorStaffId = currentInstructorStaffId(tenantSlug);
        for (Long classId : classIds) {
            ClassService.ClassData classData = classService.getClass(tenantSlug, classId)
                    .orElseThrow(() -> new BusinessException(
                            LectureErrorCode.CLASS_NOT_FOUND,
                            "Class not found with id: " + classId
                    ));
            if (actorStaffId != null && !actorStaffId.equals(classData.teacherId())) {
                throw new BusinessException(
                        CommonErrorCode.FORBIDDEN,
                        "Instructor can manage only assigned classes"
                );
            }
        }
    }

    private void validateTargetPresence(LectureVisibility visibility, List<Long> classIds) {
        if (visibility == LectureVisibility.STUDENT && classIds.isEmpty()) {
            throw new BusinessException(LectureErrorCode.STUDENT_VISIBILITY_REQUIRES_CLASS);
        }
    }

    private void requireCanManageLecture(String tenantSlug, Long teacherId) {
        Long actorStaffId = currentInstructorStaffId(tenantSlug);
        if (actorStaffId != null && !actorStaffId.equals(teacherId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private Long currentInstructorStaffId(String tenantSlug) {
        Role role = UserContextHolder.getUserRole();
        if (role == null || role == Role.STUDENT) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (role != Role.INSTRUCTOR) {
            return null;
        }
        return staffService.getStaffByUserId(tenantSlug, UserContextHolder.getRequiredUserId())
                .map(StaffService.StaffData::id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
    }

    private List<Long> classIdsForResponse(Long lectureId, Long fallbackClassId) {
        List<Long> classIds = targetRepository.findByLectureIdOrderByClassIdAsc(lectureId).stream()
                .map(LectureClassTarget::getClassId)
                .toList();
        return primaryFirst(classIds, fallbackClassId);
    }

    private static List<Long> normalizeClassIds(Long classId, List<Long> classIds) {
        Set<Long> deduplicated = new LinkedHashSet<>();
        if (classId != null) {
            deduplicated.add(classId);
        }
        if (classIds != null) {
            classIds.stream()
                    .filter(id -> id != null)
                    .forEach(deduplicated::add);
        }
        return List.copyOf(deduplicated);
    }

    private static Long firstClassId(List<Long> classIds) {
        return classIds.isEmpty() ? null : classIds.get(0);
    }

    private static List<Long> primaryFirst(List<Long> classIds, Long primaryClassId) {
        if (classIds.isEmpty()) {
            return primaryClassId == null ? List.of() : List.of(primaryClassId);
        }
        if (primaryClassId == null || !classIds.contains(primaryClassId)) {
            return classIds;
        }
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(primaryClassId),
                        classIds.stream().filter(id -> !id.equals(primaryClassId))
                )
                .toList();
    }
}
