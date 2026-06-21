package com.lumie.exam.application.service;

public final class ReportFileFormat {

    public static final String PDF_EXTENSION = ".pdf";
    public static final String PDF_CONTENT_TYPE = "application/pdf";
    public static final String LEGACY_JPG_EXTENSION = ".jpg";

    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

    private ReportFileFormat() {
    }

    public static boolean isPdf(byte[] bytes) {
        if (bytes == null || bytes.length < PDF_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PDF_SIGNATURE.length; i++) {
            if (bytes[i] != PDF_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }
}
