package com.lumie.admin.application.dto;

/**
 * Response body for {@code PATCH .../login-id} — the resolved login id
 * after persistence (may differ from the requested form, e.g. trimmed).
 */
public record ChangeLoginIdResponse(String loginId) {
}
