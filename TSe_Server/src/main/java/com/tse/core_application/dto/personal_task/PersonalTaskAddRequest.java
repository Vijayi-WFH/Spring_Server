package com.tse.core_application.dto.personal_task;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.Constants;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonalTaskAddRequest {

    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String taskTitle;

    @Size(min = 3, max = 5000, message = ErrorConstant.Task.DESC_LIMIT)
    private String taskDesc;

    private Integer taskTypeId = Constants.TaskTypes.PERSONAL_TASK;

    @NotNull(message = ErrorConstant.Task.PRIORITY)
    private String taskPriority;

    private Integer taskEstimate;

    @NotNull(message = ErrorConstant.Task.WORKFLOW_STATUS)
    private String workflowStatus;

    private LocalDateTime taskExpStartDate;

    private LocalDateTime taskExpEndDate;

    // ToDo: to be included later
//    @Transient
//    private List<String> labelsToAdd;

    @Size(min = 3, max = 1000, message = ErrorConstant.Task.KEY_DECISIONS)
    private String keyDecisions;

    @Size(min = 3, max = 1000, message = ErrorConstant.Task.PARKING_LOT)
    private String parkingLot;

    private String taskState; // this will be set by backend -- frontend need not provide this value

    private List<NoteRequest> noteRequestList;

}
