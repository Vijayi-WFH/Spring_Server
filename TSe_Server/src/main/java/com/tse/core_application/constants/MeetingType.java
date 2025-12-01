package com.tse.core_application.constants;

public enum MeetingType {
    MEETING(1),
    COLLABORATION(2);

    private final int value;

    MeetingType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
