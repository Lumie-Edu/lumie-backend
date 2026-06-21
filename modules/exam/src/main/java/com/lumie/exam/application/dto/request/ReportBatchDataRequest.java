package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReportBatchDataRequest(
        @NotEmpty
        @Size(max = 30)
        List<@NotNull Long> studentIds
) {
}
