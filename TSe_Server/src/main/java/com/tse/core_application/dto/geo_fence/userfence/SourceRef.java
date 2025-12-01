package com.tse.core_application.dto.geo_fence.userfence;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the source of a fence assignment (entity that contributed the fence).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SourceRef {

    @JsonProperty("entityTypeId")
    private Integer entityTypeId;

    @JsonProperty("entityId")
    private Long entityId;

    @JsonProperty("defaultForEntity")
    private Boolean defaultForEntity;
}
