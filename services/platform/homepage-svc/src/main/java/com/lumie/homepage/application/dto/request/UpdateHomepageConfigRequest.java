package com.lumie.homepage.application.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.lumie.homepage.domain.vo.TemplateId;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateHomepageConfigRequest(

        @NotNull(message = "templateId is required")
        TemplateId templateId,

        @Min(value = 1, message = "templateVersion must be positive")
        int templateVersion,

        @NotNull(message = "sections is required")
        JsonNode sections
) {
}
