package com.lumie.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@NotBlank
@Size(min = LoginIdPolicy.MIN_LENGTH, max = LoginIdPolicy.MAX_LENGTH)
@Pattern(regexp = LoginIdPolicy.PATTERN)
public @interface LoginId {
    String message() default "Login ID must be 4–50 letters, numbers, or underscores";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
