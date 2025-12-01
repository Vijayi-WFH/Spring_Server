package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentsRequest {

    private Long taskId;
    private String labelToSearch;
    private String labelToExclude;
    private int commentsToGet;

}
