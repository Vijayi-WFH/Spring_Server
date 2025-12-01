package com.tse.core_application.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CleanedSizeValidator.class)
public @interface CleanedSize {
    String message() default "Invalid input: Please check size validation";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int value();
}

