package com.lumie.tenant.domain.exception;

import com.lumie.tenant.domain.vo.TenantStatus;

public class InvalidTenantStateException extends RuntimeException {

    private final TenantStatus currentStatus;
    private final TenantStatus requiredStatus;

    public InvalidTenantStateException(TenantStatus currentStatus, TenantStatus requiredStatus) {
        super(String.format("Invalid tenant state transition: current=%s, required=%s",
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
