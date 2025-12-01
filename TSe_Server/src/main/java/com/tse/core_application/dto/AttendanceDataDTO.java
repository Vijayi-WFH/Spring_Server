package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceDataDTO {
    private Long accountId;
    private Integer minsWorked;
    private String typeName;
    private Integer typeId;
    private Integer expectedWorkMins;
}
