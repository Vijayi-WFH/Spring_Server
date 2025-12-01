package com.tse.core_application.dto.geo_fence.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Phase 6b: Response for POST /api/orgs/{orgId}/attendance/punch
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PunchResponse {

    @JsonProperty("eventId")
    private Long eventId;

    @JsonProperty("accountId")
    private Long accountId;

    @JsonProperty("eventKind")
    private String eventKind;

    @JsonProperty("eventSource")
    private String eventSource;

    @JsonProperty("tsUtc")
    private String tsUtc; // ISO-8601 timestamp

    @JsonProperty("fenceId")
    private Long fenceId;

    @JsonProperty("underRange")
    private Boolean underRange;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("verdict")
    private String verdict; // "PASS", "WARN", "FAIL"

    @JsonProperty("failReason")
    private String failReason;

    @JsonProperty("flags")
    private Map<String, Object> flags;
}
