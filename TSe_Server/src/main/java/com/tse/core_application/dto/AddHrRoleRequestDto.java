package com.tse.core_application.dto;
import com.tse.core_application.constants.ErrorConstant;
import lombok.*;

import java.util.List;
import com.tse.core_application.dto.*;
import org.hibernate.validator.constraints.UniqueElements;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public class AddHrRoleRequestDto {
        @NotNull(message = ErrorConstant.FeatureAccess.ORG_ID)
        private Long orgId;

        @NotNull(message = ErrorConstant.FeatureAccess.USER_ACCOUNT_ID)
        private Long userAccountId;

        @NotNull(message = ErrorConstant.FeatureAccess.ENTITY_TYPE_ID)
        private Integer entityTypeId;

        @NotNull(message = ErrorConstant.FeatureAccess.ACTIONS)
        private List< @Valid EntityActionDto> entityActions;
    }



