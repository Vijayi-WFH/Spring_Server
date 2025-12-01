package com.tse.core_application.dto.leave.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRemainingRequest {

    @NotNull
    private Long accountId;

    @NotNull
    private Short year;
}
