package com.tse.core_application.dto;

import lombok.Data;

@Data
public class PartOfActiveSprintResponse {
    private Boolean isPartOfActiveSprint = false;
    private String message;
}
