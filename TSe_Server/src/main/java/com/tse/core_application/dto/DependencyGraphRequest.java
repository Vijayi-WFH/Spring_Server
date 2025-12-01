package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DependencyGraphRequest {
    LocalDateTime startDate;
    LocalDateTime endDate;
    Boolean isInternal;
    List<Long> sprintIds;
    List<Long> epicIds;
    Boolean getTasksWithOnlyDependencies;
    Long teamId;
}
