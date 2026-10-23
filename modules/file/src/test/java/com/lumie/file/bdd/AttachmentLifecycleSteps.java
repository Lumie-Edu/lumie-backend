package com.lumie.file.bdd;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.exception.BusinessException;
import com.lumie.file.application.dto.response.FileDownloadResult;
import com.lumie.file.api.PreparedTemporaryFile;
import com.lumie.file.domain.entity.FileMetadata;
import com.lumie.file.domain.entity.FileStorageOperation;
import com.lumie.file.domain.vo.EntityType;
import com.lumie.file.domain.vo.FileUploadStatus;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

public class AttachmentLifecycleSteps {

    private final AttachmentLifecycleWorld world;
    private String deletionRoute;
    private boolean retainedIdRejected;
    private int tenantBProcessedCount;
    private boolean tenantBReadDenied;
    private boolean tenantBDownloadDenied;

    public AttachmentLifecycleSteps(AttachmentLifecycleWorld world) {
        this.world = world;
    }

    @Given("a complete temporary object with valid declared metadata")
    public void aCompleteTemporaryObjectWithValidDeclaredMetadata() {
        world.preparedTemporaryFile = new PreparedTemporaryFile(
                world.fileId,
                "notes.pdf",
                "application/pdf",
                10L,
                "tmp/demo/uploads/upload-1/notes.pdf",
                "files/demo/announcement/file-1/notes.pdf",
                "source-etag",
                Instant.parse("2026-10-20T00:00:00Z"));
    }

    @When("a Staff member registers the attachment")
    public void aStaffMemberRegistersTheAttachment() {
        world.registeredIds = world.fileServiceAdapter.attachPreparedFiles(
                "announcement", 9L, List.of(world.preparedTemporaryFile));
    }

    @Then("the attachment is PENDING and is not listed or downloadable")
    public void theAttachmentIsPendingAndUnavailable() {
        assertThat(world.registeredIds).containsExactly(world.fileId);
        FileMetadata metadata = world.files.getFirst();
        assertThat(metadata.getUploadStatus()).isEqualTo(FileUploadStatus.PENDING);
        assertThat(metadata.isUploadCompleted()).isFalse();
        assertThat(world.fileQueryService.getFilesByEntity(EntityType.ANNOUNCEMENT, 9L)).isEmpty();
    }

    @Given("a PENDING attachment with a valid temporary object")
    public void aPendingAttachmentWithAValidTemporaryObject() {
        aCompleteTemporaryObjectWithValidDeclaredMetadata();
        aStaffMemberRegistersTheAttachment();
    }

    @When("the permanent object is promoted and its metadata is verified")
    public void thePermanentObjectIsPromotedAndVerified() {
        assertThat(world.storageOperationProcessor.processDueOperations(10)).isEqualTo(1);
    }

    @Then("the attachment becomes READY")
    public void theAttachmentBecomesReady() {
        assertThat(world.files.getFirst().isReady()).isTrue();
    }

    @Then("the attachment is listed and downloadable")
    public void theAttachmentIsListedAndDownloadable() {
        assertThat(world.fileQueryService.getFilesByEntity(EntityType.ANNOUNCEMENT, 9L)).hasSize(1);
        FileDownloadResult downloadResult = world.fileQueryService.downloadFile(world.fileId);
        assertThat(downloadResult.inputStream()).isNotNull();
        assertThat(downloadResult.filename()).isEqualTo("notes.pdf");
    }

    @Given("a PENDING attachment whose temporary object is {word}")
    public void aPendingAttachmentWhoseTemporaryObjectIs(String condition) {
        aCompleteTemporaryObjectWithValidDeclaredMetadata();
        if ("expired".equals(condition)) {
            world.preparedTemporaryFile = new PreparedTemporaryFile(
                    world.fileId, "notes.pdf", "application/pdf", 10L,
                    "tmp/demo/uploads/upload-1/notes.pdf", "files/demo/announcement/file-1/notes.pdf",
                    "source-etag", Instant.now().minus(Duration.ofHours(23)));
        }
        world.registerPreparedAttachment();
        world.configureInvalidPromotion(condition);
    }

    @When("promotion is attempted")
    public void promotionIsAttempted() {
        world.storageOperationProcessor.processDueOperations(10);
    }

    @Then("the attachment does not become READY")
    public void theAttachmentDoesNotBecomeReady() {
        assertThat(world.files.getFirst().isReady()).isFalse();
    }

