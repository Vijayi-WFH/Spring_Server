package com.tse.core_application.dto.github;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class GithubRepoForPreferenceRequest {
    @NotNull(message = ErrorConstant.Github.GITHUB_REPOSITORY_PREFERENCE_ID)
    private Long githubRepositoryPreferenceId;

    @NotBlank(message = ErrorConstant.Github.GITHUB_REPOSITORY_NAME)
    @Size(min = 1, max = 100, message = ErrorConstant.Github.GITHUB_REPOSITORY_NAME_LENGTH)
    private String githubRepositoryName;

    @NotNull(message = ErrorConstant.Github.IS_ACTIVE)
    private Boolean isActive;
}
