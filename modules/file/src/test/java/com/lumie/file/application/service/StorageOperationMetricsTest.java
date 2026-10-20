package com.lumie.file.application.service;

import com.lumie.file.application.port.out.FileStorageOperationMetricsPersistencePort;
import com.lumie.file.application.port.out.FileStorageOperationMetricsPersistencePort.Series;
import com.lumie.file.application.port.out.FileStorageOperationMetricsPersistencePort.StorageOperationMetricsSnapshot;
import com.lumie.file.domain.entity.FileStorageOperation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageOperationMetricsTest {

    @Test
    void gaugesAggregateTenantSnapshotsWithoutTenantOrObjectLabels() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        FileStorageOperationMetricsPersistencePort persistence = mock(FileStorageOperationMetricsPersistencePort.class);
        StorageOperationMetrics metrics = new StorageOperationMetrics(registry, persistence);
        metrics.initializeMeters();
        when(persistence.snapshot(org.mockito.ArgumentMatchers.any())).thenReturn(
                new StorageOperationMetricsSnapshot(
                        List.of(new Series("promote", "pending", 2L)),
                        List.of(new Series("promote", "pending", 90L)), 1L),
                new StorageOperationMetricsSnapshot(
                        List.of(new Series("delete", "retry", 3L)),
                        List.of(new Series("delete", "retry", 120L)), 2L));
        metrics.beginSnapshotRefresh();

        metrics.refreshCurrentTenantSnapshot();
        metrics.refreshCurrentTenantSnapshot();

        assertThat(registry.get("lumie_file_storage_operations")
                .tags("operation", "promote", "status", "pending").gauge().value()).isEqualTo(2d);
        assertThat(registry.get("lumie_file_storage_operations")
                .tags("operation", "delete", "status", "retry").gauge().value()).isEqualTo(3d);
        assertThat(registry.get("lumie_file_storage_oldest_operation_age_seconds")
                .tags("operation", "delete", "status", "retry").gauge().value()).isEqualTo(120d);
        assertThat(registry.get("lumie_file_storage_promotions_expiring_soon")
                .tag("window", "2h").gauge().value()).isEqualTo(3d);
        assertThat(registry.getMeters().stream()
                .flatMap(meter -> meter.getId().getTags().stream())
                .map(io.micrometer.core.instrument.Tag::getKey))
                .doesNotContain("tenant", "tenant_id", "file_id", "object_key");
    }

    @Test
    void countersUseOnlyBoundedResultAndTerminalReasonLabels() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        StorageOperationMetrics metrics = new StorageOperationMetrics(
                registry, mock(FileStorageOperationMetricsPersistencePort.class));
        FileStorageOperation operation = FileStorageOperation.delete(
                java.util.UUID.randomUUID(), "files/private/path.pdf", Instant.now());

        metrics.recordAttempt(operation, "retry");
        metrics.recordTerminalFailure(operation, "storage_error");

        assertThat(registry.counter("lumie_file_storage_operation_attempts", "operation", "delete", "result", "retry")
                .count()).isEqualTo(1d);
        assertThat(registry.counter("lumie_file_storage_terminal_failures", "operation", "delete",
                "reason", "storage_error").count()).isEqualTo(1d);
        assertThatThrownBy(() -> metrics.recordTerminalFailure(operation, "private object path"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
