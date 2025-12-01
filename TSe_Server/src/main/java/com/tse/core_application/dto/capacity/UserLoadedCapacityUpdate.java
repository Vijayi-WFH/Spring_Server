package com.tse.core_application.dto.capacity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserLoadedCapacityUpdate {
    private Long accountId;
    private Double newLoadedCapacityRatio;
}
