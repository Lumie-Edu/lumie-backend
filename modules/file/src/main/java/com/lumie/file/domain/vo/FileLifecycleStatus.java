package com.lumie.file.domain.vo;

/** Lifecycle of a file object owned by the file module. */
public enum FileLifecycleStatus {
    TEMPORARY,
    READY_TO_ATTACH,
    ACTIVE,
    DELETING
}
