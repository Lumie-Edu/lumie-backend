package com.lumie.file.application.port.out;

import com.lumie.file.domain.entity.FileDownload;
import java.util.List;
import java.util.UUID;

public interface FileDownloadPersistencePort {
    int upsertDownload(Long tenantId, UUID fileId, Long userId);

    List<FileDownload> findByFileIdOrderByUpdatedAtDesc(UUID fileId);
}
