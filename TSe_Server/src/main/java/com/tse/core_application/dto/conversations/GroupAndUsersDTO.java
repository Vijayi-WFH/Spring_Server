package com.tse.core_application.dto.conversations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GroupAndUsersDTO {
    Long groupId;
    List<Long> userIds;
}