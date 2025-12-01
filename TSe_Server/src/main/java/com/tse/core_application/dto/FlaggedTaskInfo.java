package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlaggedTaskInfo {
    // this model is used to send info of the flagged task (delayed/ watchlist) that have any successors
    private Long taskId;
    private String taskNumber;
    private Long teamId;
    private String taskTitle;
    private String taskDesc;
    private LocalDateTime taskExpStartDate;
    private LocalDateTime taskExpEndDate;
    private com.tse.core_application.model.StatType taskProgressSystem;
}
