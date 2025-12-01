package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class AddJiraTaskRequest {
    List<MappedJiraUser> jiraUserMappedList;
    private Long teamId;
    private List<JiraTaskToCreate> jiraTaskToCreateList;

    @NotNull(message = ErrorConstant.Jira.TASK_HANDE_STRATEGY)
    private Integer taskHandlingStrategy;

    @NotNull(message = ErrorConstant.Jira.DEFAULT_ASSIGN_TO)
    private Long defaultAccountIdAssigned;

    @NotNull(message = ErrorConstant.Jira.DEFAULT_TASK_COMPLETED_PERCENTAGE)
    private Integer completedTaskPercentage;

    @NotNull(message = ErrorConstant.Jira.DEFAULT_ESTIMATE)
    private Integer customEstimate;

    private HashMap<String, Integer> jiraCustomStatusMappedList;

    private String emailToFetchAttachment;

    private String jiraToken;

    private List<JiraIssueTypeMapping> jiraIssueTypeMappingList;

    private String projectId;

    private String siteUrl;
}
