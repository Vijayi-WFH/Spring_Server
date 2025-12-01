package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class JiraComment {
    private LocalDateTime date;
    private String uploaderJiraId;
    private String message;
}
