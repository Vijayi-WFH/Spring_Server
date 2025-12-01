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
public class PullRequestDto {
    private String title;
    private String status;
    private LocalDateTime createdDateTime;
    private LocalDateTime updatedDateTime;
    private LocalDateTime closedDateTime;
    private LocalDateTime mergedDateTime;
    private Boolean isMerged;
    private String url;
    private String pullRequestId;
    private String userLogin;
    private String closedBy;

    private List<String> labels;
    private List<String> assignees;
    private List<String> requestedReviewers;
    private List<String> approvedBy;

    private String authorName;
    private String authorAvatarUrl;
}
