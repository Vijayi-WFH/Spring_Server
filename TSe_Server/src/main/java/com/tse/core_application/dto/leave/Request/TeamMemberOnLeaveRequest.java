package com.tse.core_application.dto.leave.Request;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberOnLeaveRequest {

    @NotNull(message = ErrorConstant.Task.fk_TEAM_ID)
    private Long teamId;

    @NotNull
    private LocalDate todayDate;
}
