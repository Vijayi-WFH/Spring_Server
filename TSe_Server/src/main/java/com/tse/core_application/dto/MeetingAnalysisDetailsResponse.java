package com.tse.core_application.dto;

import com.tse.core_application.dto.meeting.ActionItemResponseDto;
import com.tse.core_application.model.ActionItem;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MeetingAnalysisDetailsResponse {
    private Long meetingId;
    private Integer modelId;
    private List<ActionItemResponseDto> actionItemList;
    private List<MeetingNoteResponse> meetingNoteList;
    private String minutesOfMeeting;
}
