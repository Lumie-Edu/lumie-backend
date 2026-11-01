package com.lumie.file.adapter.out.persistence;

import com.lumie.file.application.port.out.FileDownloadPersistencePort;
import com.lumie.file.domain.entity.FileDownload;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FileDownloadPersistenceAdapter implements FileDownloadPersistencePort {
    private final FileDownloadJpaRepository repository;

    @Override
    @Transactional
    public int upsertDownload(Long tenantId, UUID fileId, Long userId) {
        return repository.upsertDownload(tenantId, fileId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileDownload> findByFileIdOrderByUpdatedAtDesc(UUID fileId) {
        return repository.findByFileIdOrderByUpdatedAtDesc(fileId);
    }
}
