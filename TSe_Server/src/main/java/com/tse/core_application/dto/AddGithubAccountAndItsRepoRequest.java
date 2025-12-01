package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.validators.ValidGithubRepo;
import com.tse.core_application.validators.ValidGithubUsername;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class AddGithubAccountAndItsRepoRequest {
    @NotBlank(message = ErrorConstant.Github.GITHUB_USERNAME_REQUIRED)
    @Size(min = 1, max = 39, message = ErrorConstant.Github.GITHUB_USERNAME_SIZE)
    @ValidGithubUsername
    private String githubAccountUserName;

    @NotBlank(message = ErrorConstant.Github.GITHUB_REPO_REQUIRED)
    @Size(min = 1, max = 100, message = ErrorConstant.Github.GITHUB_REPO_SIZE)
    @ValidGithubRepo
    private String githubAccountRepoName;

    @NotNull(message = ErrorConstant.Github.ORG_ID_REQUIRED)
    private Long orgId;
}
