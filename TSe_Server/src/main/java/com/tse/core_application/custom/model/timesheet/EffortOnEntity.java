package com.tse.core_application.custom.model.timesheet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EffortOnEntity {

    private Integer entityTypeId;
    private String entityNumber;
    private Long teamId;
    private Long entityId;
    private String entityTitle;

    private Integer entityEffortMins;

    private Integer entityEarnedTime;
    private Long orgId;
    private Integer taskTypeId;
    private Boolean isBug;

    private Boolean isHalfDayLeave;
    private Integer halfDayLeaveType;

    private String leaveAlias;
//    private LocalDate taskEffortDate;
    private Long projectId;
}

