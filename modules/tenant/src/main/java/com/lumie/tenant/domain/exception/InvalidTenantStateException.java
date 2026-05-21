package com.lumie.tenant.domain.exception;

import com.lumie.common.exception.BusinessException;
import com.lumie.tenant.api.TenantStatus;

public class InvalidTenantStateException extends BusinessException {

    private final TenantStatus currentStatus;
    private final TenantStatus requiredStatus;

    public InvalidTenantStateException(TenantStatus currentStatus, TenantStatus requiredStatus) {
        super(TenantErrorCode.INVALID_TENANT_STATE,
                String.format("Invalid tenant state transition: current=%s, required=%s",
                        currentStatus, requiredStatus));
        this.currentStatus = currentStatus;
        this.requiredStatus = requiredStatus;
    }

    public TenantStatus getCurrentStatus() {
        return currentStatus;
    }

    public TenantStatus getRequiredStatus() {
        return requiredStatus;
    }
}
