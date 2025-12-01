package com.tse.core_application.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class GithubUsernameValidator implements ConstraintValidator<ValidGithubUsername, String> {
    private static final String USERNAME_REGEX = "^(?!-)[a-zA-Z\\d-]{1,39}(?<!-)$";
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && (value.equals("*") || value.matches(USERNAME_REGEX));
    }
}
