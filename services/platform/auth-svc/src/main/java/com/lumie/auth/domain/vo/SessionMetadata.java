package com.lumie.auth.domain.vo;

public record SessionMetadata(String device, String ipAddress, DeviceCategory category) {
}
