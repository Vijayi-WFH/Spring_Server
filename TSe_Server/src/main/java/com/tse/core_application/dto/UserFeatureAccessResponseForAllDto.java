package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserFeatureAccessResponseForAllDto {
    private Long userFeatureAccessId;

    private Long userAccountId;

    private Integer entityTypeId;

    private Long entityId;

    private String entityName;

    private List<ActionResponseDto> actionList;

    private LocalDateTime createdDateTime;

    private LocalDateTime updatedDateTime;

    private Long orgId;

    private Integer departmentTypeId;

    private String firstName;

    private String lastName;

    private String email;

}
