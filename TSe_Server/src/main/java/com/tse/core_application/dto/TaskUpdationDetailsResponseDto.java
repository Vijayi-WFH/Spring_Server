package com.tse.core_application.dto;

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
public class TaskUpdationDetailsResponseDto {
    String notificationType;
    LocalDateTime updatedDateTime;
    EmailFirstLastAccountIdIsActive updatedBy;
    Long commentLogId;
    Long commentId;
    String comment;
    List<EmailFirstLastAccountIdIsActive> taggedMentionUsers;
    EmailFirstLastAccountIdIsActive assigneeUser;
    Long taskId;
    String taskNumber;
    Long teamId;
    Long projectId;
    Long orgId;
    String payLoad;
    String oldValue;
    String newValue;
}
