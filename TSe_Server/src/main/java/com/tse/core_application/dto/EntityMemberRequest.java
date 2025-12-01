package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
public class EntityMemberRequest {
    @NotNull(message = ErrorConstant.InviteError.ENTITY_TYPE_ID)
    private Integer entityTypeId;

    @NotNull(message = ErrorConstant.InviteError.ENTITY_ID)
    private Long entityId;

    @NotNull(message = ErrorConstant.RecurTask.SELECTED_DATE)
    private LocalDateTime todaysDate;
}
