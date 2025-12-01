package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskValidationDto {

    private Long assignedToAccountId;
    private Integer userPerceivePercentageTaskCompleted;
    private Long meetingId;

}
