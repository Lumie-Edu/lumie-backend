package com.lumie.exam.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReportBatchRequest(@NotEmpty List<@NotNull Long> studentIds) {
}
