package com.lumie.homepage.domain.exception;

import com.lumie.common.exception.BusinessException;

public class HomepageConfigNotFoundException extends BusinessException {

    public HomepageConfigNotFoundException() {
        super(HomepageErrorCode.HOMEPAGE_CONFIG_NOT_FOUND);
    }
}
