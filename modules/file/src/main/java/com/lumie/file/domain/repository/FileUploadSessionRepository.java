package com.lumie.file.domain.repository;

import com.lumie.file.domain.entity.FileUploadSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileUploadSessionRepository extends JpaRepository<FileUploadSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FileUploadSession s where s.id = :id")
    Optional<FileUploadSession> findByIdForUpdate(@Param("id") UUID id);
}
