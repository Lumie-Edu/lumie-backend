package com.lumie.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProvidedPersonNameValidator implements ConstraintValidator<ProvidedPersonName, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || PersonNamePolicy.isValid(value);
    }
}
