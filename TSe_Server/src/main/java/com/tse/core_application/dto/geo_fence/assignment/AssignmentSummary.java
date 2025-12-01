package com.tse.core_application.dto.geo_fence.assignment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssignmentSummary {
    private int added = 0;
    private int removed = 0;
    private int updatedDefault = 0;
    private int noops = 0;
    private int errors = 0;

    public void incrementAdded() {
        added++;
    }

    public void incrementRemoved() {
        removed++;
    }

    public void incrementUpdatedDefault() {
        updatedDefault++;
    }

    public void incrementNoops() {
        noops++;
    }

    public void incrementErrors() {
        errors++;
    }
}
