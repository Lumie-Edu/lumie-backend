package com.lumie.exam.domain.entity;

import com.lumie.exam.domain.vo.ReportGenerationItemStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportGenerationItemTest {

    @Test
    void onlyPendingItemCanTransitionToSuccessOrFailure() {
        ReportGenerationItem item = ReportGenerationItem.pending(10L, 20L, 2, 30L);

        assertThat(item.completeSuccessfully("tmp/report.pdf")).isTrue();
        assertThat(item.getStatus()).isEqualTo(ReportGenerationItemStatus.SUCCESS);
        assertThat(item.getFileKey()).isEqualTo("tmp/report.pdf");
        assertThat(item.completeWithFailure("late failure")).isFalse();
        assertThat(item.getStatus()).isEqualTo(ReportGenerationItemStatus.SUCCESS);
        assertThat(item.getErrorMessage()).isNull();

        ReportGenerationItem failed = ReportGenerationItem.pending(10L, 20L, 3, 31L);
        assertThat(failed.completeWithFailure("render failed")).isTrue();
        assertThat(failed.getStatus()).isEqualTo(ReportGenerationItemStatus.FAILED);
        assertThat(failed.getFileKey()).isNull();
        assertThat(failed.completeSuccessfully("tmp/report.pdf")).isFalse();
        assertThat(failed.getErrorMessage()).isEqualTo("render failed");
    }

}
