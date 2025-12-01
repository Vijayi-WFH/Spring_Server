package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JiraProjectResponse {
    private String projectId;
    private String projectCode;
    private String projectName;
    private String projectTypeKey;
    private String selfUrl;
    private String avatarUrl;
    private String expand;
}
