package com.tse.core_application.dto.user_access_response;

import lombok.Data;

@Data
public class UserTeamAccessDetail {
    private Long teamId;
    private String teamName;
    private Boolean isSelectable = false;
    private String teamCode;
}
