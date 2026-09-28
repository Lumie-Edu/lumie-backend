package com.lumie.file.api;

import java.util.List;
import java.util.UUID;

/**
 * In-process internal API exposed by file-svc and consumed by other services.
 */
public interface FileService {

    /** Validates and copies tmp objects outside the caller's DB transaction. */
    List<PreparedTemporaryFile> prepareTemporaryFiles(String entityType, List<TemporaryAttachment> attachments);

    /** Persists metadata for objects prepared before the current DB transaction. */
    List<UUID> attachPreparedFiles(String entityType, Long entityId, List<PreparedTemporaryFile> files);

    List<UUID> attachPreparedFiles(String entityType, Long entityId, List<PreparedTemporaryFile> files, UUID folderId);

    /** Best-effort compensation for prepared objects when the DB transaction fails. */
    void discardPreparedFiles(List<PreparedTemporaryFile> files);

    /** Reconciles retained direct attachments and attaches already-prepared replacements. */
    void reconcilePreparedDirectFiles(String entityType, Long entityId, List<UUID> retainedFileIds,
                                      List<PreparedTemporaryFile> preparedFiles);

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

}
