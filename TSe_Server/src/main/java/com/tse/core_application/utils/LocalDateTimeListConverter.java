package com.tse.core_application.utils;


import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class LocalDateTimeListConverter implements AttributeConverter<List<LocalDateTime>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<LocalDateTime> localDateTimeList) {
        if(localDateTimeList == null || localDateTimeList.isEmpty()) {
            return null;
        }
        return localDateTimeList.stream()
                .map(LocalDateTime::toString)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<LocalDateTime> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(dbData.split(DELIMITER))
                .map(LocalDateTime::parse)
                .collect(Collectors.toList());
    }
}
