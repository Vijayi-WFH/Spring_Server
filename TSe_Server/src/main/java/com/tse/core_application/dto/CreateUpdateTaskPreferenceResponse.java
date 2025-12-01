package com.tse.core_application.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class CreateUpdateTaskPreferenceResponse {
    private String quickCreateWorkflowStatus;
    private LocalTime expStartTime;
    private LocalTime expEndTime;
    private Boolean isGeoFencingAllowed = false;
    private Boolean isGeoFencingActive = false;
}
