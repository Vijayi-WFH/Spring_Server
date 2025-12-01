package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RemoveHrRoleDto {
    @NotNull(message = ErrorConstant.FeatureAccess.FEATURE_ACCESS_ID)
    private Long userFeatureAccessId;
}
