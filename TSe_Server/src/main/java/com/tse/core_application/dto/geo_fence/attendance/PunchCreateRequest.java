package com.tse.core_application.dto.geo_fence.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phase 6b: Request body for POST /api/orgs/{orgId}/attendance/punch
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PunchCreateRequest {

    @JsonProperty("accountId")
    private Long accountId;

    @JsonProperty("eventKind")
    private String eventKind; // "CHECK_IN", "CHECK_OUT", "BREAK_START", "BREAK_END"

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("lon")
    private Double lon;

    @JsonProperty("accuracyM")
    private Double accuracyM;

    @JsonProperty("clientLocalTs")
    private String clientLocalTs; // ISO-8601 timestamp

    @JsonProperty("clientTz")
    private String clientTz; // e.g., "America/New_York"

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;
}
