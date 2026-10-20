package com.lumie.app;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.file.adapter.out.persistence.FileStorageOperationJpaRepository;
import com.lumie.file.api.FileObjectKeyFactory;
import com.lumie.file.api.TemporaryAttachment;
import com.lumie.file.application.port.out.StoragePort;
import com.lumie.file.application.service.FileCommandService;
import com.lumie.file.application.service.FileQueryService;
import com.lumie.file.adapter.in.internal.FileServiceAdapter;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.repository.FileMetadataRepository;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileStorageOperationStatus;
import com.lumie.file.domain.vo.FileUploadStatus;
import com.lumie.file.application.service.StorageOperationProcessor;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class FileStorageLifecycleIntegrationTest extends FullContextTestBase {

    private static final String BUCKET = "lumie-files";
    private static final String TENANT_SLUG = "inst-file-smoke";
    private static final long TENANT_ID = 847_213L;
    private static final long ANNOUNCEMENT_ID = 39_117L;

    @Container
    static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName.parse("zot.lumie-infra.com/minio/minio:RELEASE.2025-01-20T14-49-07Z"
                    + "@sha256:ed9be66eb5f2636c18289c34c3b725ddf57815f2777c77b5938543b78a44f144")
                    .asCompatibleSubstituteFor("minio/minio"));

    static {
        MINIO.start();
        System.setProperty("lumie.test.minio.endpoint", MINIO.getS3URL());
        System.setProperty("lumie.test.minio.external-endpoint", MINIO.getS3URL());
        System.setProperty("lumie.test.minio.access-key", MINIO.getUserName());
        System.setProperty("lumie.test.minio.secret-key", MINIO.getPassword());
    }

    @Autowired private MinioClient minioClient;
    @Autowired private StoragePort storagePort;
    @Autowired private FileServiceAdapter fileServiceAdapter;
    @Autowired private FileCommandService fileCommandService;
    @Autowired private FileQueryService fileQueryService;
    @Autowired private FileMetadataRepository fileMetadataRepository;
    @Autowired private FileStorageOperationJpaRepository operationRepository;
    @Autowired private StorageOperationProcessor processor;

    @BeforeAll
    static void migrateDatabase() throws Exception {
        createRequiredRole("lumie_app");
        createRequiredRole("postgres");
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/public")
                .baselineOnMigrate(true)
                .baselineVersion("0");
        configuration.getConfigurationExtension(PostgreSQLConfigurationExtension.class).setTransactionalLock(false);
        Flyway flyway = configuration.load();
        assertThat(flyway.migrate().success).isTrue();
    }

    @org.junit.jupiter.api.AfterAll
    static void clearMinioProperties() {
        System.clearProperty("lumie.test.minio.endpoint");
        System.clearProperty("lumie.test.minio.external-endpoint");
        System.clearProperty("lumie.test.minio.access-key");
        System.clearProperty("lumie.test.minio.secret-key");
    }

    @BeforeEach
    void setTenantAndBucket() throws Exception {
        TenantContextHolder.setTenant(TENANT_SLUG);
        TenantContextHolder.setTenantId(TENANT_ID);
        UserContextHolder.setUserRole(Role.OWNER);
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        }
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    @Test
    void temporaryUploadPromotesToReadyThenDeletesThroughTheDurableOutbox() {
        UUID uploadId = UUID.randomUUID();
        String sourceKey = FileObjectKeyFactory.temporaryUpload("file-smoke", uploadId, "notes.pdf");
        byte[] body = "file lifecycle smoke".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        storagePort.uploadObject(sourceKey, new ByteArrayInputStream(body), body.length, "application/pdf");

        List<com.lumie.file.api.PreparedTemporaryFile> prepared = fileServiceAdapter.prepareTemporaryFiles(
                "announcement", List.of(new TemporaryAttachment(sourceKey, "notes.pdf", "application/pdf", body.length)));
        UUID fileId = fileServiceAdapter.attachPreparedFiles("announcement", ANNOUNCEMENT_ID, prepared).getFirst();

        FileMetadata pending = fileMetadataRepository.findById(fileId).orElseThrow();
        assertThat(pending.getUploadStatus()).isEqualTo(FileUploadStatus.PENDING);
        assertThat(pending.isUploadCompleted()).isFalse();
        assertThat(fileQueryService.getFilesByEntity(EntityType.ANNOUNCEMENT, ANNOUNCEMENT_ID)).isEmpty();
        assertThat(operationRepository.findAll()).anySatisfy(operation -> {
            assertThat(operation.getFileId()).isEqualTo(fileId);
            assertThat(operation.getStatus()).isEqualTo(FileStorageOperationStatus.PENDING);
        });

        assertThat(processor.processDueOperations(10)).isEqualTo(1);
        FileMetadata ready = fileMetadataRepository.findById(fileId).orElseThrow();
        assertThat(ready.isReady()).isTrue();
        assertThat(fileQueryService.getFilesByEntity(EntityType.ANNOUNCEMENT, ANNOUNCEMENT_ID))
                .extracting(com.lumie.file.application.dto.response.FileMetadataResponse::id)
                .containsExactly(fileId);

        String durableKey = ready.getObjectKey();
        fileCommandService.deleteFile(fileId);
        assertThat(fileMetadataRepository.findById(fileId).orElseThrow().isDeleting()).isTrue();
        assertThat(processor.processDueOperations(10)).isEqualTo(1);
        assertThat(storagePort.objectExists(durableKey)).isFalse();
        assertThat(fileMetadataRepository.findById(fileId)).isEmpty();
        assertThat(operationRepository.findAll()).filteredOn(op -> fileId.equals(op.getFileId()))
                .extracting(FileStorageOperation::getStatus)
                .containsOnly(FileStorageOperationStatus.COMPLETED);
    }

    private static void createRequiredRole(String role) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + role
                    + "') THEN CREATE ROLE " + role + " NOLOGIN NOSUPERUSER NOBYPASSRLS; END IF; END $$");
        }
    }
}
