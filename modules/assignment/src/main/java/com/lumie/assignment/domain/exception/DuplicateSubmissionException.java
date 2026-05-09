package com.lumie.assignment.domain.exception;

import com.lumie.common.exception.BusinessException;

public class DuplicateSubmissionException extends BusinessException {

    public DuplicateSubmissionException(Long assignmentId, Long studentId) {
        super(AssignmentErrorCode.DUPLICATE_SUBMISSION,
              "Student " + studentId + " has already submitted for assignment " + assignmentId);
    }
}
