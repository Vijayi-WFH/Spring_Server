package com.tse.core_application.dto.geo_fence.fence;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for filtering geo-fences")
public class FenceFilterRequest {

    @Schema(description = "Filter by status: active, inactive, or both", example = "both", defaultValue = "both")
    private String status = "both";

    @Schema(description = "Search by fence name (case-insensitive)", example = "Main Office")
    private String fenceName;

    @Schema(description = "Filter by site code", example = "SITE001")
    private String siteCode;
}
