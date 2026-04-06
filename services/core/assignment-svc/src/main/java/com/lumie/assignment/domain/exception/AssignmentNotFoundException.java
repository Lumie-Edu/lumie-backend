package com.lumie.assignment.domain.exception;

import com.lumie.common.exception.BusinessException;

public class AssignmentNotFoundException extends BusinessException {

    public AssignmentNotFoundException(Long id) {
        super(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND, "Assignment not found with id: " + id);
    }
}
