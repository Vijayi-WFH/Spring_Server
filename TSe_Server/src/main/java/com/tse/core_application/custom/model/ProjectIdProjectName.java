package com.tse.core_application.custom.model;


import lombok.Value;

@Value
public class ProjectIdProjectName {

    Long projectId;
    String projectName;
    Boolean isDeleted;

    public ProjectIdProjectName(Long projectId, Object projectName, Boolean isDeleted){
        this.projectId = projectId;
        this.projectName = (String) projectName;
        this.isDeleted = isDeleted;
    }

}
