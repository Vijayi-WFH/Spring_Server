package com.tse.core_application.custom.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BUProject {

    private Long buId;
    private String buName;
    private List<ProjectTeam> projects;
}
