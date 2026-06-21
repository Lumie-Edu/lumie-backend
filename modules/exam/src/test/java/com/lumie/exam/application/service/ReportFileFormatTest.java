package com.lumie.exam.application.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ReportFileFormatTest {

    @Test
    void isPdf_acceptsPdfSignature() {
        assertThat(ReportFileFormat.isPdf("%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII)))
                .isTrue();
    }

    @Test
    void isPdf_rejectsNonPdfBytes() {
        assertThat(ReportFileFormat.isPdf(new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00}))
                .isFalse();
        assertThat(ReportFileFormat.isPdf(null))
                .isFalse();
    }
}
