package com.tse.core_application.dto;

import lombok.Data;

@Data
public class DeleteProjectResponse {
    String message;
    TeamListForBulkResponse teamListForBulkResponse;
}
