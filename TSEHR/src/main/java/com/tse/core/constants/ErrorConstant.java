package com.tse.core.constants;

public class ErrorConstant {

    private ErrorConstant() {}

    public static final String PRIMARY_EMAIL_ERROR = "Primary email is missing.";
    public static final String IS_PRIMARY_EMAIL_ERROR = "Is primary email is missing.";
    public static final String EMAIL_CHECK_ERROR = "Email should be in correct format";
    public static final String FIRST_NAME_ERROR = "First name is missing.";
    public static final String LAST_NAME_ERROR = "Last name is missing.";
    public static final String GENDER_ERROR = "Gender is missing.";
    public static final String AGE_RANGE_ERROR = "Age range is missing.";
    public static final String HIGHEST_EDUCATION_ERROR = "Highest education is missing";
    public static final String DEVICE_OS_ERROR = "Device OS is missing.";
    public static final String DEVICE_OS_VER_ERROR = "Device OS version is missing.";
    public static final String DEVICE_MAKE_ERROR = "Device make is missing.";
    public static final String DEVICE_MODEL_ERROR = "Device model is missing.";
    public static final String DEVICE_UNIQUE_ID_ERROR = "Device unique id is missing.";
    public static final String ORG_NAME_ERROR = "Organization name is missing.";
    public static final String MIN_MAX_ORG_NAME = "Organization name must be between 2 to 25 characters long";
    public static final String USERNAME_ERROR = "Username is missing.";
    public static final String OTP_ERROR = "Otp is missing.";
    public static final String OTP_MISMATCH_ERROR = "Otp mismatched";
    public static final String USER_ALREADY_EXISTS="User Already Exists";
    public static final String Country_Error = "Country is missing";
    public static final String ENTITY_DETAILS = "Entity details are missing.";




    public static final class Task {
        public static final String TASK_TITLE = "task title is missing";
        public static final String TASK_DESC = "task description is missing";
        public static final String TASK_WORKFLOW_ID = "task workflow id is missing";
        public static final String fk_WORK_FLOW_TASK_STATUS_ID = "workflow task status id is missing";
        public static final String CURRENT_ACTIVITY_INDICATOR = "current activity indicator is missing";
        public static final String fk_TEAM_ID = "team id is missing";
        public static final String fk_ORG_ID = "organization id is missing";
        public static final String fk_ACCOUNT_ID = "account id is missing";
        public static final String  TITLE_LIMIT = "Title should be between 3 and 70 characters";
        public static final String DESC_LIMIT = "Description Length should be between 3 and 5000 characters";
        public static final String TASK_TYPE_ID = "task type id must not be null";
        public static final String REASON_CRITERIA = "Reason should be a minimum of 2 characters and less than 250 character";
        public static final String ACCOUNT_ID = "account id is missing";

    }

    public static final class Leave{
        public static final String ACCOUNT_ID_ERROR ="Account id can not be null or empty";
        public static final String ORG_ID_ERROR ="Organization is a mandatory field";
        public static final String INITIAL_LEAVE_ERROR ="Initial leave can not be null or empty";
        public static final String IS_LEAVE_CARRY_FORWARD_ERROR ="Is leave carry forward can not be null or empty";
        public static final String MAX_LEAVES_CARRY_FORWARD_ERROR ="Max leave carry forward can not be null or empty";
        public static final String LEAVE_TYPE_ID_ERROR = "Leave type can not be null or empty.";
        public static final String LEAVE_POLICY_ID = "Leave policy id is a mandatory field";
    }
}
