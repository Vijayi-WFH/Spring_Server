package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;

@Getter
@Setter
public class JiraTaskToCreate {

    @Column(nullable = false)
    private Long issueId;

    private String jiraTaskTitle;

    @Column(nullable = false)
    private Boolean isCreated = false;
}
