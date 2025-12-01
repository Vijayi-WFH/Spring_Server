package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RemoveTeamMemberRequest {
    private String email;
    private String roleName;
    private Long teamId;
    private List<TaskIdAssignedTo> taskIdAssignedToList;
}
