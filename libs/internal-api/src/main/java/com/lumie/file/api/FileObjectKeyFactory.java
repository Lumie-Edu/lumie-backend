package com.lumie.file.api;

/** Canonical object-key policy shared by file-consuming domains. */
public final class FileObjectKeyFactory {
    private FileObjectKeyFactory() {}

    public static String tenantLogo(String storageTenantId) {
        return storageTenantId + "/branding/logo";
    }

    public static String temporaryOmr(String storageTenantId, String batchId, String filename) {
        return "tmp/" + storageTenantId + "/omr/" + batchId + "/" + filename;
    }

    public static String temporaryReport(String storageTenantId, Long examId, Long studentId) {
        return "tmp/" + storageTenantId + "/reports/" + examId + "/" + studentId + ".pdf";
    }
}
