package com.lumie.content.application.service;

import com.lumie.classroom.api.ClassService;
import com.lumie.content.application.dto.request.CreateAnnouncementRequest;
import com.lumie.content.application.dto.request.UpdateAnnouncementRequest;
import com.lumie.content.application.dto.response.AnnouncementResponse;
import com.lumie.content.domain.entity.Announcement;
import com.lumie.content.domain.entity.AnnouncementClassTarget;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.repository.AnnouncementClassTargetRepository;
import com.lumie.content.domain.repository.AnnouncementRepository;
import com.lumie.file.api.FileService;
import com.lumie.staff.api.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementCommandService {

    private static final String DELETED_STAFF_NAME = "삭제된 직원";

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementClassTargetRepository classTargetRepository;
    private final FileService fileService;
    private final ClassService classService;
    private final StaffService staffService;

    @Transactional
    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request) {
        log.info("Creating announcement: {}", request.announcementTitle());

        Announcement announcement = Announcement.create(
                request.authorId(),
                request.announcementTitle(),
                request.announcementContent(),
                request.isItAssetAnnouncement(),
                request.isItImportantAnnouncement()
        );

        Announcement saved = announcementRepository.save(announcement);
        List<Long> classIds = replaceClassTargets(saved.getId(), request.classIds());
        fileService.attachPreparedFiles("announcement", saved.getId(), request.attachmentFileIds());
        log.info("Announcement created with id: {}", saved.getId());

        return AnnouncementResponse.from(saved, authorNameOrFallback(saved.getAuthorId()), classIds);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(Long id, UpdateAnnouncementRequest request) {
        log.info("Updating announcement: {}", id);

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND));

        announcement.update(
                request.announcementTitle(),
                request.announcementContent(),
                request.isItAssetAnnouncement(),
                request.isItImportantAnnouncement()
        );

        fileService.reconcileDirectFiles("announcement", id, request.attachmentFileIds());

        List<Long> classIds = request.classIds() == null
                ? classTargetRepository.findByAnnouncementId(id).stream()
                        .map(AnnouncementClassTarget::getClassId)
                        .toList()
                : replaceClassTargets(id, request.classIds());
        Announcement updated = announcementRepository.save(announcement);
        log.info("Announcement updated: {}", updated.getId());

        return AnnouncementResponse.from(updated, authorNameOrFallback(updated.getAuthorId()), classIds);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        log.info("Deleting announcement: {}", id);

        if (!announcementRepository.existsById(id)) {
            throw new BusinessException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND);
        }

        fileService.deleteFileLinksByEntity("announcement", id);
        fileService.deleteFilesByEntity("announcement", id);
        classTargetRepository.deleteByAnnouncementId(id);
        announcementRepository.deleteById(id);
        log.info("Announcement deleted: {}", id);
    }

    private List<Long> replaceClassTargets(Long announcementId, List<Long> requestedClassIds) {
        List<Long> classIds = normalizeClassIds(requestedClassIds);
        validateClassTargets(classIds);

        classTargetRepository.deleteByAnnouncementId(announcementId);
        classTargetRepository.flush();
        if (!classIds.isEmpty()) {
            classTargetRepository.saveAll(classIds.stream()
                    .map(classId -> AnnouncementClassTarget.create(announcementId, classId))
                    .toList());
        }
        return classIds;
    }

    private void validateClassTargets(List<Long> classIds) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        for (Long classId : classIds) {
            if (classService.getClass(tenantSlug, classId).isEmpty()) {
                throw new BusinessException(ContentErrorCode.INVALID_CLASS_TARGET);
            }
        }
    }

    private List<Long> normalizeClassIds(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        if (classIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ContentErrorCode.INVALID_CLASS_TARGET);
        }
        return classIds.stream()
                .distinct()
                .toList();
    }

    private String authorNameOrFallback(Long authorId) {
        if (authorId == null) {
            return DELETED_STAFF_NAME;
        }
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        return staffService.getStaffByUserId(tenantSlug, authorId)
                .map(StaffService.StaffData::name)
                .orElse(DELETED_STAFF_NAME);
    }
}
