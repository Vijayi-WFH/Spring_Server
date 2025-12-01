package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AddedSprintMemberResponse {
    private Long sprintId;
    private List<AccountDetailsForBulkResponse> successList;
    private List<AccountDetailsForBulkResponse> failureList;
    private String message;
}
