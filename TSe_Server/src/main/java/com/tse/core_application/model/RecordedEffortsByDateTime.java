package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordedEffortsByDateTime {

    private String firstName;

    private String middleName;

    private String lastName;

    private Long accountId;

    private Long timeTrackingId;

    private Integer recordedEffortMins;

    private Integer recordedEarnedTime;

    private LocalDate recordedEffortDate;

    private LocalDateTime lastUpdatedDateTime;

    private Boolean isEditable;

    private Integer entityTypeId;

    private String entityNumber;

    private Long entityId;

    private Boolean isBilled = false;
}
