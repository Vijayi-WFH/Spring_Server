package com.tse.core_application.custom.model;

import com.tse.core_application.constants.RelationDirection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependentTaskDetail {

    @NotNull
    private String relatedTaskNumber;

    @NotNull
    private Integer relationTypeId; // FS, FF, SS, SF

    @NotNull
    private RelationDirection relationDirection; // Whether the relatedTaskNumber is a predecessor or successor to the given task
}
