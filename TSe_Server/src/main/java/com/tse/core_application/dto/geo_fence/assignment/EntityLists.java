package com.tse.core_application.dto.geo_fence.assignment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EntityLists {
    private List<AssignedEntity> users = new ArrayList<>();
    private List<AssignedEntity> teams = new ArrayList<>();
    private List<AssignedEntity> projects = new ArrayList<>();
    private List<AssignedEntity> orgs = new ArrayList<>();
}
