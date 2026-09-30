package com.lumie.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validates a name when supplied in a partial update. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProvidedPersonNameValidator.class)
public @interface ProvidedPersonName {
    String message() default "Name must be between 2 and 100 characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
