package com.lumie.student.application.dto.request;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentUpdateRequestValidationTest {
    @Test
    void providedNameCannotBeWhitespaceButMayBeOmitted() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new StudentUpdateRequest(null, null, null, null, null, null))).isEmpty();
            assertThat(validator.validate(new StudentUpdateRequest("  ", null, null, null, null, null)))
                    .extracting(violation -> violation.getPropertyPath().toString()).containsExactly("name");
        }
    }
}
