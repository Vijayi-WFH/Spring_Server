package com.tse.core_application.dto.org_response;

import lombok.Data;

import java.util.List;

@Data
public class ProjectDetail {
    private Long projectId;
    private String projectName;
    private List<TeamDetail> team;
}
