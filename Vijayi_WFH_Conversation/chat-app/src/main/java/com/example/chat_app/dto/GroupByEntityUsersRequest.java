package com.example.chat_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupByEntityUsersRequest {

    private Long entityId;
    private Integer entityTypeId;
    private List<Long> usersIds;

}
