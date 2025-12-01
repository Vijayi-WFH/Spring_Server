package com.tse.core_application.dto.github;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkItemGithubBranchRequest {
    private Long workItemId;
    private String repoId;
    private String baseBranchName;
    private String newBranchName;
}
