package com.tse.core_application.dto;
import com.tse.core_application.model.UserAccount;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserActivateDeactivateDto {
    private Integer roleId;
    private Long activateDeactivatedByAccountId;
    private UserAccount userAccount;
    private Long orgId;
}
