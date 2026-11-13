package com.lumie.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemporaryUploadPolicyTest {

    @Test
    void allowsSupportedExtensionsWithoutCaseSensitivity() {
        assertThat(TemporaryUploadPolicy.isAllowedFilename("Notes.PDF")).isTrue();
        assertThat(TemporaryUploadPolicy.isAllowedFilename("scan.jpeg")).isTrue();
    }

    @Test
    void rejectsMissingExtensionsAndPathTraversal() {
        assertThat(TemporaryUploadPolicy.isAllowedFilename("notes")).isFalse();
        assertThat(TemporaryUploadPolicy.isAllowedFilename("../notes.pdf")).isFalse();
        assertThat(TemporaryUploadPolicy.isAllowedFilename("folder\\notes.pdf")).isFalse();
        assertThat(TemporaryUploadPolicy.isAllowedFilename(null)).isFalse();
        assertThat(TemporaryUploadPolicy.isAllowedFilename(" ")).isFalse();
    }

    @Test
    void capsUploadsAtTenMegabytes() {
        assertThat(TemporaryUploadPolicy.MAX_BYTES).isEqualTo(10L * 1024 * 1024);
    }
}
