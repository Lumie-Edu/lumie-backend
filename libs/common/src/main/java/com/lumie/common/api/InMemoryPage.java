package com.lumie.common.api;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public final class InMemoryPage {
    private InMemoryPage() {}

    public static <T> Page<T> of(List<T> rows, Pageable pageable) {
        if (pageable.isUnpaged()) return new PageImpl<>(rows, pageable, rows.size());
        int start = (int) Math.min(pageable.getOffset(), rows.size());
        int end = (int) Math.min((long) start + pageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(start, end), pageable, rows.size());
    }
}
