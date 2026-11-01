package com.lumie.file.adapter.out.persistence;

import com.lumie.file.application.port.out.FileUploadSessionPersistencePort;
import com.lumie.file.domain.entity.FileUploadSession;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FileUploadSessionPersistenceAdapter implements FileUploadSessionPersistencePort {
    private final FileUploadSessionJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<FileUploadSession> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public Optional<FileUploadSession> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id);
    }

    @Override
    @Transactional
    public FileUploadSession save(FileUploadSession session) {
        return repository.save(session);
    }
}
