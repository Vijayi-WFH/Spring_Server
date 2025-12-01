package com.tse.core_application.model;

public enum PlaceOfIdentification {
    INTERNAL,
    EXTERNAL,
    UNKNOWN;

    public static PlaceOfIdentification containsAny(String value) {
        for (PlaceOfIdentification enumValue : PlaceOfIdentification.values()) {
            if(enumValue.toString().equals(value)) {
                return enumValue;
            }
        }
        return null;
    }
}
