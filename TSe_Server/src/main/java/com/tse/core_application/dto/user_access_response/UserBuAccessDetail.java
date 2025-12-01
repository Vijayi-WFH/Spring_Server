package com.tse.core_application.dto.user_access_response;

import lombok.Data;

import java.util.List;

@Data
public class UserBuAccessDetail {
    private Long buId;
    private String buName;
    private Boolean isSelectable = false;
    private List<UserProjectAccessDetail> projects;
}
