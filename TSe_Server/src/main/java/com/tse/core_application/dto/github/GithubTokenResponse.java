package com.tse.core_application.dto.github;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GithubTokenResponse {
    private String access_token;
    private String token_type;
    private String scope;
}
