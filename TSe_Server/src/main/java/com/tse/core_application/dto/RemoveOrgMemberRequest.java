package com.tse.core_application.dto;

import lombok.Data;

import java.util.List;

@Data
public class RemoveOrgMemberRequest {
    private Long orgId;
    private Long accountId;
    private List<TaskIdAssignedTo> taskIdAssignedToList;
}
