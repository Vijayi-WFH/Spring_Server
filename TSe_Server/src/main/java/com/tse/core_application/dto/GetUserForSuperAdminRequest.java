package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class GetUserForSuperAdminRequest {
    private String userName;
    private Boolean showPersonal;
    @NotNull(message = ErrorConstant.ORG_List_ERROR)
    private List<Long> organizationList;
}
