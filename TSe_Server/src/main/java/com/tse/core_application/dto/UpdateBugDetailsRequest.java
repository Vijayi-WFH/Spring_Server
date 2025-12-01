package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@AllArgsConstructor
public class UpdateBugDetailsRequest {

    @NotNull(message = "Task Identifier is missing")
    private Long taskId;

    @NotNull(message = "Resolution is a required field")
    private Integer resolutionId;

    @NotNull(message = "Steps taken to complete is a required field")
    @Size(max=1000, message= ErrorConstant.Task.STEPS_TAKEN_TO_COMPLETE )
    private String stepsTakenToComplete;

    private String placeOfIdentification;
    // Root cause analysis id (in case of bug)
    private Integer rcaId;

    private Boolean isRcaDone;

    private String rcaReason;

    private List<Long> rcaIntroducedBy;
}
