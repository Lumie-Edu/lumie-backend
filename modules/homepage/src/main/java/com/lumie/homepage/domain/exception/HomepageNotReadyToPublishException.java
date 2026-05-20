package com.lumie.homepage.domain.exception;

import com.lumie.common.exception.BusinessException;

public class HomepageNotReadyToPublishException extends BusinessException {

    public HomepageNotReadyToPublishException() {
        super(HomepageErrorCode.NOT_READY_TO_PUBLISH);
    }
}
