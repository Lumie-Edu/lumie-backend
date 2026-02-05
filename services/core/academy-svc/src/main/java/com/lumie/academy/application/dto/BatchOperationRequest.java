package com.lumie.academy.application.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchOperationRequest(
        @NotEmpty(message = "ids must not be empty")
        List<Long> ids
) {}
