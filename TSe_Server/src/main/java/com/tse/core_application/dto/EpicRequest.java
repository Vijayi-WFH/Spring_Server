package com.tse.core_application.dto;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;

import com.tse.core_application.model.EpicComment;
import com.tse.core_application.model.Label;
import com.tse.core_application.model.Task;
import com.tse.core_application.model.WorkFlowEpicStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class EpicRequest {

    private Long epicId;

    @NotBlank(message = ErrorConstant.Epic.EPIC_TITLE)
    @Size(min = 3, max = 70, message = ErrorConstant.Epic.EPIC_LIMIT)
    private String epicTitle;

    @NotBlank(message = ErrorConstant.Epic.EPIC_DESC)
    @Size(min = 3, max = 5000, message = ErrorConstant.Epic.DESC_LIMIT)
    private String epicDesc;

    @Nullable
    private String epicPriority;

    private Integer workflowEpicStatusId;

    @NotNull(message = ErrorConstant.Epic.ORG_ID)
    private Long orgId;

    @NotNull(message = ErrorConstant.Epic.PROJECT_ID)
    private Long projectId;

    @NotNull(message = ErrorConstant.Epic.ENTITY_TYPE_ID)
    private Integer entityTypeId;

    private List<Long> addTeamList;

    private List<Long> removeTeamList;

    private Integer estimate;

    private Long assignTo;

    private Long epicOwner;

    private String color;

    private LocalDateTime expStartDateTime;

    private LocalDateTime expEndDateTime;

    private LocalDateTime dueDateTime;

    private LocalDateTime actStartDateTime;

    private LocalDateTime actEndDateTime;

    // for update
    private String attachments;

    private String release;

    private String valueArea;

    private String functionalArea;

    private String quarterlyPlan;

    private String yearlyPlan;

    private String epicNote;

    private Boolean deleteWorkItem = false;
}
