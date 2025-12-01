package com.tse.core_application.dto.geo_fence.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * Request body for POST /api/orgs/{orgId}/attendance/punched
 * Used when a supervisor fulfills a punch request on behalf of an employee.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PunchedEventRequest {

    @NotNull(message = "Account ID is required")
    @JsonProperty("accountId")
    private Long accountId;

    @NotNull(message = "Punch Request ID is required")
    @JsonProperty("punchRequestId")
    private Long punchRequestId;

    @NotNull(message = "Latitude is required")
    @JsonProperty("lat")
    private Double lat;

    @NotNull(message = "Longitude is required")
    @JsonProperty("lon")
    private Double lon;

    @JsonProperty("accuracyM")
    private Double accuracyM;
}
