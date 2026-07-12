package com.lumie.common.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SlugValidator {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{2,29}$");

    private static final Set<String> RESERVED_SLUGS = Set.of(
            "admin", "api", "www", "app", "static", "assets",
            "login", "logout", "register", "auth", "dashboard", "admin-panel",
            "health", "metrics", "actuator", "system", "internal"
    );

    private SlugValidator() {
    }

    public static boolean isValid(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        return SLUG_PATTERN.matcher(slug).matches() && !RESERVED_SLUGS.contains(slug);
    }

    public static boolean isReserved(String slug) {
        return slug != null && RESERVED_SLUGS.contains(slug.toLowerCase(Locale.ROOT));
    }
}
