package com.tse.core_application.dto.github;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrossOrgGithubLinkRequest {
    private Long sourceOrgId;
    private Long targetOrgId;
}
