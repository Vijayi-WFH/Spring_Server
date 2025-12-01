package com.tse.core.utils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public abstract class ListConverter<T extends Number> implements AttributeConverter<List<T>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return list.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(DELIMITER));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting number value to String");
        }
    }

    @Override
    public List<T> convertToEntityAttribute(String joined) {
        if (joined == null || joined.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return Arrays.stream(joined.split(DELIMITER))
                    .map(this::convertStringToNumber)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error converting String element to number");
        }
    }

    protected abstract T convertStringToNumber(String str);
}
