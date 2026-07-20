package com.lumie.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberUtilsTest {

    // --- isValid ---

    @Test
    void isValid_returnsFalse_forNull() {
        assertThat(PhoneNumberUtils.isValid(null)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forBlank() {
        assertThat(PhoneNumberUtils.isValid("   ")).isFalse();
    }

    @Test
    void isValid_returnsTrue_forValidElevenDigitNumber() {
        assertThat(PhoneNumberUtils.isValid("010-1234-5678")).isTrue();
    }

    @Test
    void isValid_returnsTrue_forDigitsOnlyElevenDigit() {
        assertThat(PhoneNumberUtils.isValid("01012345678")).isTrue();
    }

    @Test
    void isValid_returnsTrue_forValidTenDigitNumber() {
        assertThat(PhoneNumberUtils.isValid("02-1234-5678")).isTrue();
    }

    @Test
    void isValid_returnsFalse_forInvalidFormat() {
        assertThat(PhoneNumberUtils.isValid("1234")).isFalse();
        assertThat(PhoneNumberUtils.isValid("notaphone")).isFalse();
    }

    @Test
    void isValidStudentPhone_requiresElevenDigits() {
        assertThat(PhoneNumberUtils.isValidStudentPhone("010-1234-5678")).isTrue();
        assertThat(PhoneNumberUtils.isValidStudentPhone("01012345678")).isTrue();
        assertThat(PhoneNumberUtils.isValidStudentPhone("02-1234-5678")).isFalse();
        assertThat(PhoneNumberUtils.isValidStudentPhone("1012345678")).isFalse();
    }

    // --- toHyphenated (equivalent to old normalize) ---

    @Test
    void toHyphenated_returnsNull_forNull() {
        assertThat(PhoneNumberUtils.toHyphenated(null)).isNull();
    }

    @Test
    void toHyphenated_formatsElevenDigitsWithHyphens() {
        assertThat(PhoneNumberUtils.toHyphenated("01012345678")).isEqualTo("010-1234-5678");
    }

    @Test
    void toHyphenated_formatsTenDigitsWithHyphens() {
        assertThat(PhoneNumberUtils.toHyphenated("0212345678")).isEqualTo("02-1234-5678");
    }

    @Test
    void toHyphenated_stripsNonDigitsBeforeFormatting() {
        assertThat(PhoneNumberUtils.toHyphenated("010 1234 5678")).isEqualTo("010-1234-5678");
        assertThat(PhoneNumberUtils.toHyphenated("010.1234.5678")).isEqualTo("010-1234-5678");
    }

    @Test
    void toHyphenated_returnsInputUnchanged_whenDigitCountIsNotTenOrEleven() {
        assertThat(PhoneNumberUtils.toHyphenated("1234")).isEqualTo("1234");
    }

    // --- toDigits ---

    @Test
    void toDigits_returnsNull_forNull() {
        assertThat(PhoneNumberUtils.toDigits(null)).isNull();
    }

    @Test
    void toDigits_returnsOnlyDigits() {
        assertThat(PhoneNumberUtils.toDigits("010-1234-5678")).isEqualTo("01012345678");
        assertThat(PhoneNumberUtils.toDigits("02)123-4567")).isEqualTo("021234567");
        assertThat(PhoneNumberUtils.toDigits("010.1234.5678")).isEqualTo("01012345678");
    }
}
