package com.tse.core.custom.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EffortOnEntity {

    private Long teamId;

    private Long entityId;
   
    private Integer entityTypeId;

    private String entityNumber;

    private String entityTitle;

    private Integer entityEffortMins;

    private Integer entityEarnedTime;

    private Long orgId;

    private Long projectId;

    private Integer taskTypeId;

    private Boolean isBug;

    private Boolean isHalfDayLeave;

    private Integer halfDayLeaveType;

    private String leaveAlias;
//    private LocalDate taskEffortDate;
    }

