package com.lumie.file.domain.vo;

import com.lumie.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FilePathTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void toObjectKey_includesFileIdToAvoidFilenameCollisions() {
        TenantContextHolder.setTenant("joossameng");
        UUID firstFileId = UUID.fromString("10e28afa-87c1-47ee-847d-8940d85058ff");
        UUID secondFileId = UUID.fromString("d8c424dc-05e2-4e88-bebb-18e86e631f02");

        String firstObjectKey = FilePath.of(EntityType.TEXTBOOK, firstFileId, "exam_review.pdf").toObjectKey();
        String secondObjectKey = FilePath.of(EntityType.TEXTBOOK, secondFileId, "exam_review.pdf").toObjectKey();

        assertThat(firstObjectKey)
                .isEqualTo("joossameng/textbook/10e28afa-87c1-47ee-847d-8940d85058ff/exam_review.pdf");
        assertThat(secondObjectKey)
                .isEqualTo("joossameng/textbook/d8c424dc-05e2-4e88-bebb-18e86e631f02/exam_review.pdf");
        assertThat(firstObjectKey).isNotEqualTo(secondObjectKey);
    }
}
