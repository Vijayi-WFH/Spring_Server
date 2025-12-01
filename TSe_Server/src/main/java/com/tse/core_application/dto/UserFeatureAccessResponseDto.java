package com.tse.core_application.dto;

import com.tse.core_application.model.Action;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserFeatureAccessResponseDto {

        private Long userFeatureAccessId;

        private Long userAccountId;

        private Integer entityTypeId;

        private Long entityId;

        private List<ActionResponseDto> actionList;

        private LocalDateTime createdDateTime;

        private LocalDateTime updatedDateTime;

        private Long orgId;

        private Integer departmentTypeId;
    }

