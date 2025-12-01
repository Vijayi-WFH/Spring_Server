package com.tse.core_application.dto.conversations;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupByEntityUsersRequest {

    private Long entityId;
    private Integer entityTypeId;
    private List<Long> usersIds;
}
