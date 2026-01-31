package com.lumie.exam.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkSubmitResultsRequest(
        @NotEmpty(message = "Results list cannot be empty")
        @Valid
        List<SubmitResultRequest> results
) {
}
