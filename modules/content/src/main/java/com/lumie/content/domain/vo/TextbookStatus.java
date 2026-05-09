package com.lumie.content.domain.vo;

public enum TextbookStatus {
    ACTIVE("활성"),
    INACTIVE("비활성");

    private final String description;

    TextbookStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
