package com.lumie.auth.application.port.in;

import com.lumie.auth.application.dto.response.SessionResponse;

import java.util.List;

public interface GetSessionsUseCase {

    List<SessionResponse> getSessions(Long userId, String tenantSlug, String currentSid);
}
