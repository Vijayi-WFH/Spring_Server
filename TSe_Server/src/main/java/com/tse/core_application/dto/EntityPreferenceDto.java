package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EntityPreferenceDto {
    private Long entityId;
    private Integer entityTypeId;
    private Integer meetingEffortEditDuration;
    private Long accountIdWithHigherRoles;

    public EntityPreferenceDto(Integer entityTypeId, Long entityId, Long accountIdsWithHigherRoles) {
        this.entityId = entityId;
        this.entityTypeId = entityTypeId;
        this.accountIdWithHigherRoles = accountIdsWithHigherRoles;
    }
}
