package com.tse.core_application.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeletedProjectReport {
    private Long projectId;
    private String projectName;
    private EmailFirstLastAccountIdIsActive deletedBy;
    private LocalDateTime deletedOn;
}
