package com.lumie.internal.file;

import java.util.List;
import java.util.UUID;

/**
 * In-process internal API exposed by file-svc and consumed by other services.
 */
public interface FileInternalService {

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
}
