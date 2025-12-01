package com.tse.core_application.dto.geo_fence.assignment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignFenceResult {
    private Long fenceId;
    private AssignmentSummary summary;
    private List<EntityResult> results;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
