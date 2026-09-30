package com.lumie.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@NotBlank
@Size(min = PersonNamePolicy.MIN_LENGTH, max = PersonNamePolicy.MAX_LENGTH)
public @interface PersonName {
    String message() default "Name must be between 2 and 100 characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
