package com.tse.core.custom.model;

import lombok.Value;

@Value
public class ProjectIdProjectName {

    Long projectId;
    String projectName;

    public ProjectIdProjectName(Long projectId, Object projectName){
        this.projectId = projectId;
        this.projectName = (String) projectName;
    }

}
