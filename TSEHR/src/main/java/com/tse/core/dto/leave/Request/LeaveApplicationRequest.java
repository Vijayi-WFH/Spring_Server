package com.tse.core.dto.leave.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
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

    private Boolean includeLunchTime;

    @NotNull
    private String leaveReason;

    private String approverReason;

    @NotNull
    private Long approverAccountId;

    @NotNull
    private String phone;

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
