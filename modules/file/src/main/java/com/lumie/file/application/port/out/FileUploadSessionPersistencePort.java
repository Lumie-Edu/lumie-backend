package com.lumie.file.application.port.out;

import com.lumie.file.domain.entity.FileUploadSession;
import java.util.Optional;
import java.util.UUID;

public interface FileUploadSessionPersistencePort {
    Optional<FileUploadSession> findById(UUID id);

    Optional<FileUploadSession> findByIdForUpdate(UUID id);

    FileUploadSession save(FileUploadSession session);
}
