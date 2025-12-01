package com.tse.core_application.dto;

import com.tse.core_application.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
public class TeamPreferenceRequest {

    private Long entityPreferenceId;
    @NotNull(message = Constants.EntityPreference.ENTITY_TYPE_ID)
    private Integer entityTypeId;
    @NotNull(message = Constants.EntityPreference.ENTITY_ID)
    private Long entityId;

    private Integer bufferTimeToStartSprintEarly;
}
