package com.tse.core_application.dto;

import com.tse.core_application.model.Sprint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AllSprintResponse {
    private List<SprintResponse> activeSprintList = new ArrayList<>();
    private List<SprintResponse> completedSprintList = new ArrayList<>();
    private List<SprintResponse> notStartedSprintList = new ArrayList<>();
}
