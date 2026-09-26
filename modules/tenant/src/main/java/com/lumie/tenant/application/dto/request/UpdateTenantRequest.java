package com.lumie.tenant.application.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record UpdateTenantRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 200, message = "Display name must be at most 200 characters")
        String displayName,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,

        @ArraySchema(schema = @Schema(allowableValues = {
                "ai",
                "students", "employees",
                "classes", "assignments", "lectures",
                "grades", "omr-grading", "reports",
                "announcements", "sms", "resources", "qna",
                "textbooks", "reviews"
        }))
        @Size(max = 100, message = "Hidden sidebar item ids must contain at most 100 entries")
        List<@Size(max = 80, message = "Sidebar item id must be at most 80 characters")
        @Pattern(
                regexp = "^[a-z0-9][a-z0-9-]*$",
                message = "Sidebar item id must be lowercase alphanumeric or hyphen"
        ) String> hiddenSidebarItemIds
) {
}
