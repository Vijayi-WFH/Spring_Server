package com.tse.core_application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.model.UserAccount;
import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.TypeReference;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class SprintWithoutSprintMembers {

    private Long sprintId;

    private String sprintTitle;

    private String sprintObjective;

    private LocalDateTime sprintExpStartDate;

    private LocalDateTime sprintExpEndDate;

    private LocalDateTime capacityAdjustmentDeadline;

    private LocalDateTime sprintActStartDate;

    private LocalDateTime sprintActEndDate;

    private Integer sprintStatus;

    private Integer entityTypeId;

    private Long entityId;

    private Integer hoursOfSprint;

    private UserAccount fkAccountIdCreator;

    private Integer earnedEfforts = 0;

    private Long nextSprintId;

    private Long previousSprintId;

    private Boolean canModifyEstimates = false;

    private Boolean canModifyIndicatorStayActiveInStartedSprint = false;
}
