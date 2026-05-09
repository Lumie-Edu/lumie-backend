package com.lumie.assignment.domain.exception;

import com.lumie.common.exception.BusinessException;

public class SubmissionNotFoundException extends BusinessException {

    public SubmissionNotFoundException(Long id) {
        super(AssignmentErrorCode.SUBMISSION_NOT_FOUND, "Submission not found with id: " + id);
    }
}
