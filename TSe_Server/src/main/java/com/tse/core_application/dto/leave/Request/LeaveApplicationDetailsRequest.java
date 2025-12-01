package com.tse.core_application.dto.leave.Request;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class LeaveApplicationDetailsRequest {

    @NotNull(message = ErrorConstant.FROM_DATE)
    private LocalDate fromDate;
    @NotNull(message = ErrorConstant.TO_DATE)
    private LocalDate toDate;
    @NotNull(message = ErrorConstant.ORG_ID_ERROR)
    private Long orgId;
    private Boolean sortOnName;
    private List<Long> approverAccountIdList;
    private List<Short> leaveStatusIdList;
    private List<Long> accountIdList;      // Frontend doesn’t have to send this

}