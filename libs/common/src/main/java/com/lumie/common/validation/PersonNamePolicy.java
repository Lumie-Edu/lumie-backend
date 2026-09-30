package com.lumie.common.validation;

public final class PersonNamePolicy {
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 100;
    private PersonNamePolicy() {}

    public static boolean isValid(String value) {
        return value != null && !value.isBlank() && value.length() >= MIN_LENGTH && value.length() <= MAX_LENGTH;
    }
}
