package com.lumie.file.domain.repository;

import com.lumie.file.domain.entity.FileDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FileDownloadRepository extends JpaRepository<FileDownload, Long> {
    @Modifying
    @Query(value = """
            insert into file_download (tenant_id, file_id, user_id, download_count, created_at, updated_at)
            values (:tenantId, :fileId, :userId, 1, current_timestamp, current_timestamp)
            on conflict (tenant_id, file_id, user_id)
            do update set download_count = file_download.download_count + 1,
                          updated_at = current_timestamp
            """, nativeQuery = true)
    int upsertDownload(@Param("tenantId") Long tenantId,
                       @Param("fileId") UUID fileId,
                       @Param("userId") Long userId);

    List<FileDownload> findByFileIdOrderByUpdatedAtDesc(UUID fileId);
}
