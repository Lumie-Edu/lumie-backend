package com.lumie.academy.domain.vo;

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
        if (!isValid(normalized)) {
            throw new IllegalArgumentException("Invalid phone number: " + phone);
        }
        return new PhoneNumber(normalized);
    }

    private static String normalize(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.length() == 10) {
            return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
        } else if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }

        return phone;
    }

    private static boolean isValid(String phone) {
        return PHONE_PATTERN.matcher(phone).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
