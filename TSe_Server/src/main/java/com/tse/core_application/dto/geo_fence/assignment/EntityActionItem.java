package com.tse.core_application.dto.geo_fence.assignment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EntityActionItem {
    private Integer entityTypeId;
    private Long entityId;
    private Boolean makeDefault;
}
