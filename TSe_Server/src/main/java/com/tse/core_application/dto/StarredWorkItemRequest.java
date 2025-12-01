package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StarredWorkItemRequest {
    private Long taskId;
    private Boolean isStarred;
}
