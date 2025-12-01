package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.ActionItem;
import com.tse.core_application.validators.annotations.TrimmedSize;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class MeetingAnalysisDetailsRequest {

    @NotNull(message = ErrorConstant.Meeting.MEETING_ID)
    private Long meetingId;
    private Integer modelId;
    private List<ActionItem> actionItemList;
    private List<MeetingNoteDto> meetingNoteList;
    @TrimmedSize (max = 5000, message = ErrorConstant.Meeting.MINUTES_OF_MEETING)
    private String minutesOfMeeting;

}
