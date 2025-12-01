package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class MeetingAnalysisFetchButtonRequest {

    @NotNull(message = ErrorConstant.ORG_ID_ERROR)
    private Long orgId;
    @NotNull(message = ErrorConstant.Meeting.MEETING_ID)
    private Long meetingId;
    @NotNull(message = ErrorConstant.Meeting.MODEL_ID)
    private Integer modelId;
    @NotNull(message = ErrorConstant.Meeting.IS_PROCESSING)
    private Boolean isProcessed;

}
