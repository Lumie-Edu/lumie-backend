package com.lumie.file.api;

/** Canonical object-key policy shared by file-consuming domains. */
public final class FileObjectKeyFactory {
    private FileObjectKeyFactory() {}

    public static String tenantLogo(String storageTenantId) {
        return storageTenantId + "/branding/logo";
    }

    public static String temporaryOmr(String storageTenantId, String batchId, String filename) {
        return temporaryOmrPrefix(storageTenantId) + batchId + "/" + filename;
    }

    public static String temporaryReport(String storageTenantId, Long examId, Long studentId) {
        return "tmp/" + storageTenantId + "/reports/" + examId + "/" + studentId + ".pdf";
    }
    public static String temporaryUploadPrefix(String storageTenantId) {
        return "tmp/" + storageTenantId + "/uploads/";
    }

    public static String temporaryUpload(String storageTenantId, java.util.UUID fileId, String filename) {
        return temporaryUploadPrefix(storageTenantId) + fileId + "/" + filename;
    }

    public static boolean isTemporaryUploadKey(String storageTenantId, String objectKey, String filename) {
        if (objectKey == null || filename == null) return false;
        String prefix = temporaryUploadPrefix(storageTenantId);
        if (!objectKey.startsWith(prefix)) return false;
        String remainder = objectKey.substring(prefix.length());
        int separator = remainder.indexOf('/');
        if (separator < 0) return false;
        String id = remainder.substring(0, separator);
        try {
            java.util.UUID parsed = java.util.UUID.fromString(id);
            return parsed.toString().equals(id) && remainder.substring(separator + 1).equals(filename);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static String file(String storageTenantId, String entityPath, java.util.UUID fileId, String filename) {
        return storageTenantId + "/" + entityPath + "/" + fileId + "/" + filename;
    }

    public static String temporaryOmrPrefix(String storageTenantId) {
        return "tmp/" + storageTenantId + "/omr/";
    }

    public static String omrSubmission(String storageTenantId, Long submissionId) {
        return storageTenantId + "/omr/submissions/" + submissionId + ".png";
    }

    public static String reportArchive(String storageTenantId, Long examId, Long jobId) {
        return "tmp/" + storageTenantId + "/reports/" + examId + "/batch_" + jobId + ".zip";
    }

    public static String legacyReport(String storageTenantId, Long examId, Long studentId) {
        return "tmp/" + storageTenantId + "/reports/" + examId + "/" + studentId + ".jpg";
    }
}
