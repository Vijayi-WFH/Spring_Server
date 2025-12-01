package com.tse.core_application.dto.AiMLDtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
public class AiWorkItemDescResponse {

    private Long taskId;
    private String taskNumber;
    private String taskTitle;
    private String taskDesc;
    private Long orgId;
    private Long teamId;
    private Long projectId;
    private String teamName;
    private String assignedEmail;
    private Integer taskTypeId;
    private LocalDateTime createDateTime;
    private Boolean isAdd;
    private Float score;

    public AiWorkItemDescResponse(Long taskId, String taskNumber, String taskTitle, Object taskDesc, Long orgId,
                                  Long teamId, Long projectId, Object teamName, Object email, Integer taskTypeId, LocalDateTime createDateTime) {
        this.taskId = taskId;
        this.taskNumber = taskNumber;
        this.taskTitle = taskTitle;
        this.taskDesc = (String) taskDesc;
        this.orgId = orgId;
        this.teamId = teamId;
        this.projectId = projectId;
        this.teamName = (String) teamName;
        this.assignedEmail = (String) email;
        this.taskTypeId = taskTypeId;
        this.createDateTime = createDateTime;
    }

    public AiWorkItemDescResponse(String taskTitle, String taskDesc, Long teamId) {
        this.taskTitle = taskTitle;
        this.taskDesc = taskDesc;
        this.teamId = teamId;
    }
}
