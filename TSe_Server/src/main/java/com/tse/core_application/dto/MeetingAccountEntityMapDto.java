package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MeetingAccountEntityMapDto {
    private Long meetingId;
    private Long accountId;
    private Long entityId;
    private Integer entityTypeId;

    public MeetingAccountEntityMapDto(Long meetingId, Long accountId) {
        this.meetingId = meetingId;
        this.accountId = accountId;
    }
}
