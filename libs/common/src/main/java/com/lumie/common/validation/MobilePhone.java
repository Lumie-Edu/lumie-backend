package com.lumie.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MobilePhoneValidator.class)
public @interface MobilePhone {
    String message() default "Phone must contain 11 digits";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
