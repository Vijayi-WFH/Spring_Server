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
public class LoadedCapacityRatioUpdateRequest {
    private Long sprintId;
    private List<UserLoadedCapacityUpdate> updates;
}
