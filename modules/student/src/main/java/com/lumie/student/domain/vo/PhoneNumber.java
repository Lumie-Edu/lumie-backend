package com.lumie.student.domain.vo;

import com.lumie.common.exception.BusinessException;
import com.lumie.student.domain.exception.StudentErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class PhoneNumber {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{2,3}-\\d{3,4}-\\d{4}$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d{10,11}$");

    @Column(name = "phone", length = 20)
    private String value;

    private PhoneNumber(String value) {
        this.value = value;
    }

    public static PhoneNumber of(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String normalized = normalize(phone);
        if (!isValid(phone)) {
            throw new BusinessException(StudentErrorCode.INVALID_PHONE_NUMBER,
                    "Invalid phone number: " + phone);
        }
        return new PhoneNumber(normalized);
    }

    /**
     * Returns the canonical hyphenated form of {@code raw} (strips non-digits, then reformats
     * as XX-XXXX-XXXX for 10-digit or XXX-XXXX-XXXX for 11-digit numbers).
     * Returns {@code null} when {@code raw} is {@code null}.
     * Does NOT validate — call {@link #isValid(String)} separately when needed.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");

        if (digits.length() == 10) {
            return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
        } else if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }

        return raw;
    }

    /**
     * Returns {@code true} if {@code raw} can be parsed into a valid {@link PhoneNumber}.
     * Null-safe: returns {@code false} for {@code null} or blank input. Does NOT throw.
     */
    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return PHONE_PATTERN.matcher(normalize(raw)).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
