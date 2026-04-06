package com.lumie.journal.domain.exception;

import com.lumie.common.exception.BusinessException;

public class JournalNotFoundException extends BusinessException {

    public JournalNotFoundException(Long id) {
        super(JournalErrorCode.JOURNAL_NOT_FOUND, "Journal not found with id: " + id);
    }
}
