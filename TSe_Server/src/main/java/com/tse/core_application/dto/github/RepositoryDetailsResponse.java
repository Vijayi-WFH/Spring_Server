package com.tse.core_application.dto.github;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDetailsResponse {
    private String message;
    private List<GithubRepositoryDTO> githubRepositoryDTOList;
}
