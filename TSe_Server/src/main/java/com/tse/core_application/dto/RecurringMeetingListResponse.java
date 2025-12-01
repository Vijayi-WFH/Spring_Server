package com.tse.core_application.dto;

import com.tse.core_application.dto.label.LabelResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecurringMeetingListResponse {

    private Long recurringMeetingId;
    private String recurringMeetingNumber;
    private LocalDateTime recurringMeetingStartDateTime;  // default date : current date
    private LocalDateTime recurringMeetingEndDateTime;   // default : current date + 30 days
    private Boolean isCancelled;
    private Integer numOfOccurrences;
    private Integer recurringFrequencyIndicator;
    private String recurDays;
    private Integer recurEvery;
    private List<MeetingResponse> meetingResponseList;
    private Integer pageNumber;
    private Integer pageSize;
    private Long totalMeetingsInList;
    private Integer totalPages;
    private boolean isLastPage;
    private List<LabelResponse> labels;


}
