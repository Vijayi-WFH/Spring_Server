package com.example.chat_app.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class GroupUpdateRequest implements Serializable {

    private String groupName;
    private String groupDesc;
    private Boolean isActive;
}
