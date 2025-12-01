package com.tse.core_application.custom.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SprintResponseForFilter {
    Long sprintId;
    Object sprintTitle;
    Object teamName;
    Object projectName;
    Object orgName;
    LocalDateTime sprintExpStartDate;
    LocalDateTime sprintExpEndDate;
    Integer hoursOfSprint;
    Integer consumedSprintHours;
}
