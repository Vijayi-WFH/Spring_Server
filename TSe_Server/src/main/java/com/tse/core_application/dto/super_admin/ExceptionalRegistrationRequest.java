package com.tse.core_application.dto.super_admin;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ExceptionalRegistrationRequest {

    @NotNull(message = ErrorConstant.USERNAME_ERROR)
    @Size(max = 70, message = ErrorConstant.EMAIL_LENGTH)
    private String email;

    private Boolean paidSubscription;

    private Boolean onTrial;

    private Integer maxOrgCount;

    private Integer maxBuCount;

    private Integer maxProjectCount;

    private Integer maxTeamCount;

    private Integer maxUserCount;

    private Long maxMemoryQuota;

}
