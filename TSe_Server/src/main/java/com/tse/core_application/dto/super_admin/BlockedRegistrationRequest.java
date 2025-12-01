package com.tse.core_application.dto.super_admin;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class BlockedRegistrationRequest {
    @NotNull(message = ErrorConstant.USERNAME_ERROR)
    @Size(max = 70, message = ErrorConstant.EMAIL_LENGTH)
    private String email;

    @NotNull(message = ErrorConstant.ORG_NAME_ERROR)
    private String organizationName;

    private Boolean isDeleted = false;

}
