package com.tse.core_application.dto.user_access_response;

import lombok.Data;

import java.util.List;

@Data
public class UserOrgAccessStructureResponse {
    private Long orgId;
    private String orgName;
    private Boolean isSelectable = false;
    private List<UserBuAccessDetail> bu;

}
