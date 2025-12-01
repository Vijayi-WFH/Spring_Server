package com.tse.core_application.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendanceRecordDto {
    private LocalDate dateTime;
    private Integer minsWorked;
    private List<String> typeName;
    private List<Integer> typeId;
    private List<String> description;
    private Integer expectedWorkMins;
}
