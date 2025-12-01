package com.tse.core_application.constants;

import lombok.Getter;

@Getter
public enum PriorityEnum {
    P0("P0 - Extremely Critical"),
    P1("P1 - Critical"),
    P2("P2 - Normal Importance"),
    P3("P3 - Almost Normal Importance"),
    P4("P4 - Low Priority");

    private final String description;

    PriorityEnum(String description) {
        this.description = description;
    }

    public static PriorityEnum fromString(String text) {
        for (PriorityEnum priority : PriorityEnum.values()) {
            if (priority.name().equals(text)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}

