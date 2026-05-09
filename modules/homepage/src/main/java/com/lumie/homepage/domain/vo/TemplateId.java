package com.lumie.homepage.domain.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TemplateId {
    SOLO_INSTRUCTOR("solo-instructor");

    private final String slug;

    TemplateId(String slug) {
        this.slug = slug;
    }

    @JsonValue
    public String getSlug() {
        return slug;
    }

    @JsonCreator
    public static TemplateId fromSlug(String value) {
        if (value == null) return null;
        return Arrays.stream(values())
                .filter(t -> t.slug.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown templateId: " + value));
    }
}
