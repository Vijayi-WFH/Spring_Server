package com.tse.core_application.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOffDaysResponse {
    private LocalDate date;
    private Integer leaveTypeId;
    private String leaveTypeName;
    private String leaveDescription;
    private Boolean isHalfDayLeave;
    private Long leaveApplicationId;
}
