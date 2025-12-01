package com.tse.core_application.dto;

import lombok.*;

import java.util.HashMap;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserDetailsResponse {
    private HashMap<String, Object> user;
}
