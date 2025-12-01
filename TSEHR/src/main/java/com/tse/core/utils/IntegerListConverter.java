package com.tse.core.utils;

import javax.persistence.Converter;

@Converter
public class IntegerListConverter extends ListConverter<Integer> {
    @Override
    protected Integer convertStringToNumber(String str) {
        return Integer.valueOf(str);
    }
}
