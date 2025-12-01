package com.tse.core_application.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class GithubRepoValidator implements ConstraintValidator<ValidGithubRepo, String> {
    private static final String REPO_REGEX = "^[a-zA-Z0-9_.-]+$";
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && (value.equals("*") || value.matches(REPO_REGEX));
    }
}
