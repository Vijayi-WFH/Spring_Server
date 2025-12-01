package com.tse.core_application.dto.geo_fence.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoFenceAttendanceDataRequest {
    @NotNull(message = "orgId is required")
    private Long orgId;

    @NotNull(message = "fromDate is required (format: yyyy-MM-dd)")
    private String fromDate;

    @NotNull(message = "toDate is required (format: yyyy-MM-dd)")
    private String toDate;

    private List<Long> accountIds;
}
