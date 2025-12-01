package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SprintStatusRequest {
    private String status;
    private LocalDateTime statusUpdateDate;
    private Boolean autoUpdate;
    private Boolean skipSprint = false;
    private Boolean deleteWorkItem = false;
//    before editing exp start date in minutes
    private Integer minTimeToStart;
}
