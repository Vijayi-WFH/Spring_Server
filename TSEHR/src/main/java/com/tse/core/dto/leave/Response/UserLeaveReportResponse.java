package com.tse.core.dto.leave.Response;

import com.tse.core.custom.model.AccountDetails;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLeaveReportResponse {
    private AccountDetails accountDetails;
    private Float sickLeaves;
    private Float timeOffLeaves;
    private LocalDateTime startLeavePolicyOn;
    private Boolean isDateEditable;
}
