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
public class CommitDto {
    private String commitHash;
    private String message;
    private String authorName;
    private String authorLogin;
    private String authorAvatarUrl;
    private String url;
    private LocalDateTime commitDateTime;
}
