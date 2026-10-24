package com.lumie.notification.domain.vo;

public enum RecipientRole {
    STUDENT,
    PARENT;

    public RecipientType toRecipientType() {
        return switch (this) {
            case STUDENT -> RecipientType.STUDENT;
            case PARENT -> RecipientType.PARENT;
        };
    }
}
