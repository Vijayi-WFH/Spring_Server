package com.tse.core_application.dto.geo_fence.assignment;

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
public class AssignFenceRequest {

    @NotNull
    private Long fenceId;

    private List<EntityActionItem> add;

    private List<EntityActionItem> remove;

    private Long updatedBy;
}
