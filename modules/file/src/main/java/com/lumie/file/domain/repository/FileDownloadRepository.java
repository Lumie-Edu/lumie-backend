package com.lumie.file.domain.repository;

import com.lumie.file.domain.entity.FileDownload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileDownloadRepository extends JpaRepository<FileDownload, Long> {
    Optional<FileDownload> findByFileIdAndUserId(UUID fileId, Long userId);
    List<FileDownload> findByFileId(UUID fileId);
}
