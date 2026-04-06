package com.lumie.journal.adapter.internal;

import com.lumie.journal.application.dto.JournalResponse;
import com.lumie.journal.application.service.JournalQueryService;
import com.lumie.journal.domain.exception.JournalNotFoundException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.journal.JournalInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JournalInternalServiceAdapter implements JournalInternalService {

    private final JournalQueryService journalQueryService;

    @Override
    public Optional<JournalData> getJournal(String tenantSlug, Long journalId) {
        String previous = TenantContextHolder.getTenant();
        try {
            TenantContextHolder.setTenant(tenantSlug);
            JournalResponse response = journalQueryService.getJournal(journalId);
            return Optional.of(toJournalData(response));
        } catch (JournalNotFoundException e) {
            return Optional.empty();
        } finally {
            restoreTenant(previous);
        }
    }

    @Override
    public JournalListData getJournalsByClass(String tenantSlug, Long classId, int page, int size) {
        String previous = TenantContextHolder.getTenant();
        try {
            TenantContextHolder.setTenant(tenantSlug);
            Page<JournalResponse> journals = journalQueryService.getJournalsByClass(classId,
                    PageRequest.of(page, size));
            return new JournalListData(
                    journals.getContent().stream().map(this::toJournalData).toList(),
                    journals.getTotalElements(), page, size
            );
        } finally {
            restoreTenant(previous);
        }
    }

    private static void restoreTenant(String previous) {
        if (previous != null) {
            TenantContextHolder.setTenant(previous);
        } else {
            TenantContextHolder.clear();
        }
    }

    private JournalData toJournalData(JournalResponse response) {
        return new JournalData(
                response.id(),
                response.classId(),
                response.academyId(),
                response.teacherId(),
                response.lessonDate() != null ? response.lessonDate().toString() : "",
                response.title(),
                response.content(),
                response.status() != null ? response.status().name() : "",
                response.createdAt() != null ? response.createdAt().toString() : "",
                response.updatedAt() != null ? response.updatedAt().toString() : ""
        );
    }
}
