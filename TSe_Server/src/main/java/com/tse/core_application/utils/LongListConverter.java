package com.tse.core_application.utils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class LongListConverter implements AttributeConverter<List<Long>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<Long> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return list.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(DELIMITER));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting Long value to String");
        }
    }

    @Override
    public List<Long> convertToEntityAttribute(String joined) {
        if (joined == null || joined.isEmpty()) {
            return Collections.emptyList();
        }

        try{
            return Arrays.stream(joined.split(DELIMITER))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error converting String element to Long");
        }
    }
}