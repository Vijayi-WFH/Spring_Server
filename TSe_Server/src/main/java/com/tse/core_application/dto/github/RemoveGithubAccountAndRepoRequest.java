package com.tse.core_application.dto.github;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class RemoveGithubAccountAndRepoRequest {
    @NotNull(message = ErrorConstant.Github.PREF_ID_REQUIRED)
    private Long githubAccountAndRepoPreferenceId;
}
