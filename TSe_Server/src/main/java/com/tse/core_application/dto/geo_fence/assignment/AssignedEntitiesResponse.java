package com.tse.core_application.dto.geo_fence.assignment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignedEntitiesResponse {
    private Long fenceId;
    private EntityLists assigned;
    private EntityLists unassigned;
    private EntityCounts count;
}
