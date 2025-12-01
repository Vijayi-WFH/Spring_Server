package com.tse.core_application.dto.personal_task;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.dto.RecurrenceScheduleDTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalTime;

@Data
public class RecurrencePersonalTaskDTO {

    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String taskTitle;

    @Size(min = 3, max = 1000, message = ErrorConstant.Task.DESC_LIMIT)
    private String taskDesc;

//    private Integer taskTypeId; -- we have the default in this case as 1

    private String taskPriority;

//    @NotNull(message = ErrorConstant.RecurTask.WORKFLOW_ID)
//    private Integer taskWorkFlowId; -- we have the default for this as of now

    @NotNull(message = ErrorConstant.RecurTask.WORKFLOW_STATUS)
    private String taskWorkFlowStatus; // Options: 'Due Date Not Provided', 'Not-Started'

    private Integer taskEstimate;

//    private Long assignedToAccountId; -- not needed

    @NotNull(message = ErrorConstant.RecurTask.EXP_START_TIME)
    private LocalTime taskExpStartTime;

    @NotNull(message = ErrorConstant.RecurTask.EXP_END_TIME)
    private LocalTime taskExpEndTime;

    @NotNull(message = ErrorConstant.RecurTask.RECURRENCE_SCHEDULE)
    private RecurrenceScheduleDTO recurrenceSchedule;
}
