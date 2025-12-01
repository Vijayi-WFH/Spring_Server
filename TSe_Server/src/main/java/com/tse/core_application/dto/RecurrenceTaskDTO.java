package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;


@Data
public class RecurrenceTaskDTO {

    // fields related to the task details

    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String taskTitle;

    @Size(min = 3, max = 5000, message = ErrorConstant.Task.DESC_LIMIT)
    private String taskDesc;

    private Integer taskTypeId;

    private String taskPriority;

    @NotNull(message = ErrorConstant.RecurTask.WORKFLOW_ID)
    private Integer taskWorkFlowId;

    @NotNull(message = ErrorConstant.RecurTask.WORKFLOW_STATUS)
    private String taskWorkFlowStatus; // Options: 'Backlog', 'Not-Started'

    private Integer taskEstimate; // mandatory for 'Not-Started'

    @NotNull(message = ErrorConstant.RecurTask.TEAM_ID)
    private Long teamId;

    private Long assignedToAccountId;

    private List<String> labelsToAdd;

    @NotNull(message = ErrorConstant.RecurTask.EXP_START_TIME)
    private LocalTime taskExpStartTime;

    @NotNull(message = ErrorConstant.RecurTask.EXP_END_TIME)
    private LocalTime taskExpEndTime;

    @NotNull(message = ErrorConstant.RecurTask.RECURRENCE_SCHEDULE)
    private RecurrenceScheduleDTO recurrenceSchedule;

    private Boolean isBug = false;
}
