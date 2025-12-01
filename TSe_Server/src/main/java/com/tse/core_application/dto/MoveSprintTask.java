package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
public class MoveSprintTask {
    private Long previousSprintId;
    private Long taskId;
    private Long newSprintId;

    @Override
    public boolean equals(Object obj) {
        // Check if the object is the same reference
        if (this == obj) return true;

        // Check if the object is null or not the same class
        if (obj == null || getClass() != obj.getClass()) return false;

        // Cast the object to MoveSprintTask for comparison
        MoveSprintTask that = (MoveSprintTask) obj;

        // Compare fields that define equality
        return Objects.equals(previousSprintId, that.previousSprintId) &&
                Objects.equals(taskId, that.taskId) &&
                Objects.equals(newSprintId, that.newSprintId);
    }

    @Override
    public int hashCode() {
        // Use the fields to generate a consistent hash code
        return Objects.hash(previousSprintId, taskId, newSprintId);
    }
}
