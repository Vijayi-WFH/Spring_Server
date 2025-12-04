package com.tse.core_application.dto.meeting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MeetingLinkInfo {
    private String url;
    private String platform;   // GOOGLE_MEET | ZOOM | MICROSOFT_TEAMS | WEBEX | CUSTOM_JITSI | OTHER
    private String label;      // "Google Meet" | "Zoom" | "Microsoft Teams" | "Webex" | meetingName | "Other link"
}
