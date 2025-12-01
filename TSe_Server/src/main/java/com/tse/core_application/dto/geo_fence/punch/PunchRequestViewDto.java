package com.tse.core_application.dto.geo_fence.punch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for viewing a punch request with computed fields.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PunchRequestViewDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("orgId")
    private Long orgId;

    @JsonProperty("entityTypeId")
    private Integer entityTypeId;

    @JsonProperty("entityId")
    private Long entityId;

    @JsonProperty("requesterAccountId")
    private Long requesterAccountId;

    @JsonProperty("requestedDateTime")
    private LocalDateTime requestedDateTime;

    @JsonProperty("respondWithinMinutes")
    private Integer respondWithinMinutes;

    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;

    @JsonProperty("state")
    private String state; // PENDING/FULFILLED/EXPIRED/CANCELLED

    @JsonProperty("activeNow")
    private Boolean activeNow; // computed: now in [requestedDateTime, expiresAt)

    @JsonProperty("secondsRemaining")
    private Long secondsRemaining; // if activeNow, else 0

    @JsonProperty("appliesToAccountIds")
    private List<Long> appliesToAccountIds = new ArrayList<>(); // optional: which accounts this applies to
}
