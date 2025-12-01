package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RemoveSprintMemberRequest {
    private Long sprintId;
    private Long removedMemberAccountId;
    private List<TaskIdAssignedTo> taskIdAssignedToList;
}
