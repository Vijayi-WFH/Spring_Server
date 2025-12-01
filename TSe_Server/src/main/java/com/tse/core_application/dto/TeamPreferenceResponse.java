package com.tse.core_application.dto;

import com.tse.core_application.model.Constants;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class TeamPreferenceResponse {

    private Long entityPreferenceId;
    @NotNull(message = Constants.EntityPreference.ENTITY_TYPE_ID)
    private Integer entityTypeId;
    @NotNull(message = Constants.EntityPreference.ENTITY_ID)
    private Long entityId;

    private Integer bufferTimeToStartSprintEarly;
}
