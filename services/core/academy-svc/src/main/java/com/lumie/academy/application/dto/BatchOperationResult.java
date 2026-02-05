package com.lumie.academy.application.dto;

import java.util.List;

public record BatchOperationResult(
        int total,
        int success,
        int failed,
        List<FailedItem> failures
) {
    public record FailedItem(Long id, String reason) {}

    public static BatchOperationResult success(int count) {
        return new BatchOperationResult(count, count, 0, List.of());
    }

    public static BatchOperationResult partial(int total, int success, List<FailedItem> failures) {
        return new BatchOperationResult(total, success, failures.size(), failures);
    }
}
