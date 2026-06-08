package com.lumie.file.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "file_download", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"file_id", "user_id", "tenant_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileDownload extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "download_count", nullable = false)
    private int downloadCount = 1;

    @Builder
    private FileDownload(UUID fileId, Long userId) {
        this.fileId = fileId;
        this.userId = userId;
    }

    public static FileDownload create(UUID fileId, Long userId) {
        return FileDownload.builder()
                .fileId(fileId)
                .userId(userId)
                .build();
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }
}
