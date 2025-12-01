package com.tse.core_application.model;

import lombok.Data;

import java.util.List;

@Data
public class ReactivateDeactivateUserRequest {

    private List<Long> accountIds;
    private String username;

}
