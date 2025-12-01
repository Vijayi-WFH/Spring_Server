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
public class LinkedGithubResponse {
    private Long userId;
    private String firstName;
    private String lastName;
    private Boolean isLinked;
    private String message;
    private String githubUserName;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;
}
