package com.lumie.common.validation;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PersonValidationTest {
    record Input(@PersonName String name, @MobilePhone String phone, @LoginId String loginId) {}
    record PartialInput(@ProvidedPersonName String name) {}

    @Test void providedNameIsOptionalButCannotBeBlank() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new PartialInput(null))).isEmpty();
            assertThat(validator.validate(new PartialInput("홍길동"))).isEmpty();
            assertThat(validator.validate(new PartialInput("  "))).hasSize(1);
            assertThat(validator.validate(new PartialInput("홍"))).hasSize(1);
            assertThat(validator.validate(new PartialInput("가".repeat(101)))).hasSize(1);
        }
    }
    @Test void validatesSharedBoundariesAndNormalizedMobile() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new Input("홍길동", "010-1234-5678", "hong_123"))).isEmpty();
            assertThat(validator.validate(new Input("홍", "02-1234-5678", "ab-c")))
                    .extracting(v -> v.getPropertyPath().toString()).containsExactlyInAnyOrder("name", "phone", "loginId");
            assertThat(validator.validate(new Input("홍길동", null, "hong_123"))).isEmpty();
            assertThat(validator.validate(new Input("가".repeat(101), "", "abc")))
                    .extracting(v -> v.getPropertyPath().toString()).containsExactlyInAnyOrder("name", "loginId");
        }
    }
}
