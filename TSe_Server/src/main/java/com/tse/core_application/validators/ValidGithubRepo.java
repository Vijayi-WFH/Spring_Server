package com.tse.core_application.validators;

import com.tse.core_application.constants.ErrorConstant;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GithubRepoValidator.class)
public @interface ValidGithubRepo {
    String message() default ErrorConstant.Github.GITHUB_REPO_INVALID;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
