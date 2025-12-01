package com.tse.core_application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AllSprintResponseForGetAllSprint {
    private List<SprintResponseForGetAllSprints> activeSprintList = new ArrayList<>();
    private List<SprintResponseForGetAllSprints> completedSprintList = new ArrayList<>();
    private List<SprintResponseForGetAllSprints> notStartedSprintList = new ArrayList<>();
    private List<SprintResponseForGetAllSprints> deletedSprintList = new ArrayList<>();
}
