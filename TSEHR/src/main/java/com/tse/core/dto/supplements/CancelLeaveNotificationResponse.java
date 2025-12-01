package com.tse.core.dto.supplements;

import com.tse.core.model.leave.LeaveApplication;
import lombok.Data;

@Data
public class CancelLeaveNotificationResponse {
    private Boolean sendNotification;
    private Long notifyToAccountId;
    private Boolean modifyCapacity;
    private LeaveApplication leaveApplication;
}