    @Then("the failure is recorded for retry or terminal handling")
    public void theFailureIsRecordedForRetryOrTerminalHandling() {
        assertThat(world.operation.get().getStatus())
                .isIn(com.lumie.file.domain.vo.FileStorageOperationStatus.RETRY,
                        com.lumie.file.domain.vo.FileStorageOperationStatus.FAILED);
        assertThat(world.operation.get().getLastErrorCode()).isNotBlank();
    }

    @Given("^a READY attachment is affected by (.+)$")
    public void aReadyAttachmentIsAffectedBy(String operation) {
        deletionRoute = operation;
        if ("textbook-folder deletion".equals(operation)) {
            UUID folderId = UUID.fromString("8f72cc89-7c4e-4f7e-9bd0-99ffdfccbb09");
            world.registerReadyAttachment(EntityType.TEXTBOOK, null, folderId);
            world.prepareTextbookFolder(folderId);
        } else {
            world.registerReadyAttachment();
        }
    }

    @When("the database change commits")
    public void theDatabaseChangeCommits() {
        switch (deletionRoute) {
            case "single-file deletion" -> world.fileCommandService.deleteFile(world.fileId);
            case "announcement replacement" -> world.fileServiceAdapter.deleteFilesByEntityAndIds(
                    "announcement", 9L, List.of(world.fileId));
            case "announcement deletion" -> world.fileServiceAdapter.deleteFilesByEntity("announcement", 9L);
            case "textbook-folder deletion" -> world.textbookFolderCommandService.deleteFolder(
                    UUID.fromString("8f72cc89-7c4e-4f7e-9bd0-99ffdfccbb09"));
            default -> throw new IllegalArgumentException("Unsupported deletion route: " + deletionRoute);
        }
    }

    @Then("the attachment is no longer readable")
    public void theAttachmentIsNoLongerReadable() {
        assertThatThrownBy(() -> world.fileQueryService.getFileMetadata(world.fileId))
                .isInstanceOf(BusinessException.class);
    }

    @Then("a durable delete intent exists for its permanent object")
    public void aDurableDeleteIntentExistsForItsPermanentObject() {
        assertThat(world.operation.get().getOperationType())
                .isEqualTo(com.lumie.file.domain.vo.FileStorageOperationType.DELETE);
        assertThat(world.operation.get().getTargetObjectKey()).isEqualTo(world.files.getFirst().getObjectKey());
    }

    @Given("a durable delete intent for an object that is already absent")
    public void aDurableDeleteIntentForAnAlreadyAbsentObject() {
        world.prepareAlreadyAbsentDelete();
    }

    @When("the delete processor handles the intent")
    public void theDeleteProcessorHandlesTheIntent() {
        assertThat(world.storageOperationProcessor.processDueOperations(10)).isEqualTo(1);
    }

    @Then("the intent reaches successful completion")
    public void theIntentReachesSuccessfulCompletion() {
        assertThat(world.operation.get().getStatus())
                .isEqualTo(com.lumie.file.domain.vo.FileStorageOperationStatus.COMPLETED);
    }

    @Given("^an announcement replacement contains a retained attachment ID that is (.+)$")
    public void anAnnouncementReplacementContainsInvalidRetainedId(String ownership) {
        world.prepareInvalidRetainedId(ownership);
    }

    @When("Staff submits the replacement")
    public void staffSubmitsTheReplacement() {
        try {
            world.fileServiceAdapter.reconcilePreparedDirectFiles(
                    "announcement", 9L, List.of(world.requestedRetainedId), List.of());
        } catch (BusinessException exception) {
            retainedIdRejected = true;
        }
    }

    @Then("the request is rejected before deleting any existing attachment")
    public void theRequestIsRejectedBeforeDeletingAnyExistingAttachment() {
        assertThat(retainedIdRejected).isTrue();
        assertThat(world.files.getFirst().isReady()).isTrue();
        assertThat(world.operation.get()).isNull();
    }

    @Then("no object outside the announcement is modified")
    public void noObjectOutsideTheAnnouncementIsModified() {
        assertThat(world.files).allSatisfy(file -> assertThat(file.isDeleting()).isFalse());
    }

    @Given("a nested folder has two READY files and MinIO fails to delete one object transiently")
    public void aNestedFolderHasTwoReadyFilesAndOneDeleteFails() {
        world.preparePartialFolderDeleteFailure();
    }

