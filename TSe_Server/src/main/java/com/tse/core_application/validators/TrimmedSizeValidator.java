package com.tse.core_application.validators;

import com.tse.core_application.validators.annotations.TrimmedSize;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TrimmedSizeValidator implements ConstraintValidator<TrimmedSize, String> {

    private int min;
    private int max;

    @Override
    public void initialize(TrimmedSize constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String trimmedValue = value.trim();
        int length = trimmedValue.length();

        if (length < min || length > max) {
            // Optional: Customize the error message if needed, or let the default from the annotation handle it.
            // context.disableDefaultConstraintViolation();
            // context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
            //        .addConstraintViolation();
            return false;
        }
        return true;
    }
}
