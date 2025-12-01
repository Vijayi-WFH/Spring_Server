package com.tse.core_application.dto.github;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemGithubBranchDetails {
    private String branchSha;
    private String branchName;
    private String baseBranchSha;
    private String baseBranch;
    private String repoId;
    private String repoName;
    private String lastCommitHash;
    private String branchLink;
    private LocalDateTime createdDateTime;
    private List<CommitDto> commits;
    private List<PullRequestDto> pullRequests;
}
