package com.lumie.lecture.domain.exception;

import com.lumie.common.exception.BusinessException;

public class LectureNotFoundException extends BusinessException {

    public LectureNotFoundException() {
        super(LectureErrorCode.LECTURE_NOT_FOUND);
    }
}
