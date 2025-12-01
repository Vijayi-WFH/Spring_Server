package com.tse.core_application.dto;

import com.tse.core_application.model.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MappedUserDetails {
    private UserAccount userAccount;
    private boolean isActive;
}
