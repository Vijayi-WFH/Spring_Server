package com.example.chat_app.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {

    private Long accountId;
    private Long userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private Long orgId;
    private Boolean isActive;
    private String email;
    private Boolean isAdmin;
    private Boolean isDeleted;
    private Boolean isOrgAdmin;

    public UserDto(Long accountId, Long userId, String firstName, String lastName, Long orgId, Boolean isActive, String email) {
        this.accountId = accountId;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.orgId = orgId;
        this.isActive = isActive;
        this.email = email;
    }
}
