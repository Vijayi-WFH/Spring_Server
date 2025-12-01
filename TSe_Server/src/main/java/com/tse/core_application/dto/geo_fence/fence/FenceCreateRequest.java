package com.tse.core_application.dto.geo_fence.fence;

import com.tse.core_application.model.geo_fencing.fence.GeoFence;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FenceCreateRequest {

    @NotBlank
    @Size(min = 1, max = 120)
    private String name;

    @NotNull
    private GeoFence.LocationKind locationKind;

    private String siteCode;

    private String tz;

    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double centerLat;

    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double centerLng;

    @NotNull
    @Min(30)
    private Integer radiusM;

    private Long createdBy;
}
