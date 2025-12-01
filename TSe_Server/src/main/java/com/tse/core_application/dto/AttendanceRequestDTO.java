package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AttendanceRequestDTO {
    @NotNull(message = ErrorConstant.ORG_ID_ERROR)
    private Long orgId;
    private Long projectId;
    private Long teamId;
    @NotNull(message = ErrorConstant.Task.ACCOUNT_ID)
    private List<Long> accountIds;
    private LocalDate startDate;
    private LocalDate endDate;
}
