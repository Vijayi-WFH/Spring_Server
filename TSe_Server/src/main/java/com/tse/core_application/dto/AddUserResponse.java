package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class AddUserResponse {
    @NotNull
    private User managingUser;
    @NotNull
    private User managedUser;
    @NotNull
    private String token;
}
