package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class AlertRequest {

    private String alertTitle;

    @NotNull(message = ErrorConstant.Alert.ALERT_REASON)
    @Size(min = 3, max = 255, message = ErrorConstant.Alert.ALERT_REASON_LENGTH)
    private String alertReason;

    private Long accountIdSender;

    @NotNull(message = ErrorConstant.Alert.ALERT_TYPE)
    private String alertType;

    private String associatedTaskNumber;

    private Long associatedTaskId;

    @NotNull(message = ErrorConstant.Alert.RECEIVER_ACCOUNT)
    private Long accountIdReceiver;

    @NotNull(message = ErrorConstant.Alert.TEAM_ID)
    private Long teamId;

    @NotNull(message = ErrorConstant.Alert.PROJECT_ID)
    private Long projectId;

    @NotNull(message = ErrorConstant.Alert.ORG_ID)
    private Long orgId;
}
