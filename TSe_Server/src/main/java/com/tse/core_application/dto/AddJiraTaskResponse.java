package com.tse.core_application.dto;

import lombok.Data;

import java.util.List;

@Data
public class AddJiraTaskResponse {
    List<JiraTaskBulkResponse> successList;
    List<JiraTaskBulkResponse> failureList;
}
