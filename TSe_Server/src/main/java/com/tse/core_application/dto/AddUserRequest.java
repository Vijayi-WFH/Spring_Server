package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class AddUserRequest {
    @NotNull
    private String username;
    @NotBlank(message = ErrorConstant.OTP_ERROR)
    private String otp;
    @NotBlank(message = ErrorConstant.DEVICE_UNIQUE_ID_ERROR)
    private String deviceUniqueIdentifier;
}
