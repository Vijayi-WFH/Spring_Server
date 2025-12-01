package com.tse.core_application.dto;
import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BlockedByRequestDto {

    @NotNull(message = ErrorConstant.ENTITY_TYPE_ID)
    private Integer entityTypeId;

    @NotNull(message = ErrorConstant.ENTITY_ID)
    private Long entityId;

    private List<Integer> blockReasonTypeId;
}
