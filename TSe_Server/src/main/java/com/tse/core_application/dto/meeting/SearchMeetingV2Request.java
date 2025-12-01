package com.tse.core_application.dto.meeting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchMeetingV2Request {

    private Long entityTypeId;
    private Long entityId;
    private String meetingNumber;
}
