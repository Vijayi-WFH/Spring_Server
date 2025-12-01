package com.tse.core_application.dto.project;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserAllProjectResponseWithOrg {
    private Long orgId;
    private String orgName;
    private List<UserAllProjectResponseWithBu> userAllProjectResponseWithBuList;
}
