package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SprintInfo {
    private String sprintTitle;
    private Long sprintId;
    private LocalDateTime sprintExpStartDate;
    private LocalDateTime sprintExpEndDate;
    private Integer sprintStatus;
    private String teamCode;
}
