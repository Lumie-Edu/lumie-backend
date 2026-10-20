package com.lumie.file.application.port.out;

import java.time.Instant;
import java.util.List;

/** Tenant-scoped low-cardinality operation aggregates for metrics collection. */
public interface FileStorageOperationMetricsPersistencePort {

    StorageOperationMetricsSnapshot snapshot(Instant now);

    record StorageOperationMetricsSnapshot(
            List<Series> operationCounts,
            List<Series> oldestOperationAges,
            long promotionsExpiringSoon
    ) { }

    record Series(String operation, String status, long value) { }
}
