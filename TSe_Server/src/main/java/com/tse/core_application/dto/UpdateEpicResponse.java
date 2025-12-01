package com.tse.core_application.dto;

import com.tse.core_application.model.Epic;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class UpdateEpicResponse {
    EpicResponse epicResponse;
    List<TaskForBulkResponse> taskForBulkResponses;
    private String message;
    List<TaskForBulkResponse> successList;
    List<TaskForBulkResponse> failureList;
}
