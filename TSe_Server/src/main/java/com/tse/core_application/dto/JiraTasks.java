package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class JiraTasks {
    private String summary;
    private String description;
    private Long issueId;
    private String parentId;
    private String issueType;
    private String status;
//    private String projectKey;
//    private String projectName;
//    private String projectType;
    private String projectLead;
    private String projectLeadId;
//    private String projectDescription;
    private String priority;
//    private String resolution;
    private String assignee;
    private String assigneeId;
    private String reporter;
    private String reporterId;
    private String creator;
    private String creatorId;
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDateTime lastViewed;
    private LocalDateTime resolved;
    private LocalDateTime dueDate;
    private Integer votes;
//    private List<String> labels;
    private List<String> watchers;
    private List<String> watchersId;
    private List<LogWork> logWorkDetails;
    private Integer originalEstimate;
    private Integer remainingEstimate;
    private Integer timeSpent;
    private List<JiraAttachment> attachments = new ArrayList<>();
    private List<JiraComment> comments = new ArrayList<>();
}
