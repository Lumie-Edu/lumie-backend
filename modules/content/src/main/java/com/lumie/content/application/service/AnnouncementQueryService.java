package com.lumie.content.application.service;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.auth.CurrentStudentResolver;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.content.application.dto.response.AnnouncementResponse;
import com.lumie.content.domain.entity.Announcement;
import com.lumie.content.domain.entity.AnnouncementClassTarget;
import com.lumie.staff.api.StaffService;
import com.lumie.common.exception.BusinessException;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.repository.AnnouncementClassTargetRepository;
import com.lumie.content.domain.repository.AnnouncementRepository;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnnouncementQueryService {

    private static final String DELETED_STUDENT_NAME = "삭제된 학생";
    private static final String DELETED_STAFF_NAME = "삭제된 직원";

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementClassTargetRepository classTargetRepository;
    private final com.lumie.content.domain.repository.AnnouncementReadRepository announcementReadRepository;
    private final StaffService staffService;
    private final StudentService studentService;
    private final ClassService classService;

    public Page<AnnouncementResponse> listAnnouncements(Boolean isAsset, Pageable pageable) {
        log.debug("Listing announcements with pagination, isAsset={}", isAsset);

        List<Long> visibleClassIds = currentStudentClassIdsOrNull();
        Page<Announcement> page = findVisibleAnnouncements(isAsset, visibleClassIds, pageable);

        Map<Long, String> authorNames = resolveAuthorNames(page.getContent());
        Map<Long, List<Long>> classIdsByAnnouncementId = resolveClassTargets(page.getContent());
        return page.map(a -> AnnouncementResponse.from(
                a,
                authorNameOrFallback(a.getAuthorId(), authorNames),
                responseClassIds(classIdsByAnnouncementId.getOrDefault(a.getId(), List.of()))
        ));
    }

    public List<AnnouncementResponse> listImportantAnnouncements() {
        log.debug("Listing important announcements");

        List<Long> visibleClassIds = currentStudentClassIdsOrNull();
        List<Announcement> announcements = visibleClassIds == null
                ? announcementRepository.findByIsItImportantAnnouncementTrue()
                : visibleClassIds.isEmpty()
                        ? announcementRepository.findImportantVisibleWithoutClassTargets()
                        : announcementRepository.findImportantVisibleByClassTargets(visibleClassIds);
        Map<Long, String> authorNames = resolveAuthorNames(announcements);
        Map<Long, List<Long>> classIdsByAnnouncementId = resolveClassTargets(announcements);
        return announcements.stream()
                .map(a -> AnnouncementResponse.from(
                        a,
                        authorNameOrFallback(a.getAuthorId(), authorNames),
                        responseClassIds(classIdsByAnnouncementId.getOrDefault(a.getId(), List.of()))
                ))
                .toList();
    }

    @Transactional
    public AnnouncementResponse getAnnouncement(Long id) {
        log.debug("Getting announcement: {}", id);

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND));

        List<Long> classIds = classTargetRepository.findByAnnouncementId(id).stream()
                .map(AnnouncementClassTarget::getClassId)
                .toList();
        ensureVisibleToCurrentUser(classIds);

        if (isStudentUser()) {
            Long userId = UserContextHolder.getRequiredUserId();
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            announcementReadRepository.insertIfAbsent(tenantId, id, userId);
        }

        Map<Long, String> authorNames = resolveAuthorNames(List.of(announcement));
        return AnnouncementResponse.from(
                announcement,
                authorNameOrFallback(announcement.getAuthorId(), authorNames),
                responseClassIds(classIds)
        );
    }

    public List<String> getAnnouncementReaders(Long id) {
        log.debug("Getting readers for announcement: {}", id);

        AuthorizationGuard.requireStaff();
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND));

        List<Long> userIds = announcementReadRepository.findUserIdsByAnnouncementId(id);
        if (userIds.isEmpty()) {
            return List.of();
        }

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        var students = studentService.getStudentsByUserIds(tenantSlug, userIds).students();

        Map<Long, String> nameMap = students.stream()
                .collect(Collectors.toMap(StudentService.StudentData::userId, StudentService.StudentData::studentName, (a, b) -> a));

        return userIds.stream()
                .map(uid -> nameMap.getOrDefault(uid, DELETED_STUDENT_NAME))
                .toList();
    }

    public boolean canUserViewAnnouncement(Long id, String role, Long userId) {
        if (!announcementRepository.existsById(id)) {
            return false;
        }

        List<Long> classIds = classTargetRepository.findByAnnouncementId(id).stream()
                .map(AnnouncementClassTarget::getClassId)
                .toList();
        Role parsedRole = role == null ? null : Role.valueOf(role);
        return isVisibleToUser(classIds, parsedRole, userId);
    }

    private List<Long> responseClassIds(List<Long> classIds) {
        return isStudentUser() ? List.of() : classIds;
    }

    private Page<Announcement> findVisibleAnnouncements(
            Boolean isAsset,
            List<Long> visibleClassIds,
            Pageable pageable) {
        if (visibleClassIds == null) {
            return isAsset != null
                    ? announcementRepository.findByIsItAssetAnnouncementOrderByImportantDescCreatedAtDesc(isAsset, pageable)
                    : announcementRepository.findAllOrderByImportantDescCreatedAtDesc(pageable);
        }

        if (visibleClassIds.isEmpty()) {
            return isAsset != null
                    ? announcementRepository.findVisibleWithoutClassTargetsByIsAsset(isAsset, pageable)
                    : announcementRepository.findVisibleWithoutClassTargets(pageable);
        }

        return isAsset != null
                ? announcementRepository.findVisibleByClassTargetsAndIsAsset(isAsset, visibleClassIds, pageable)
                : announcementRepository.findVisibleByClassTargets(visibleClassIds, pageable);
    }

    private void ensureVisibleToCurrentUser(List<Long> classIds) {
        if (isVisibleToCurrentUser(classIds)) {
            return;
        }

        throw new BusinessException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND);
    }

    private boolean isVisibleToCurrentUser(List<Long> classIds) {
        return isVisibleToUser(classIds, UserContextHolder.getUserRole(), UserContextHolder.getUserId());
    }

    private boolean isVisibleToUser(List<Long> classIds, Role role, Long userId) {
        if (role == null) {
            return false;
        }
        if (role != Role.STUDENT || classIds.isEmpty()) {
            return true;
        }
        if (userId == null) {
            return false;
        }

        List<Long> visibleClassIds = currentStudentClassIds(userId);
        return classIds.stream().anyMatch(visibleClassIds::contains);
    }

    private List<Long> currentStudentClassIdsOrNull() {
        if (!isStudentUser()) {
            return null;
        }

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Long userId = UserContextHolder.getRequiredUserId();
        return currentStudentClassIds(userId);
    }

    private List<Long> currentStudentClassIds(Long userId) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Long studentId = CurrentStudentResolver.requireStudentId(
                studentService, tenantSlug, userId, ContentErrorCode.STUDENT_NOT_FOUND);

        return classService.getClassesByStudent(tenantSlug, studentId, 0, 1000)
                .classes()
                .stream()
                .map(ClassService.ClassData::id)
                .toList();
    }

    private boolean isStudentUser() {
        return UserContextHolder.getUserRole() == Role.STUDENT;
    }



    private Map<Long, List<Long>> resolveClassTargets(List<Announcement> announcements) {
        List<Long> announcementIds = announcements.stream()
                .map(Announcement::getId)
                .toList();
        if (announcementIds.isEmpty()) {
            return Map.of();
        }

        return classTargetRepository.findByAnnouncementIdIn(announcementIds).stream()
                .collect(Collectors.groupingBy(
                        AnnouncementClassTarget::getAnnouncementId,
                        Collectors.mapping(AnnouncementClassTarget::getClassId, Collectors.toList())
                ));
    }

    private Map<Long, String> resolveAuthorNames(List<Announcement> announcements) {
        List<Long> authorIds = announcements.stream()
                .map(Announcement::getAuthorId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (authorIds.isEmpty()) {
            return Map.of();
        }

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        return authorIds.stream()
                .flatMap(authorId -> staffService.getStaffByUserId(tenantSlug, authorId)
                        .map(staff -> Map.entry(authorId, staff.name()))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }

    private String authorNameOrFallback(Long authorId, Map<Long, String> authorNames) {
        if (authorId == null) {
            return DELETED_STAFF_NAME;
        }
        return authorNames.getOrDefault(authorId, DELETED_STAFF_NAME);
    }
}
