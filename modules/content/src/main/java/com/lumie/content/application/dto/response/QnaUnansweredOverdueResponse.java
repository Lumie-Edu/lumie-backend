package com.lumie.content.application.dto.response;

public record QnaUnansweredOverdueResponse(
        long unansweredOverdueCount,
        long thresholdHours
) {}
