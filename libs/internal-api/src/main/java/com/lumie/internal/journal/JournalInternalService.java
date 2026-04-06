package com.lumie.internal.journal;

import java.util.List;
import java.util.Optional;

public interface JournalInternalService {

    Optional<JournalData> getJournal(String tenantSlug, Long journalId);

    JournalListData getJournalsByClass(String tenantSlug, Long classId, int page, int size);

    record JournalData(
            Long id,
            Long classId,
            Long academyId,
            Long teacherId,
            String lessonDate,
            String title,
            String content,
            String status,
            String createdAt,
            String updatedAt
    ) {}

    record JournalListData(
            List<JournalData> journals,
            long totalCount,
            int page,
            int size
    ) {}
}
