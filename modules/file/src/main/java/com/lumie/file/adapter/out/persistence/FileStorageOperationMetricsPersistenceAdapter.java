package com.lumie.file.adapter.out.persistence;

import com.lumie.file.application.port.out.FileStorageOperationMetricsPersistencePort;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FileStorageOperationMetricsPersistenceAdapter implements FileStorageOperationMetricsPersistencePort {

    private final FileStorageOperationJpaRepository operationRepository;

    @Override
    @Transactional(readOnly = true)
    public StorageOperationMetricsSnapshot snapshot(Instant now) {
        List<Series> counts = operationRepository.operationCounts().stream()
                .map(row -> new Series(row.getOperation(), row.getStatus(), row.getValue()))
                .toList();
        List<Series> ages = operationRepository.oldestOperationAges(now).stream()
                .map(row -> new Series(row.getOperation(), row.getStatus(), row.getValue()))
                .toList();
        return new StorageOperationMetricsSnapshot(
                counts, ages, operationRepository.countPromotionsExpiringSoon(now));
    }
}
