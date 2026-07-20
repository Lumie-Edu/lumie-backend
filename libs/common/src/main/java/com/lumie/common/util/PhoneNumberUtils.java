package com.lumie.common.util;


import java.util.regex.Pattern;

public class PhoneNumberUtils {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{2,3}-\\d{3,4}-\\d{4}$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d{10,11}$");
    private static final Pattern STUDENT_PHONE_DIGITS_ONLY = Pattern.compile("^\\d{11}$");

    private PhoneNumberUtils() {
        // Utility class
    }

    /**
     * Extracts only the digits from the given raw phone number string.
     * Returns null if the input is null.
     */
    public static String toDigits(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("[^0-9]", "");
    }

    /**
     * Normalizes a phone number to standard hyphenated format (e.g. 010-1234-5678).
     * This is useful for presentation or validation, but NOT recommended for DB storage.
     */
    public static String toHyphenated(String raw) {
        if (raw == null) return null;
        String digits = toDigits(raw);

        if (digits.length() == 10) {
            return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
        } else if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        return raw;
    }

    /**
     * Validates if the given string represents a valid Korean phone number.
     * It checks if it has 10 or 11 digits after stripping non-digits.
     */
    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String digits = toDigits(raw);
        return DIGITS_ONLY.matcher(digits).matches();
    }

    /**
     * Validates a student/staff phone number used for person matching.
     * Those surfaces require an 11-digit mobile number after normalization.
     */
    public static boolean isValidStudentPhone(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String digits = toDigits(raw);
        return STUDENT_PHONE_DIGITS_ONLY.matcher(digits).matches();
    }
}
