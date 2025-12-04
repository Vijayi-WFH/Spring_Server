package com.tse.core.model;

import lombok.Getter;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {

    public static final String SCHEMA_NAME = "tse";
    public static final Integer TEAM_TYPE_ID = 5;
    public static final Integer TASK_TYPE_ID = 6;
    public static final Integer HOLIDAY_TYPE_ID = 9;

    public static final float LUNCH_HOUR =1;

    public static final Integer DEFAULT_WORK_FLOW_TYPE_ID = 3;

    public static final Integer TEAM = 5;

    public static class Leave{

        public static final String DEFAULT_LEAVE_POLICY_TITLE = "Default Leave Policy";
        public static final Float DEFAULT_INITIAL_LEAVES = 12f;
        public static final Boolean DEFAULT_IS_LEAVE_CARRY_FORWARD = false;
        public static final Float DEFAULT_MAX_LEAVE_CARRY_FORWARD = 0f;
        public static final String WAITING_APPROVAL_LEAVE_APPLICATION_STATUS = "WAITING_APPROVAL";

        public static final String WAITING_CANCEL_LEAVE_APPLICATION_STATUS = "WAITING_CANCEL";
        public static final String CANCEL_LEAVE_APPLICATION_STATUS = "CANCELLED";

        public static final String APPROVED_LEAVE_APPLICATION_STATUS = "APPROVED";

        public static final String REJECTED_LEAVE_APPLICATION_STATUS = "REJECTED";
        public static final String CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS = "CANCELLED AFTER APPROVAL";
        public static final String LEAVE_APPLICATION_EXPIRED_STATUS = "APPLICATION EXPIRED";
        public static final String CONSUMED_LEAVE_APPLICATION_STATUS = "CONSUMED";


        public static final Short WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID = 1;
        public static final Short WAITING_CANCEL_LEAVE_APPLICATION_STATUS_ID = 2;
        public static final Short APPROVED_LEAVE_APPLICATION_STATUS_ID = 3;
        public static final Short REJECTED_LEAVE_APPLICATION_STATUS_ID = 4;
        public static final Short CANCELLED_LEAVE_APPLICATION_STATUS_ID = 5;

        public static final Short CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID = 6;
        public static final Short LEAVE_APPLICATION_EXPIRED_STATUS_ID = 7;

        public static final Short CONSUMED_LEAVE_APPLICATION_STATUS_ID = 8;

        // Claude change: PT-14409 - Added DELETED status for consumed leave deletion feature
        // This status is used when Org Admin deletes a consumed leave with mandatory reason
        public static final String DELETED_LEAVE_APPLICATION_STATUS = "DELETED";
        public static final Short DELETED_LEAVE_APPLICATION_STATUS_ID = 9;

        public static final List<Short> APPROVER_LEAVE_APPLICATION_STATUS = List.of((short) 3,(short)4,(short)5);
        public static final Short TIME_OFF_LEAVE_TYPE_ID = 1;
        public static final Short SICK_LEAVE_TYPE_ID = 2;

        public static final Short LEAVE_FOR_A_DAY = 1;
        public static final Short MULTIPLE_DAYS_LEAVE = 2;
        public static final Short SICK_LEAVE_FOR_A_DAY=4;
        public static final Short SICK_LEAVE_FOR_MULTIPLE_DAYS=5;
        public static final Short HALF_DAY_SICK_LEAVE = 6;
        public static final Short HALF_DAY_TIME_OFF_LEAVE = 3;
        public static final List<Short> SICK_LEAVES = List.of((short)4,(short)5,(short)6);
        public static final List<Short> HALF_DAY_LEAVES = List.of((short)3,(short)6);
    }

    public static class HalfDayLeaveType{
        public static final Integer FIRST_HALF = 1;
        public static final Integer SECOND_HALF = 2;
    }

    public static List<Integer> halfDayLeaveTypeList = List.of(HalfDayLeaveType.FIRST_HALF, HalfDayLeaveType.SECOND_HALF);

    public static final Map<Short,String> leaveSelectionType=new HashMap<>()
    {{
        put((short) 1,"Leave for a day");
        put((short) 2,"Multiple days leave");
        put((short) 4,"Sick leave for a day");
        put((short) 5,"Sick leave for multiple days");
        put((short) 6,"Half day sick leave");
        put((short) 3,"Half day time off leave");
    }};

    public static class EntityTypes {
        public static final int USER = 1;
        public static final int ORG = 2;
        public static final int BU = 3;
        public static final int PROJECT = 4;
        public static final int TEAM = 5;
        public static final int TASK = 6;
        public static final int MEETING = 7 ;
        public static final int LEAVE = 8;
        public static final int HOLIDAY = 9;
    }

    public static class TaskTypes {
        public static final int TASK = 1;
        public static final int PARENT_TASK = 2;
        public static final int CHILD_TASK = 3;
        public static final int BUG_TASK = 4;
        public static final int EPIC = 5;
        public static final int INITIATIVE = 6;
        public static final int RISK = 7;
        public static final int PERSONAL_TASK = 8;
    }

    public static final Integer DEFAULT_OFFICE_MINUTES = 540;

    public static final LocalTime defaultLeaveRequestorCancelTime = LocalTime.of(11,0,0);


    @Getter
    public static enum LeaveRequesterDate {
        FROMDATE(1, "fromDate"),
        TODATE(2, "toDate");

        private final Integer typeId;
        private final String type;

        LeaveRequesterDate(Integer typeId, String type) {
            this.typeId = typeId;
            this.type = type;
        }

    }

    public static final List<Integer> defaultOffDays = List.of(6,7);

    public static final List<Integer> rolesToViewPeopleOnLeave = List.of(11, 12, 14, 15, 102, 112, 132);
}


