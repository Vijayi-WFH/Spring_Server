package com.tse.core_application.dto;

import com.tse.core_application.model.MeetingStats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MeetingOrganizerResponse {

    private Boolean hasMeetingStarted;
    private Boolean hasMeetingEnded;
    private LocalDateTime actualStartDateTime;
    private LocalDateTime actualEndDateTime;
    private MeetingStats meetingProgress;
}
