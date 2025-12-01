package com.tse.core_application.dto.user_access_response;

import lombok.Data;

import java.util.List;

@Data
public class UserProjectAccessDetail {
    private Long projectId;
    private String projectName;
    private Boolean isSelectable = false;
    private List<UserTeamAccessDetail> teams;
}
