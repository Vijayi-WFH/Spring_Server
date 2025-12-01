package com.tse.core_application.dto.project;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserAllProjectResponseWithBu {
    private Long buId;
    private String buName;
    List<ProjectResponse> projectResponseList;
}
