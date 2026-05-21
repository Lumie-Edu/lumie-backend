package com.lumie.student.domain.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    // --- isValid ---

    @Test
    void isValid_returnsFalse_forNull() {
        assertThat(PhoneNumber.isValid(null)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forBlank() {
        assertThat(PhoneNumber.isValid("   ")).isFalse();
    }

    @Test
    void isValid_returnsTrue_forValidElevenDigitNumber() {
        assertThat(PhoneNumber.isValid("010-1234-5678")).isTrue();
    }

    @Test
    void isValid_returnsTrue_forDigitsOnlyElevenDigit() {
        assertThat(PhoneNumber.isValid("01012345678")).isTrue();
    }

    @Test
    void isValid_returnsTrue_forValidTenDigitNumber() {
        assertThat(PhoneNumber.isValid("02-1234-5678")).isTrue();
    }

    @Test
    void isValid_returnsFalse_forInvalidFormat() {
        assertThat(PhoneNumber.isValid("1234")).isFalse();
        assertThat(PhoneNumber.isValid("notaphone")).isFalse();
    }

    // --- normalize ---

    @Test
    void normalize_returnsNull_forNull() {
        assertThat(PhoneNumber.normalize(null)).isNull();
    }

    @Test
    void normalize_formatsElevenDigitsWithHyphens() {
        assertThat(PhoneNumber.normalize("01012345678")).isEqualTo("010-1234-5678");
    }

    @Test
    void normalize_formatsTenDigitsWithHyphens() {
        assertThat(PhoneNumber.normalize("0212345678")).isEqualTo("02-1234-5678");
    }

    @Test
    void normalize_stripsNonDigitsBeforeFormatting() {
        assertThat(PhoneNumber.normalize("010 1234 5678")).isEqualTo("010-1234-5678");
        assertThat(PhoneNumber.normalize("010.1234.5678")).isEqualTo("010-1234-5678");
    }

    @Test
    void normalize_returnsInputUnchanged_whenDigitCountIsNotTenOrEleven() {
        assertThat(PhoneNumber.normalize("1234")).isEqualTo("1234");
    }

    // --- of (factory — throws on invalid) ---

    @Test
    void of_returnsNull_forNullInput() {
        assertThat(PhoneNumber.of(null)).isNull();
    }

    @Test
    void of_returnsNull_forBlankInput() {
        assertThat(PhoneNumber.of("  ")).isNull();
    }

    @Test
    void of_throwsBusinessException_forInvalidPhone() {
        assertThatThrownBy(() -> PhoneNumber.of("notaphone"))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
    }

    @Test
    void of_parsesValidRawPhoneAndNormalizesIt() {
        PhoneNumber pn = PhoneNumber.of("01012345678");
        assertThat(pn).isNotNull();
        assertThat(pn.getValue()).isEqualTo("010-1234-5678");
    }
}
