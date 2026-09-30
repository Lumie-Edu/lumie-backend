package com.lumie.common.validation;

import com.lumie.common.util.PhoneNumberUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MobilePhoneValidator implements ConstraintValidator<MobilePhone, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || PhoneNumberUtils.isValidStudentPhone(value);
    }
}
