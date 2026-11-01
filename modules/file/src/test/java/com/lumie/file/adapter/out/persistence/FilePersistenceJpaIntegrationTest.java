package com.lumie.file.adapter.out.persistence;

import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileUploadSession;
import com.lumie.file.domain.entity.TextbookFolder;
import com.lumie.file.domain.vo.EntityType;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FileDownloadPersistenceAdapter.class, FileMetadataPersistenceAdapter.class,
        FileUploadSessionPersistenceAdapter.class, TextbookFolderPersistenceAdapter.class})
class FilePersistenceJpaIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private FileDownloadPersistenceAdapter downloadAdapter;
    @Autowired private FileMetadataPersistenceAdapter metadataAdapter;
    @Autowired private FileUploadSessionPersistenceAdapter sessionAdapter;
    @Autowired private TextbookFolderPersistenceAdapter folderAdapter;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearTenantContext() {
        com.lumie.common.tenant.TenantContextHolder.clear();
        com.lumie.common.tenant.UserContextHolder.clear();
    }

    @Test
    void metadataQueriesPreserveNullFolderFilterAndOrdering() {
        tenant(71L);
        FileMetadata z = metadata("z.pdf", null);
        FileMetadata a = metadata("a.pdf", null);
        FileMetadata nested = metadata("nested.pdf", UUID.randomUUID());
        metadataAdapter.save(z);
        metadataAdapter.save(a);
        metadataAdapter.save(nested);
        Instant now = Instant.now();
        jdbcTemplate.update("update file_metadata set created_at = ? where id = ?",
                Timestamp.from(now.minusSeconds(172_800)), z.getId());
        jdbcTemplate.update("update file_metadata set created_at = ? where id = ?",
                Timestamp.from(now.minusSeconds(86_400)), a.getId());
        jdbcTemplate.update("update file_metadata set created_at = ? where id = ?", Timestamp.from(now), nested.getId());

        assertThat(metadataAdapter.findByEntityTypeAndFolderId(EntityType.TEXTBOOK, null))
                .extracting(FileMetadata::getId).containsExactly(a.getId(), z.getId());
        assertThat(metadataAdapter.findByEntityType(EntityType.TEXTBOOK))
                .extracting(FileMetadata::getId).containsExactly(nested.getId(), a.getId(), z.getId());
        assertThat(metadataAdapter.findByEntityTypeAndEntityId(EntityType.TEXTBOOK, null)).hasSize(3);
        assertThat(metadataAdapter.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void downloadUpsertIsAtomicAndDownloaderRowsRemainNewestFirst() {
        tenant(72L);
        FileMetadata file = metadata("download.pdf", null);
        metadataAdapter.save(file);
        assertThat(downloadAdapter.upsertDownload(72L, file.getId(), 8L)).isEqualTo(1);
        assertThat(downloadAdapter.upsertDownload(72L, file.getId(), 8L)).isEqualTo(1);

        var downloads = downloadAdapter.findByFileIdOrderByUpdatedAtDesc(file.getId());
        assertThat(downloads).hasSize(1);
        assertThat(downloads.getFirst().getUserId()).isEqualTo(8L);
        assertThat(downloads.getFirst().getDownloadCount()).isEqualTo(2);
    }

    @Test
    void uploadSessionForUpdateAndFolderNullAwareQueriesRemainUsable() {
        tenant(73L);
        TextbookFolder z = folderAdapter.save(TextbookFolder.create("Zeta", null, 0));
        TextbookFolder a = folderAdapter.save(TextbookFolder.create("Alpha", null, 0));
        folderAdapter.save(TextbookFolder.create("Child", a.getId(), a.getDepth()));

        assertThat(folderAdapter.findByParentId(null)).extracting(TextbookFolder::getName)
                .containsExactly("Alpha", "Zeta");
        assertThat(folderAdapter.findByParentId(a.getId())).extracting(TextbookFolder::getName)
                .containsExactly("Child");
        assertThat(folderAdapter.existsByParentIdAndName(null, "Alpha")).isTrue();
        assertThat(folderAdapter.existsByParentIdAndName(null, "Missing")).isFalse();
        assertThat(folderAdapter.existsByParentId(a.getId())).isTrue();
        assertThat(folderAdapter.existsByParentId(z.getId())).isFalse();

        UUID id = UUID.randomUUID();
        FileUploadSession session = FileUploadSession.issue(id, 14L, EntityType.TEXTBOOK, "object/key",
                "book.pdf", "application/pdf", 20L, Instant.now().plusSeconds(30), Instant.now().plusSeconds(60));
        sessionAdapter.save(session);
        assertThat(sessionAdapter.findById(id)).contains(session);
        assertThat(sessionAdapter.findByIdForUpdate(id)).contains(session);
        assertThat(sessionAdapter.findById(UUID.randomUUID())).isEmpty();
    }

    private FileMetadata metadata(String name, UUID folderId) {
        return FileMetadata.create(EntityType.TEXTBOOK, null, name, "application/pdf", 10L,
                "tenant/files/" + UUID.randomUUID(), folderId);
    }

    private static void tenant(long id) {
        com.lumie.common.tenant.TenantContextHolder.setTenant("file-test-" + id);
        com.lumie.common.tenant.TenantContextHolder.setTenantId(id);
    }
}
