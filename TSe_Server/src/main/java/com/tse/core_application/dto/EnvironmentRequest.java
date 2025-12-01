package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.validators.annotations.TrimmedSize;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentRequest {

    @TrimmedSize(min = 2, max = 70, message = ErrorConstant.CustomEnviornment.ENVIORNMENT_DISPLAY_SIZE)
    private String environmentDisplayName;

    @TrimmedSize(min = 2, max = 255, message = ErrorConstant.CustomEnviornment.ENVIORNMENT_DESCRIPTION_SIZE)
    private String environmentDescription;

    @NotNull(message = ErrorConstant.CustomEnviornment.ENTITY_TYPE_ID)
    private Integer entityTypeId;

    @NotNull(message = ErrorConstant.CustomEnviornment.ENTITY_ID)
    private Long entityId;

    @NotNull(message = ErrorConstant.CustomEnviornment.IS_ACTIVE)
    private Boolean isActive;
}
