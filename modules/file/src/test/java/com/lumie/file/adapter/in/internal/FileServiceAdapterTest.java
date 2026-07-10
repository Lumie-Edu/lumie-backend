package com.lumie.file.adapter.in.internal;

import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.domain.entity.FileLink;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.repository.FileLinkRepository;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileServiceAdapter 테스트")
class FileServiceAdapterTest {

    @Mock private FileMetadataRepository fileMetadataRepository;
    @Mock private FileLinkRepository fileLinkRepository;
    @Mock private StoragePort storagePort;
    @InjectMocks private FileServiceAdapter fileServiceAdapter;

    @Test
    @DisplayName("파일 링크 교체 시 기존 링크 삭제를 flush한 뒤 다시 저장한다")
    void replaceFileLinks_flushesDeletedLinksBeforeReinsert() {
        UUID fileId = UUID.randomUUID();
        FileMetadata textbookFile = FileMetadata.create(
                EntityType.TEXTBOOK,
                null,
                "교재.pdf",
                "application/pdf",
                1024L,
                "textbooks/sample.pdf",
                null
        );
        textbookFile.markUploadCompleted();
        given(fileMetadataRepository.findById(fileId)).willReturn(Optional.of(textbookFile));

        fileServiceAdapter.replaceFileLinks("announcement", 1L, List.of(fileId));

        var inOrder = inOrder(fileLinkRepository);
        inOrder.verify(fileLinkRepository).deleteByEntityTypeAndEntityId(EntityType.ANNOUNCEMENT, 1L);
        inOrder.verify(fileLinkRepository).flush();
        inOrder.verify(fileLinkRepository).save(any(FileLink.class));
    }
}
