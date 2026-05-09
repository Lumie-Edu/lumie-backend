package com.lumie.homepage.domain.exception;

import com.lumie.common.exception.BusinessException;
import com.lumie.homepage.infrastructure.exception.HomepageErrorCode;

public class HomepageConfigNotFoundException extends BusinessException {

    public HomepageConfigNotFoundException() {
        super(HomepageErrorCode.HOMEPAGE_CONFIG_NOT_FOUND);
    }
}
