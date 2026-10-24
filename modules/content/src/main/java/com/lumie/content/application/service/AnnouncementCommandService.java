package com.lumie.content.application.service;

import com.lumie.classroom.api.ClassService;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.content.application.dto.request.CreateAnnouncementRequest;
import com.lumie.content.application.dto.request.UpdateAnnouncementRequest;
import com.lumie.content.application.dto.response.AnnouncementResponse;
import com.lumie.content.domain.entity.Announcement;
import com.lumie.content.domain.entity.AnnouncementClassTarget;
import com.lumie.content.domain.vo.AnnouncementAudienceScope;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.repository.AnnouncementClassTargetRepository;
import com.lumie.content.domain.repository.AnnouncementRepository;
import com.lumie.file.api.FileService;
import com.lumie.file.api.PreparedTemporaryFile;
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
        AuthorizationGuard.requireStaff();
        log.info("Creating announcement: {}", request.announcementTitle());
        List<Long> requestedClassIds = normalizeClassIds(request.classIds());
        AnnouncementAudienceScope audienceScope = createScope(request.audienceScope(), requestedClassIds);
        List<PreparedTemporaryFile> preparedFiles = fileService.prepareTemporaryFiles("announcement", request.temporaryAttachments());

        try {
        Announcement announcement = Announcement.create(
                UserContextHolder.getRequiredUserId(),
                request.announcementTitle(),
                request.announcementContent(),
                request.isItAssetAnnouncement(),
                request.isItImportantAnnouncement(),
                audienceScope
        );

        Announcement saved = announcementRepository.save(announcement);
        List<Long> classIds = replaceClassTargets(saved.getId(), requestedClassIds);
        fileService.attachPreparedFiles("announcement", saved.getId(), preparedFiles);
        announcementRepository.flush();
        log.info("Announcement created with id: {}", saved.getId());

        return AnnouncementResponse.from(saved, authorNameOrFallback(saved.getAuthorId()), classIds);
        } catch (RuntimeException exception) {
            fileService.discardPreparedFiles(preparedFiles);
            throw exception;
        }
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(Long id, UpdateAnnouncementRequest request) {
        AuthorizationGuard.requireStaff();
        log.info("Updating announcement: {}", id);
        List<PreparedTemporaryFile> preparedFiles = fileService.prepareTemporaryFiles("announcement", request.temporaryAttachments());

        try {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND));

        List<Long> classIds = request.classIds() == null
                ? classTargetRepository.findByAnnouncementId(id).stream()
                        .map(AnnouncementClassTarget::getClassId)
                        .toList()
                : normalizeClassIds(request.classIds());
        AnnouncementAudienceScope audienceScope = updateScope(
                announcement.getAudienceScope(), request.audienceScope(),
                request.classIds() != null, classIds);

        announcement.update(
                request.announcementTitle(),
                request.announcementContent(),
                request.isItAssetAnnouncement(),
                request.isItImportantAnnouncement()
        );
        announcement.changeAudienceScope(audienceScope);

        fileService.reconcilePreparedDirectFiles("announcement", id, request.attachmentFileIds(), preparedFiles);

        if (request.classIds() != null || audienceScope == AnnouncementAudienceScope.ALL) {
            classIds = replaceClassTargets(id, audienceScope == AnnouncementAudienceScope.ALL ? List.of() : classIds);
        }
        Announcement updated = announcementRepository.save(announcement);
        announcementRepository.flush();
        log.info("Announcement updated: {}", updated.getId());

        return AnnouncementResponse.from(updated, authorNameOrFallback(updated.getAuthorId()), classIds);
        } catch (RuntimeException exception) {
            fileService.discardPreparedFiles(preparedFiles);
            throw exception;
        }
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        AuthorizationGuard.requireStaff();
        log.info("Deleting announcement: {}", id);

        if (!announcementRepository.existsById(id)) {
            throw new BusinessException(ContentErrorCode.ANNOUNCEMENT_NOT_FOUND);
        }

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

    private AnnouncementAudienceScope createScope(AnnouncementAudienceScope requested, List<Long> classIds) {
        if (requested == null) {
            if (classIds.isEmpty()) {
                throw new BusinessException(ContentErrorCode.INVALID_ANNOUNCEMENT_AUDIENCE);
            }
            return AnnouncementAudienceScope.TARGETED;
        }
        validateScope(requested, classIds);
        return requested;
    }

    private AnnouncementAudienceScope updateScope(AnnouncementAudienceScope current,
                                                   AnnouncementAudienceScope requested,
                                                   boolean targetsProvided, List<Long> classIds) {
        if (requested != null) {
            validateScope(requested, requested == AnnouncementAudienceScope.ALL ? List.of() : classIds);
            if (requested == AnnouncementAudienceScope.ALL && !classIds.isEmpty() && targetsProvided) {
                throw new BusinessException(ContentErrorCode.INVALID_ANNOUNCEMENT_AUDIENCE);
            }
            return requested;
        }
        if (targetsProvided && !classIds.isEmpty()) {
            return AnnouncementAudienceScope.TARGETED;
        }
        return current;
    }

    private void validateScope(AnnouncementAudienceScope scope, List<Long> classIds) {
        if ((scope == AnnouncementAudienceScope.ALL && !classIds.isEmpty())
                || (scope == AnnouncementAudienceScope.TARGETED && classIds.isEmpty())) {
            throw new BusinessException(ContentErrorCode.INVALID_ANNOUNCEMENT_AUDIENCE);
        }
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
