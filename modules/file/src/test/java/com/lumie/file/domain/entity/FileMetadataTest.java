package com.lumie.file.domain.entity;

import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileUploadStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileMetadataTest {

    @Test
    void newMetadataIsPendingAndPreservesOnlyAValidFilenameExtension() {
        FileMetadata withExtension = FileMetadata.create(
                EntityType.TEXTBOOK, null, "lesson.pdf", "application/pdf", 10L, "files/key", UUID.randomUUID());
        FileMetadata withoutExtension = FileMetadata.create(
                EntityType.TEXTBOOK, null, "lesson", "text/plain", 10L, "files/key", null);

        assertThat(withExtension.getUploadStatus()).isEqualTo(FileUploadStatus.PENDING);
        assertThat(withExtension.isUploadCompleted()).isFalse();
        assertThat(withExtension.getStoredFilename()).endsWith(".pdf");
        assertThat(withoutExtension.getStoredFilename()).doesNotContain(".");
    }

    @Test
    void readyFailedAndDeletingTransitionsKeepTheLegacyBooleanInSync() {
        FileMetadata metadata = FileMetadata.createWithId(UUID.randomUUID(), EntityType.ANNOUNCEMENT, 5L,
                "notice.pdf", "application/pdf", 10L, "files/key", null);

        metadata.markReady();
        assertThat(metadata.isReady()).isTrue();
        assertThat(metadata.isDeleting()).isFalse();

        metadata.markFailed();
        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.FAILED);
        assertThat(metadata.isUploadCompleted()).isFalse();
        assertThat(metadata.isReady()).isFalse();

        metadata.markDeleting();
        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.DELETING);
        assertThat(metadata.isDeleting()).isTrue();
        assertThat(metadata.isUploadCompleted()).isFalse();
    }

    @Test
    void tombstoneCanBeDetachedFromADeletedTextbookFolder() {
        UUID folderId = UUID.randomUUID();
        FileMetadata metadata = FileMetadata.createWithId(UUID.randomUUID(), EntityType.TEXTBOOK, null,
                "lesson.pdf", "application/pdf", 10L, "files/key", folderId);
        metadata.markDeleting();

        metadata.clearFolder();

        assertThat(metadata.getFolderId()).isNull();
        assertThat(metadata.isDeleting()).isTrue();
    }
}
