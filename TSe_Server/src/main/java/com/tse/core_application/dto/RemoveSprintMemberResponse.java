package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RemoveSprintMemberResponse {
    private Long sprintId;
    private List<TaskForBulkResponse> successList;
    private List<TaskForBulkResponse> failureList;
    private String message;
}
