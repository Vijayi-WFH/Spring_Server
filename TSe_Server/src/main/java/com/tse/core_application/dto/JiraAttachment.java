package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class JiraAttachment {
    private LocalDateTime created;
    private String uploaderJiraId;
    private String fileName;
    private String url;

}
