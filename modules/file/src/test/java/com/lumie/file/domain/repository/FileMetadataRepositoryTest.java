package com.lumie.file.domain.repository;

import com.lumie.file.domain.vo.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileMetadataRepositoryTest {

    @Test
    void ordersFolderScopedFilesByNewestCreationTime() throws NoSuchMethodException {
        Query query = FileMetadataRepository.class
                .getMethod("findByEntityTypeAndFolderId", EntityType.class, UUID.class)
                .getAnnotation(Query.class);

        assertThat(query.value()).contains("ORDER BY f.createdAt DESC, f.originalFilename ASC, f.id DESC");
    }

    @Test
    void ordersAllFilesByNewestCreationTime() throws NoSuchMethodException {
        Query query = FileMetadataRepository.class
                .getMethod("findByEntityType", EntityType.class)
                .getAnnotation(Query.class);

        assertThat(query.value()).contains("ORDER BY f.createdAt DESC, f.originalFilename ASC, f.id DESC");
    }
}
