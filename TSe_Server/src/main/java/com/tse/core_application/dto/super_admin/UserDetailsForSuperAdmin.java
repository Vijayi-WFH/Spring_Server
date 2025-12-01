package com.tse.core_application.dto.super_admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsForSuperAdmin {
    private String email;
    private Long accountId;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private Long orgId;
    private String orgName;
    private Boolean isDisabledBySams;
    private Integer deactivatedByRole;
    private Long deactivatedByAccountId;
}
