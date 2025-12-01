package com.tse.core_application.dto.leave.Request;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamLeaveHistoryRequest {

    private Long accountId;

    @NotNull(message = ErrorConstant.Task.fk_TEAM_ID)
    private Long teamId;
}
