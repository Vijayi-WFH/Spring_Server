package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskAttachmentHistoryResponse {
    private Long taskId;
    private String modifiedBy;
    private LocalDateTime modifiedOn;
    private String message;
    private Boolean isFileAdded;
    private Long version;
}
