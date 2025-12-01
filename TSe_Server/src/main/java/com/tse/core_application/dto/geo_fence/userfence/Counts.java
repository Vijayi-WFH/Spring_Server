package com.tse.core_application.dto.geo_fence.userfence;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregated counts of fence sources by entity type.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Counts {

    @JsonProperty("total")
    private int total;

    @JsonProperty("user")
    private int user;

    @JsonProperty("team")
    private int team;

    @JsonProperty("project")
    private int project;

    @JsonProperty("org")
    private int org;
}
