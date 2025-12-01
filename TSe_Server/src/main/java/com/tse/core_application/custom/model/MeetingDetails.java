package com.tse.core_application.custom.model;

import com.tse.core_application.model.MeetingStats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MeetingDetails {
    private String meetingNumber;
    private Long meetingId;
    private String title;
    private String venue;
    private LocalDateTime startDateTime;
    private String agenda;
    private String meetingKey;

    public MeetingDetails(String meetingNumber, Long meetingId, Object title, Object venue, LocalDateTime startDateTime, Object agenda, Object meetingKey) {
        this.meetingNumber = meetingNumber;
        this.meetingId = meetingId;
        this.title = (String) title;
        this.venue = (String) venue;
        this.startDateTime = startDateTime;
        this.agenda = (String) agenda;
        this.meetingKey = (String) meetingKey;
    }
}
