package com.tse.core_application.dto.leave.Request;

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
public class LeaveHistoryRequest {

    @NotNull
    private Long accountId;
    @NotNull
    private LocalDate fromDate;
    @NotNull
    private LocalDate toDate;
}
