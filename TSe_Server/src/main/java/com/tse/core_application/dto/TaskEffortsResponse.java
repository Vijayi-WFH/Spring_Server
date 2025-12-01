package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskEffortsResponse {
    private Long timeTrackingId;
    private Long entityId;
    private Integer entityTypeId;
    private Integer newEffort;
    private Integer earnedTime;
    private Boolean isEditable = false;
    private Boolean isBilled;
    private LocalDateTime lastUpdatedDateTime;
}
