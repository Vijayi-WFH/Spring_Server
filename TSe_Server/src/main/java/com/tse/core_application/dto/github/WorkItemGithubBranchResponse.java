package com.tse.core_application.dto.github;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemGithubBranchResponse {
    private Long workItemId;
    private String branchSha;
    private String branchName;
    private String baseBranchSha;
    private String baseBranchName;
    private String repoId;
    private String repoName;
    private String lastCommitHash;
    private String branchLink;
    private LocalDateTime createdDateTime;
}
