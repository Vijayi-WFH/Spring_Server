package com.tse.core_application.validators.annotations;

import com.tse.core_application.validators.NotNullOrZeroValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotNullOrZeroValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotNullOrZero {
    String message() default "Value cannot be null or zero";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

