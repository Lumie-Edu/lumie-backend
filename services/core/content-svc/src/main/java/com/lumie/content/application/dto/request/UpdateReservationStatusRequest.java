package com.lumie.content.application.dto.request;

import com.lumie.content.domain.vo.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReservationStatusRequest(
        @NotNull(message = "Status is required")
        ReservationStatus status,

        String cancelReason
) {
}
