package com.example.chat_app.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GroupAndUsersDTO {
    Long groupId;
    List<Long> userIds;
}
