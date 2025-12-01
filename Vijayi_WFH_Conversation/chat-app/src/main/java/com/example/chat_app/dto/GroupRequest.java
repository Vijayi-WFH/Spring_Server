package com.example.chat_app.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class GroupRequest implements Serializable {

    private Long groupId;

    private String name;

    private String type;

    private List<UserRequest> users;

}
