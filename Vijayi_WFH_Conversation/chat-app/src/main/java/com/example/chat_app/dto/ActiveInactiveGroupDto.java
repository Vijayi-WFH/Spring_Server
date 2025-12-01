package com.example.chat_app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActiveInactiveGroupDto {
    private Long groupId;
    private String name;
    private Long orgId;
    private Boolean isActive;
}
