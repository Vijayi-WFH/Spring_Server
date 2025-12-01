package com.tse.core_application.dto.geo_fence.userfence;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an effective fence available to a user with all contributing sources.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EffectiveFenceDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("locationKind")
    private String locationKind;

    @JsonProperty("siteCode")
    private String siteCode;

    @JsonProperty("tz")
    private String tz;

    @JsonProperty("centerLat")
    private Double centerLat;

    @JsonProperty("centerLng")
    private Double centerLng;

    @JsonProperty("radiusM")
    private Integer radiusM;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("sources")
    private List<SourceRef> sources = new ArrayList<>();
}
