package com.tse.core_application.dto;

import com.tse.core_application.model.SortingField;
import com.tse.core_application.model.TaskPriority;
import com.tse.core_application.model.StatType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class StatsRequest {

    @NotNull
    private Long userId;

    @Nullable
    private Long accountIdAssigned;

    @Nullable
    private Long teamId;

    @Nullable
    @Enumerated(EnumType.STRING)
    private List<TaskPriority> taskPriority;

    @Nullable
    private List<Long> orgIds;

    @Nullable
    private LocalDateTime currentDate;

    @Deprecated(since = "2022-09-20")
    @Nullable
    private LocalDateTime startDateForTaskActStartDate;

    @Deprecated(since = "2022-09-20")
    @Nullable
    private LocalDateTime endDateForTaskExpEndDate;

    @Nullable
    private LocalDateTime fromDate;

    @Nullable
    private LocalDateTime toDate;

    @Nullable
    private Long noOfDays;

    @Nullable
    private String fromDateType;

    @Nullable
    private String toDateType;

    @Nullable
    private LocalDateTime endDate;

    @Nullable
    private Long buId;

    @Nullable
    private Long projectId;

    @Nullable
    private Long sprintId;

    @Nullable
    private Long taskWorkflowId;

    @Nullable
    private Integer currentActivityIndicator;

    @Nullable
    private List<String> workflowTaskStatus;

    @Nullable
    @Enumerated(EnumType.STRING)
    private List<StatType> statName;

    @Nullable
    private List<Long> accountIds;

    private List<String> searches;

    private Boolean currentlyScheduledTaskIndicator;

    private LocalDate newEffortDate;

    private boolean isFirstTypeLessThan; // it will represent less than if true and greater than if false (default value - false).
    private boolean isSecondTypeLessThan = true; // it will represent less than if true and greater than if false (default value - true).

    private List<Long> labelIds;

    @NotNull
    private Boolean hasPagination = true;

    @Nullable
    private Long accountIdCreator;

    @Nullable
    private List<Integer> taskTypeList;

    HashMap<Integer, SortingField> sortingPriorityList;

    @Nullable
    private Long mentorAccountId;

    @Nullable
    private Long observerAccountId;

    @Nullable
    private Long epicId;

    private Boolean isStarred;

    private List <Long> starredBy;
  
    private List <Long> reportedBy;

    private List <Long> blockedBy;

    private List <Integer> blockedReasonTypeId;

    private List <Long> lastUpdatedBy;

}
