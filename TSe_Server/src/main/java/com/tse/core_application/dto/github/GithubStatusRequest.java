package com.tse.core_application.dto.github;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class GithubStatusRequest {
    @NotNull(message = ErrorConstant.Github.ORG_ID_REQUIRED)
    private Long orgId;
}
