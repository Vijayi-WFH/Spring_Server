package com.tse.core_application.dto.leave.Request;

import com.tse.core_application.constants.ErrorConstant;
import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplicationRequest {

    private Long leaveApplicationId;

    @NotNull
    private Long accountId;

    @NotNull
    private Short leaveSelectionTypeId;

    @NotNull
    private LocalDate fromDate;

    private LocalTime fromTime;

    @NotNull
    private LocalDate toDate;

    private LocalTime toTime;

    @NotNull
    private Boolean includeLunchTime;

    @NotNull
    @Size(min = 3, max = 1000, message = ErrorConstant.Leave.LEAVE_REASON_LENGTH)
    private String leaveReason;

    @Size(min = 3, max = 1000, message = ErrorConstant.Leave.APPROVAL_REASON)
    private String approverReason;

    @NotNull
    private Long approverAccountId;

    @NotNull
    @Pattern(regexp = "^\\+?[0-9]{1,4}[-.\\s]?(\\(?\\d{1,4}\\)?)?[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}$",
            message = ErrorConstant.Leave.PHONE_NUMBER_LENGTH)
    private String phone;

    @Size(max = 1000, message = ErrorConstant.Leave.ADDRESS_LENGTH)
    private String address;

    private List<Long> notifyTo;

    private byte[] doctorCertificate;

    private String doctorCertificateFileName;

    private String doctorCertificateFileType;

    private Long doctorCertificateFileSize;
    private Float numberOfLeaveDays;
    private Boolean isAttachmentPresent;

    private Integer halfDayLeaveType;

    private LocalDate expiryLeaveDate;
}
