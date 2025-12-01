package com.tse.core_application.dto;

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
public class MeetingFiltersRequest {

    private Long orgId;
    private List<Long> teamIds;
    private List<Long> projectIds;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private Long createdAccountId;
    private Long organizerAccountId;
    private List<Long> attendeeAccountIds;
}
