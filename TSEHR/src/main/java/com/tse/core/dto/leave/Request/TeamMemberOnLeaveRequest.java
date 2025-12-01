package com.tse.core.dto.leave.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberOnLeaveRequest {

    private Long teamId;

    private LocalDate todayDate;
}
