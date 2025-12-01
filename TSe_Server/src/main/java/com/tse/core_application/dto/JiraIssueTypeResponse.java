package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class JiraIssueTypeResponse {
    private List<JiraIssueTypeMapping> jiraIssueTypeMappingList;
    private List<TaskTypeDTO> taskTypeDTOList;
}
