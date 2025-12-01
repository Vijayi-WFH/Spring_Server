package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class VerifyJiraUserRequest {
    @NotNull(message = ErrorConstant.ORG_ID_ERROR)
    private Long orgId;

    @NotNull(message = ErrorConstant.ORG_NAME_ERROR)
    private String orgName;

    @NotBlank(message = ErrorConstant.InviteError.EMAIL)
    @Email(message = ErrorConstant.InviteError.EMAIL_FORMAT)
    private String primaryEmail;

    @NotBlank(message = ErrorConstant.DEVICE_UNIQUE_ID_ERROR)
    private String deviceUniqueIdentifier;

    private String otp;

    private Boolean isVerify = true;
}
