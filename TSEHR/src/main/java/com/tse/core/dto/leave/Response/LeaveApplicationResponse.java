package com.tse.core.dto.leave.Response;

import com.tse.core.dto.DoctorCertificate;
import com.tse.core.dto.supplements.EmailFirstLastAccountIdIsActive;
import com.tse.core.model.supplements.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplicationResponse {

    private Long leaveApplicationId;
    private Short leaveApplicationStatusId;
    private EmailFirstLastAccountIdIsActive applicantDetails;
    private String leaveType;

    private LocalDate fromDate;
    private LocalTime fromTime;

    private LocalDate toDate;
    private LocalTime toTime;
    private Boolean includeLunchTime;

    private String leaveReason;

    private String approverReason;

    private EmailFirstLastAccountIdIsActive approver;

    private String phone;

    private String address;

    private List<Long> notifyTo;

    private DoctorCertificate doctorCertificate;

    private Boolean isLeaveForHalfDay;
    private Float numberOfLeaveDays;

    private String leaveCancellationReason;
    private Short leaveTypeId;

    private Integer halfDayLeaveType;
    private LocalDate applicationDate;
}
