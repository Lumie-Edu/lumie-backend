package com.lumie.file.domain.vo;

public enum EntityType {
    ANNOUNCEMENT("announcement"),
    QNA("qna"),
    TEXTBOOK("textbook"),
    OMR("omr");

    private final String pathSegment;

    EntityType(String pathSegment) {
        this.pathSegment = pathSegment;
    }

    public String getPathSegment() {
        return pathSegment;
    }
}
