package com.lumie.lecture.domain.vo;

public enum ProcessingStatus {
    PENDING_PROCESSING,
    EXTRACTING_AUDIO,
    TRANSCRIBING,
    SUMMARIZING,
    COMPLETED,
    FAILED
}
