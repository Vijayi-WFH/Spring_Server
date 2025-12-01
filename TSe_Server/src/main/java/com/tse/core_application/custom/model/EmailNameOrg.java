package com.tse.core_application.custom.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailNameOrg {

    String email;
    String org;
    String firstName;
    String lastName;
    Long accountId;
    Long teamId;
}
