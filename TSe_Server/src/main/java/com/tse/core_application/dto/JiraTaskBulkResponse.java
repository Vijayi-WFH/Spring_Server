package com.tse.core_application.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JiraTaskBulkResponse {
    private Long issueId;
    private String summary;
    private Long taskId;
    private String taskNumber;
    private Long teamId;
    private String message;
}
