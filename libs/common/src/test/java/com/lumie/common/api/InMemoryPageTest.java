package com.lumie.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPageTest {
    @Test void pastEndReturnsEmptyPageWithOriginalTotal() {
        var page = InMemoryPage.of(List.of(1, 2, 3), PageRequest.of(2, 10));
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3);
    }
    @Test void largeOffsetDoesNotOverflow() {
        var page = InMemoryPage.of(List.of(1, 2, 3), PageRequest.of(Integer.MAX_VALUE, 2));
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3);
    }
    @Test void normalAndUnpagedContentArePreserved() {
        assertThat(InMemoryPage.of(List.of(1, 2, 3), PageRequest.of(1, 2)).getContent()).containsExactly(3);
        assertThat(InMemoryPage.of(List.of(1, 2, 3), Pageable.unpaged()).getContent()).containsExactly(1, 2, 3);
    }
}
