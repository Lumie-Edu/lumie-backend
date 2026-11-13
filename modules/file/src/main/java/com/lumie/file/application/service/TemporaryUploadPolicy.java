package com.lumie.file.application.service;

import java.util.Locale;
import java.util.Set;

public final class TemporaryUploadPolicy {
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final Set<String> EXTENSIONS = Set.of(
            "pdf", "hwp", "hwpx", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "jpg", "jpeg", "png", "webp", "gif", "zip");

    private TemporaryUploadPolicy() {}

    public static boolean isAllowedFilename(String value) {
        if (value == null || value.isBlank()) return false;
        int dot = value.lastIndexOf('.');
        return dot > 0 && !value.contains("/") && !value.contains("\\") && !value.contains("..")
                && EXTENSIONS.contains(value.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
