package com.tse.core_application.dto.geo_fence.punch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO for creating a new punch request.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PunchRequestCreateDto {

    @NotNull(message = "entityTypeId is required")
    @JsonProperty("entityTypeId")
    private Integer entityTypeId; // 1=USER, 2=ORG, 4=PROJECT, 5=TEAM

    @NotNull(message = "entityId is required")
    @JsonProperty("entityId")
    private Long entityId;

    @NotNull(message = "requesterAccountId is required")
    @JsonProperty("requesterAccountId")
    private Long requesterAccountId;

    @NotNull(message = "requestedDateTime is required")
    @JsonProperty("requestedDateTime")
    private LocalDateTime requestedDateTime;

    @NotNull(message = "respondWithinMinutes is required")
    @JsonProperty("respondWithinMinutes")
    private Integer respondWithinMinutes;
}
