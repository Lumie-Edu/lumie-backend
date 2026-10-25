package com.lumie.file.domain.vo;

public enum UploadSessionStatus {
    ISSUED,
    UPLOADED,
    CLAIMED,
    CANCEL_REQUESTED,
    EXPIRED,
    CLEANUP_PENDING,
    CLEANED,
    CLEANUP_FAILED
}