    @When("Staff deletes the folder and the delete processor handles both intents")
    public void staffDeletesFolderAndProcessorHandlesBoth() {
        world.textbookFolderCommandService.deleteFolder(
                UUID.fromString("8f72cc89-7c4e-4f7e-9bd0-99ffdfccbb09"));
        assertThat(world.storageOperationProcessor.processDueOperations(10)).isEqualTo(2);
    }

    @Then("both file records are unavailable")
    public void bothFileRecordsAreUnavailable() {
        UUID secondFileId = UUID.fromString("cc13408d-e89d-4321-813f-0febfa75ac14");
        assertThatThrownBy(() -> world.fileQueryService.getFileMetadata(world.fileId))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> world.fileQueryService.getFileMetadata(secondFileId))
                .isInstanceOf(BusinessException.class);
    }

    @Then("the successful object deletion completes independently")
    public void theSuccessfulObjectDeletionCompletesIndependently() {
        assertThat(world.storageOperations.stream().filter(operation -> operation.getFileId().equals(world.fileId))
                .findFirst().orElseThrow().getStatus())
                .isEqualTo(com.lumie.file.domain.vo.FileStorageOperationStatus.COMPLETED);
    }

    @Then("the failed object's delete intent remains durable and retryable")
    public void theFailedObjectsIntentRemainsRetryable() {
        UUID secondFileId = UUID.fromString("cc13408d-e89d-4321-813f-0febfa75ac14");
        assertThat(world.storageOperations.stream().filter(operation -> operation.getFileId().equals(secondFileId))
                .findFirst().orElseThrow().getStatus())
                .isEqualTo(com.lumie.file.domain.vo.FileStorageOperationStatus.RETRY);
    }

    @Given("a PENDING attachment nearing its 24-hour temporary-object expiry")
    public void aPendingAttachmentNearTemporaryExpiry() {
        aCompleteTemporaryObjectWithValidDeclaredMetadata();
        world.preparedTemporaryFile = new PreparedTemporaryFile(
                world.fileId, "notes.pdf", "application/pdf", 10L,
                "tmp/demo/uploads/upload-1/notes.pdf", "files/demo/announcement/file-1/notes.pdf",
                "source-etag", Instant.now().minus(Duration.ofHours(23)));
        world.registerPreparedAttachment();
    }

    @When("promotion has not succeeded by its bounded retry deadline")
    public void promotionHasReachedItsRetryDeadline() {
        assertThat(world.storageOperationProcessor.processDueOperations(10)).isEqualTo(1);
    }

    @Then("an actionable terminal failure alert is emitted")
    public void actionableTerminalFailureAlertIsEmitted() {
        assertThat(world.metricsRegistry.counter("lumie_file_storage_terminal_failures",
                "operation", "promote", "reason", "retry_deadline").count()).isEqualTo(1d);
    }

    @Given("a promotion operation completed and a later delete made its attachment unavailable")
    public void aCompletedPromotionWasLaterDeleted() {
        world.preparePromotionCompletedBeforeDelete();
    }

    @When("the processor receives the completed promotion operation again")
    public void theProcessorReceivesCompletedPromotionAgain() {
        FileStorageOperation promotion = world.storageOperations.stream()
                .filter(item -> item.getOperationType() == com.lumie.file.domain.vo.FileStorageOperationType.PROMOTE)
                .findFirst().orElseThrow();
        world.operationPersistencePort.finalizePromotion(promotion.getId(), promotion.getFileId(), "worker-a");
    }

    @Then("the attachment remains unavailable")
    public void theAttachmentRemainsUnavailable() {
        assertThatThrownBy(() -> world.fileQueryService.getFileMetadata(world.fileId))
                .isInstanceOf(BusinessException.class);
    }

    @Then("the durable delete intent remains processable until the permanent object is absent")
    public void theDeleteIntentRemainsProcessable() {
        assertThat(world.storageOperations).anySatisfy(item -> {
            assertThat(item.getOperationType()).isEqualTo(com.lumie.file.domain.vo.FileStorageOperationType.DELETE);
            assertThat(item.getStatus()).isIn(com.lumie.file.domain.vo.FileStorageOperationStatus.PENDING,
                    com.lumie.file.domain.vo.FileStorageOperationStatus.RETRY);
        });
    }

    @Given("a PENDING attachment and its durable storage operation belong to Tenant A")
    public void aPendingAttachmentAndOperationBelongToTenantA() {
        world.prepareTenantPromotion();
    }

    @When("a Staff member from Tenant B requests the attachment and a Tenant B processor claims due work")
    public void tenantBRequestsAndClaimsTenantAWork() {
        TenantContextHolder.setTenant("inst-other");
        TenantContextHolder.setTenantId(99L);
        try {
            world.fileQueryService.getFileMetadata(world.fileId);
        } catch (BusinessException exception) {
            tenantBReadDenied = true;
        }
        try {
            world.fileQueryService.downloadFile(world.fileId);
        } catch (BusinessException exception) {
            tenantBDownloadDenied = true;
        }
        tenantBProcessedCount = world.storageOperationProcessor.processDueOperations(10);
    }

    @Then("the Staff member cannot read or download Tenant A's attachment")
    public void tenantBCannotReadTenantAFile() {
        assertThat(tenantBReadDenied).isTrue();
        assertThat(tenantBDownloadDenied).isTrue();
    }

    @Then("the processor cannot observe or claim Tenant A's operation")
    public void tenantBProcessorCannotClaimTenantAOperation() {
        assertThat(tenantBProcessedCount).isZero();
    }

    @Then("no storage action is performed on Tenant A's object key")
    public void noTenantAStorageActionUnderTenantBContext() {
        verifyNoInteractions(world.storagePort);
    }

    @When("a Tenant A processor claims due work")
    public void tenantAProcessorClaimsDueWork() {
        TenantContextHolder.setTenant("inst-demo");
        TenantContextHolder.setTenantId(42L);
        world.storageOperationProcessor.processDueOperations(10);
    }

    @Then("it can process only Tenant A's operation using Tenant A's context")
    public void tenantAProcessorHandlesOwnOperation() {
        assertThat(world.files.getFirst().isReady()).isTrue();
        assertThat(world.operation.get().getStatus())
                .isEqualTo(com.lumie.file.domain.vo.FileStorageOperationStatus.COMPLETED);
    }

    @Given("a storage operation remains pending or retrying for more than five minutes")
    public void aStorageOperationHasStaleBacklogAge() {
        world.prepareStaleMetricsSnapshot();
    }

    @When("the monitoring rule evaluates the operation metrics")
    public void theMonitoringRuleEvaluatesOperationMetrics() {
        assertThat(world.metricsRegistry.get("lumie_file_storage_oldest_operation_age_seconds")
                .tags("operation", "delete", "status", "retry").gauge().value()).isGreaterThan(300d);
    }

    @Then("the stale-backlog warning fires")
    public void theStaleBacklogWarningFires() {
        assertThat(world.metricsRegistry.get("lumie_file_storage_oldest_operation_age_seconds")
                .tags("operation", "delete", "status", "retry").gauge().value()).isGreaterThan(300d);
    }

    @Then("the operation can be identified from its log details without tenant or object labels on metrics")
    public void operationIsIdentifiableWithoutHighCardinalityMetricLabels() {
        assertThat(world.operation.get().getLastErrorCode()).isEqualTo("STORAGE_ERROR");
        assertThat(world.metricsRegistry.get("lumie_file_storage_oldest_operation_age_seconds")
                .tags("operation", "delete", "status", "retry").gauge().getId().getTags())
                .extracting(io.micrometer.core.instrument.Tag::getKey)
                .doesNotContain("tenant", "tenant_id", "file_id", "object_key");
    }

    @Given("a delete operation has reached its retry deadline after transient storage failures")
    public void aDeleteOperationHasReachedItsRetryDeadline() {
        world.prepareTerminalDeleteFailure();
    }

    @When("the processor marks the operation terminally failed")
    public void theProcessorMarksOperationTerminallyFailed() {
        assertThat(world.storageOperationProcessor.processDueOperations(10)).isEqualTo(1);
    }

    @Then("the durable delete intent and last failure remain auditable")
    public void theDeleteIntentAndFailureRemainAuditable() {
        assertThat(world.operation.get().getStatus())
                .isEqualTo(com.lumie.file.domain.vo.FileStorageOperationStatus.FAILED);
        assertThat(world.operation.get().getLastErrorCode()).isNotBlank();
        assertThat(world.operation.get().getLastErrorMessage()).contains("deadline");
    }

    @Then("the terminal-deletion alert signal is emitted")
    public void terminalDeletionAlertSignalIsEmitted() {
        assertThat(world.metricsRegistry.counter("lumie_file_storage_terminal_failures",
                "operation", "delete", "reason", "retry_deadline").count()).isEqualTo(1d);
    }

    @After
    public void clearTenantContext() {
        TenantContextHolder.clear();
        com.lumie.common.tenant.UserContextHolder.clear();
    }
}
