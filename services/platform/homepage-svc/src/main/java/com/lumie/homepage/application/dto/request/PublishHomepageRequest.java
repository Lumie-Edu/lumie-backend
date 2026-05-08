package com.lumie.homepage.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record PublishHomepageRequest(

        @NotNull(message = "published is required")
        Boolean published
) {
}
