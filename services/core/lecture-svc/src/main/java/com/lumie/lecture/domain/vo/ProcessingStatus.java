package com.lumie.lecture.domain.vo;

public enum ProcessingStatus {
    UPLOADING,
    PENDING_PROCESSING,
    EXTRACTING_AUDIO,
    TRANSCRIBING,
    SUMMARIZING,
    COMPLETED,
    FAILED
}
