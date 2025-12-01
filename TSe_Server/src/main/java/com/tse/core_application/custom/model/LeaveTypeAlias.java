package com.tse.core_application.custom.model;

import lombok.Data;

@Data
public class LeaveTypeAlias {
    private String timeOffAlias;
    private String sickLeaveAlias;


    public LeaveTypeAlias (String timeOffAlias, String sickLeaveAlias) {
        this.timeOffAlias = timeOffAlias;
        this.sickLeaveAlias = sickLeaveAlias;
    }
}
