package com.lumie.auth.application.port.in;

public interface RevokeSessionUseCase {

    void revokeSession(Long userId, String tenantSlug, String sid);

    void revokeOtherSessions(Long userId, String tenantSlug, String currentSid);
}
