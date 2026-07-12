package com.lumie.auth.adapter.in.web;

import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.auth.domain.vo.SessionMetadata;
import jakarta.servlet.http.HttpServletRequest;

public final class SessionMetadataExtractor {

    private SessionMetadataExtractor() {
    }

    public static SessionMetadata extract(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String device = parseDevice(userAgent);
        DeviceCategory category = parseCategory(userAgent);
        return new SessionMetadata(device, category);
    }

    private static String parseDevice(String ua) {
        if (ua == null || ua.isBlank()) {
            return "Unknown";
        }
        return parseBrowser(ua) + " / " + parseOs(ua);
    }

    private static String parseBrowser(String ua) {
        if (ua.contains("Edg/") || ua.contains("Edge/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera/")) return "Opera";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Safari/")) return "Safari";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("MSIE") || ua.contains("Trident/")) return "IE";
        return "Unknown Browser";
    }

    private static String parseOs(String ua) {
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("iPad")) return "iPad";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("Macintosh") || ua.contains("Mac OS")) return "macOS";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("CrOS")) return "ChromeOS";
        return "Unknown OS";
    }

    private static DeviceCategory parseCategory(String ua) {
        if (ua == null || ua.isBlank()) {
            return DeviceCategory.UNKNOWN;
        }
        if (ua.contains("iPad")) {
            return DeviceCategory.TABLET;
        }
        if (ua.contains("iPhone")) {
            return DeviceCategory.MOBILE;
        }
        if (ua.contains("Android")) {
            return ua.contains("Mobile") ? DeviceCategory.MOBILE : DeviceCategory.TABLET;
        }
        if (ua.contains("Macintosh") || ua.contains("Mac OS")
                || ua.contains("Windows") || ua.contains("Linux") || ua.contains("CrOS")) {
            return DeviceCategory.PC;
        }
        return DeviceCategory.UNKNOWN;
    }
}
