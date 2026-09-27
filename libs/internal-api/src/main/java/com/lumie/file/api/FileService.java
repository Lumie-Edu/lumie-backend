package com.lumie.file.api;

import java.util.List;
import java.util.UUID;

/**
 * In-process internal API exposed by file-svc and consumed by other services.
 */
public interface FileService {

    /**
     * Copies completed, caller-owned temporary attachments to their durable
     * object keys. This must be called before the content transaction begins.
     */
    void publishTemporaryFiles(String entityType, Long entityId, List<TemporaryAttachment> attachments);

    /**
     * Deletes all files (MinIO objects + metadata) associated with the given entity.
     * DB deletion runs within the caller's transaction; MinIO deletion runs after commit.
     *
     * @param entityType the entity type string (e.g. "announcement", "qna")
     * @param entityId   the entity id
     */
    void deleteFilesByEntity(String entityType, Long entityId);

    /**
     * Deletes specific files by their IDs (MinIO objects + metadata).
     * DB deletion runs within the caller's transaction; MinIO deletion runs after commit.
     *
     * @param fileIds list of file metadata UUIDs to delete
     */
    void deleteFilesByIds(List<UUID> fileIds);

    /** Deletes only files directly owned by the specified entity. */
    void deleteFilesByEntityAndIds(String entityType, Long entityId, List<UUID> fileIds);

    /** Reconciles the direct files owned by an entity to the requested final list. Null means unchanged. */
    void reconcileDirectFiles(String entityType, Long entityId, List<UUID> retainedFileIds,
                              List<TemporaryAttachment> newAttachments);

    /**
     * Replaces reusable file links for the given entity. The source files stay
     * owned by their original entity type and are not copied or deleted.
     *
     * @param entityType target entity type string (e.g. "announcement")
     * @param entityId   target entity id
     * @param fileIds    file metadata UUIDs to link
     */
    void replaceFileLinks(String entityType, Long entityId, List<UUID> fileIds);

    /**
     * Deletes reusable file links for the given entity without deleting source files.
     *
     * @param entityType target entity type string (e.g. "announcement")
     * @param entityId   target entity id
     */
    void deleteFileLinksByEntity(String entityType, Long entityId);
}
