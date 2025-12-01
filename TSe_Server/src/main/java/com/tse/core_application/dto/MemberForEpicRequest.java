package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MemberForEpicRequest {
    private Long projectId;
    private List<Long> teamIdList;
}
