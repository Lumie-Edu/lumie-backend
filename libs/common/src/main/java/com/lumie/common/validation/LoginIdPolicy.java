package com.lumie.common.validation;

public final class LoginIdPolicy {
    public static final String PATTERN = "^[a-zA-Z0-9_]+$";
    public static final int MIN_LENGTH = 4;
    public static final int MAX_LENGTH = 50;
    private LoginIdPolicy() {}

    public static boolean isValid(String value) {
        return value != null && value.length() >= MIN_LENGTH && value.length() <= MAX_LENGTH && value.matches(PATTERN);
    }
}
