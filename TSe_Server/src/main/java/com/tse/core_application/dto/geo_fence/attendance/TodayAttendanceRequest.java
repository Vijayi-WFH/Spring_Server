package com.tse.core_application.dto.geo_fence.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodayAttendanceRequest {

    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotNull(message = "date is required")
    private String date; // yyyy-MM-dd format
}
