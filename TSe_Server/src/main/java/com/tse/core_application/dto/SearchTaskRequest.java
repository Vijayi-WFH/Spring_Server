package com.tse.core_application.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class SearchTaskRequest {
//    @NotNull (message = "searchTerm can not be null")
    private String searchTerm;
    private Long orgId;
    private Long buId;
    private Long teamId;
    private Long projectId;
    private List<String> workflowStatuses;
    private List<Integer> workflowStatusIds; // frontend will send workflowStatuses only -- this is just for backend processing
    private Long accountIdAssigned;
    private List<String> taskProgressSystems;
    private Long sprintId;
    private Long epicId;
    private List<String> taskNumbersToSkip;
    private List<Long> labelIds;
    private Integer currentActivityIndicator;
    private Boolean currentlyScheduledTaskIndicator;
    private Boolean includePersonalTasks = false;
    private Boolean isStarred;
    private List<Long>starredBy;
}

