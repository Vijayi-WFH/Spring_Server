package com.tse.core_application.validators;

import com.tse.core_application.validators.annotations.NotNullOrZero;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NotNullOrZeroValidator implements ConstraintValidator<NotNullOrZero, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return value != 0;
    }
}

