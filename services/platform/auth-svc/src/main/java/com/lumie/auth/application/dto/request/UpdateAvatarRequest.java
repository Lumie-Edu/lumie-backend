package com.lumie.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAvatarRequest(
        @NotBlank(message = "avatarSeed must not be blank")
        @Size(max = 50, message = "avatarSeed must be 50 characters or fewer")
        String avatarSeed
) {
}
