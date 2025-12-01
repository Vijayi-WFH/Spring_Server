package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingCondensedViewRequest {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private Long orgId;
    private List<Long> teamIds;

}
