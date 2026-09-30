package com.lumie.auth.api;

/** Only changed identity fields are projected before the user's transaction commits. */
public record UserIdentityChangedEvent(
        Long userId,
        String name,
        String phone,
        String email,
        boolean nameChanged,
        boolean phoneChanged,
        boolean emailChanged
) {}
