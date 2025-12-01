package com.tse.core_application.dto.capacity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSprintCapacityDetails {
    private Long sprintId;
    private UserCapacityDetail userCapacityDetails;
    private List<CapacityTaskDetails> taskDetails;
}
