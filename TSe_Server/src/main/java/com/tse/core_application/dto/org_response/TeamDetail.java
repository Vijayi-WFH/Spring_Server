package com.tse.core_application.dto.org_response;

import lombok.Data;

import java.util.List;

@Data
public class TeamDetail {
    private Long teamId;
    private String teamName;
    private List<UserDetail> userDetail;
}
