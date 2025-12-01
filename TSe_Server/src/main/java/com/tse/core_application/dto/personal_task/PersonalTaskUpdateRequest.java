package com.tse.core_application.dto.personal_task;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.NewEffortTrack;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonalTaskUpdateRequest {

    @NotNull(message = "Id is a required field")
    private Long personalTaskId;

    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String taskTitle;

    @Size(min = 3, max = 1000, message = ErrorConstant.Task.DESC_LIMIT)
    private String taskDesc;

//    private Integer taskTypeId = Constants.TaskTypes.TASK; -- may be allowed in future as of now this is constant

    private String taskPriority;

    private Integer taskEstimate;

    private String workflowStatus;

    private LocalDateTime taskExpStartDate;

    private LocalDateTime taskExpEndDate;

    @Size(min = 3, max = 1000, message = ErrorConstant.Task.KEY_DECISIONS)
    private String keyDecisions;

    @Size(min = 3, max = 1000, message = ErrorConstant.Task.PARKING_LOT)
    private String parkingLot;

    private LocalDateTime taskActStDate;

    private LocalDateTime taskActEndDate;

    private Boolean currentActivityIndicator;

    private Boolean currentlyScheduledTaskIndicator;

    private Integer userPerceivedPercentageTaskCompleted;

    private List<NewEffortTrack> newEffortTracks;

    private List<NoteRequest> noteRequestList;

    //    private List<String> labelsToAdd; -- to be added in future

}
