package com.tse.core.dto.leave.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRemainingHistoryRequest {

    @NotNull
    private Long accountId;

    @NotNull
    private Short year;

    private LocalDate fromDate;
    private LocalDate toDate;
}
