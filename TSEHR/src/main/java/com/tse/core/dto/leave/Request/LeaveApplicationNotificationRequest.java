package com.tse.core.dto.leave.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveApplicationNotificationRequest {

    private Boolean sendNotification = false;
    private Long leaveApplicationId;
    private String notificationFor;
    private List<Long> notifyTo;
    private String fromDate;
    private String fromTime;
    private String toDate;
    private String toTime;
    private Long applicantAccountId;
    private Long approverAccountId;
    private Boolean isSprintCapacityAdjustment;
}
