package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

import javax.validation.constraints.NotNull;
import java.util.List;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public class EntityActionDto {
        @NotNull(message = ErrorConstant.FeatureAccess.ENTITY_ID)
        private Long entityId;

        @NotNull(message = ErrorConstant.FeatureAccess.ACTIONS)
        @UniqueElements(message = "Duplicate actions are not allowed.")
        private List<Integer> actionList;
    }



