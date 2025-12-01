package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntityMembersAvailabilityResponse {
    private Integer numberOfMembersOnLeave;
    private Integer numberOfMembersAvailable;
}
