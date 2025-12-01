package com.tse.core_application.dto;

import com.tse.core_application.model.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class EpicResponse {

    private Long epicId;

    private String epicTitle;

    private String epicNumber;

    private String epicDesc;

    private String epicPriority;

    private WorkFlowEpicStatus fkWorkflowEpicStatus;

    private String epicNote;

    private List<Long> linkedEpicId;

    private Long orgId;

    private Long projectId;

    private List<TeamResponse> teamList;

    private Integer entityTypeId;

    private Long entityId;

//    private List<Label> epicLabels;

    private Integer estimate;

    private Integer originalEstimate;

    private Integer runningEstimate;

    private Long accountIdAssigned;

    private Long accountIdOwner;

    private String attachments;

    private String release;

    private LocalDateTime expStartDateTime;

    private LocalDateTime actStartDateTime;

    private LocalDateTime expEndDateTime;

    private LocalDateTime actEndDateTime;

    private LocalDateTime dueDateTime;

    private String valueArea;

    private String functionalArea;

    private String quarterlyPlan;

    private String yearlyPlan;

    private List<EpicComment> comments;

    private String color;

    private Integer loggedEfforts;

    private Integer earnedEfforts;
}
