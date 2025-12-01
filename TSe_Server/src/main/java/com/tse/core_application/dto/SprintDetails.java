package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SprintDetails {

    private Long sprintId;
    private String sprintTitle;
    private String sprintObjective;
    private LocalDateTime sprintExpStartDate;
    private LocalDateTime sprintExpEndDate;
    private LocalDateTime capacityAdjustmentDeadline;
    private LocalDateTime sprintActStartDate;
    private LocalDateTime sprintActEndDate;
    private Integer sprintStatus;
    private Integer entityTypeId;
    private Long entityId;
    private String entityName;
}
