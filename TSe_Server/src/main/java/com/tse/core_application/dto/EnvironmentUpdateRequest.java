package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.validators.annotations.TrimmedSize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EnvironmentUpdateRequest
{
    @NotNull(message = ErrorConstant.CustomEnviornment.CUSTOM_ENVIORNMENT_ID)
    private Integer customEnvironmentId;

    @TrimmedSize(min = 2, max = 70, message = ErrorConstant.CustomEnviornment.ENVIORNMENT_DISPLAY_SIZE)
    @NotNull(message = ErrorConstant.CustomEnviornment.ENVIORNMENT_DISPLAY_NAME)
    private String environmentDisplayName;

    @TrimmedSize(min = 2, max = 255, message = ErrorConstant.CustomEnviornment.ENVIORNMENT_DESCRIPTION_SIZE)
    private String environmentDescription;

    @NotNull(message = ErrorConstant.CustomEnviornment.IS_ACTIVE)
    private Boolean isActive;
}
