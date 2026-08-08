package com.lumie.exam.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OmrSubmission source order")
class OmrSubmissionTest {

    @Test
    @DisplayName("same batch에서는 imageIndex가 큰 sheet가 최신이다")
    void laterImageInSameBatchIsNewer() {
        Instant acceptedAt = Instant.parse("2026-07-25T00:00:00Z");
        OmrSubmission first = OmrSubmission.pending(null, 10L, 0, "tmp/first", acceptedAt);
        OmrSubmission later = OmrSubmission.pending(null, 10L, 1, "tmp/later", acceptedAt);

        assertThat(later.isNewerThan(first)).isTrue();
        assertThat(first.isNewerThan(later)).isFalse();
    }

    @Test
    @DisplayName("later batch는 imageIndex와 무관하게 이전 batch보다 최신이다")
    void laterBatchIsNewerRegardlessOfImageIndex() {
        OmrSubmission earlierBatch = OmrSubmission.pending(
                null, 11L, 9, "tmp/earlier", Instant.parse("2026-07-25T00:00:00Z"));
        OmrSubmission laterBatch = OmrSubmission.pending(
                null, 12L, 0, "tmp/later", Instant.parse("2026-07-25T00:01:00Z"));

        assertThat(laterBatch.isNewerThan(earlierBatch)).isTrue();
        assertThat(earlierBatch.isNewerThan(laterBatch)).isFalse();
    }

    @Test
    @DisplayName("원장님 전화번호 보정은 원본 OCR 번호를 덮어쓰지 않는다")
    void correctPhonePreservesOriginalOcrPhone() {
        OmrSubmission submission = OmrSubmission.pending(
                null, 13L, 0, "tmp/source", Instant.parse("2026-07-25T00:00:00Z"));
        submission.complete("01011112222", 100, 1, "permanent/source", List.of());

        submission.correctPhone("01033334444");

        assertThat(submission.getOcrPhoneNumber()).isEqualTo("01011112222");
        assertThat(submission.getReconciliationPhoneNumber()).isEqualTo("01033334444");
        assertThat(submission.getEffectivePhoneNumber()).isEqualTo("01033334444");
    }
}
