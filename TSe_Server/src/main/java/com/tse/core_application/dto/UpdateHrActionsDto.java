package com.tse.core_application.dto;
import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.UniqueElements;

import javax.validation.constraints.NotNull;
import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateHrActionsDto {
    @NotNull(message = ErrorConstant.FeatureAccess.FEATURE_ACCESS_ID)
        private Long userFeatureAccessId;

    @NotNull(message = ErrorConstant.FeatureAccess.ACTIONS)
    @UniqueElements(message = "Duplicate actions are not allowed.")
    private List<Integer> actionList;
    }


