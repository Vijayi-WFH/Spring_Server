package com.tse.core_application.validators;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CleanedSizeValidator implements ConstraintValidator<CleanedSize, String> {
    private int value;

    @Override
    public void initialize(CleanedSize constraintAnnotation) {
        this.value = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String htmlText, ConstraintValidatorContext context) {
        if (htmlText == null) {
            return true;
        }

        String cleanedValue = cleanString(htmlText);
        // Validate the length of the cleaned string
        return cleanedValue.length() <= value;
    }

    private String cleanString(String htmlText) {
        Document doc = Jsoup.parse(htmlText);
        String plainText = doc.text();
        return plainText;
    }
}

