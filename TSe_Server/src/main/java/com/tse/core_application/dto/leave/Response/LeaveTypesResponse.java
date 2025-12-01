package com.tse.core_application.dto.leave.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeaveTypesResponse {
    private short leaveTypeId;
    private String leaveTypeName;
}
