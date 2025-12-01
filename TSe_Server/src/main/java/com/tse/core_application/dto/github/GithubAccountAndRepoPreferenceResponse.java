package com.tse.core_application.dto.github;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class GithubAccountAndRepoPreferenceResponse {
    private Long githubAccountAndRepoPreferenceId;
    private String githubAccountUserName;
    private String githubAccountRepoName;
    private Long orgId;
    private Boolean isActive;
    private LocalDateTime createdDateTime;
    private LocalDateTime updatedDateTime;
}
