package com.tse.core_application.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeletedTeamReport {
    private String teamName;
    private Long teamId;
    private String teamCode;
    private EmailFirstLastAccountIdIsActive deletedBy;
    private LocalDateTime deletedOn;
}
