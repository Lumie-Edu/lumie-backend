package com.lumie.notification.domain.vo;

public enum RecipientRole {
    STUDENT,
    GUARDIAN;

    public RecipientType toRecipientType() {
        return switch (this) {
            case STUDENT -> RecipientType.STUDENT;
            case GUARDIAN -> RecipientType.GUARDIAN;
        };
    }
}
