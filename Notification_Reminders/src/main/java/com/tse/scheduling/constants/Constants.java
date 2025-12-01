package com.tse.scheduling.constants;

public class Constants {
    public static final String meetingRoot ="/meeting";
    public static final String reminder = "/reminder";
    public static final String getStartMeetingConfirmation = "/startMeetingConfirmation";
    public static final String getEndMeetingConfirmation = "/endMeetingConfirmation";

    public static final String leaveRoot ="/leave";

    public static final String getLeaveRemainingReset="/leaveRemainingReset";
    public static final String getLeaveRemainingMonthlyUpdate="/leaveRemainingMonthlyUpdate";

    public static final String timesheetRoot = "/timesheet";
    public static final String timesheetPreReminder = "/timesheetPreReminder";
    public static final String timesheetPostReminder = "/timesheetPostReminder";
    public static final String timesheetBeforeOfficeReminder = "/timesheetBeforeOfficeReminder";
    public static final String fillHolidaysTimesheet = "/fillHolidaysTimesheet";

    public static final String notificationRoot = "/notification";
    public static final String deleteOldNotifications = "/deleteOldNotifications";

    public static final String blockedTaskRoot = "/blockedTask";
    public static final String getBlockedTaskReminder = "/blockedTaskReminder";
    public static final String userReminder = "/userReminder";

    public static final String alertRoot = "/alert";
    public static final String dependencyAlert = "/dependencyAlert";
    public static final String sendLeaveApprovalReminder = "/sendLeaveApprovalReminder";
    public static final String expireLeaveApplications = "/expireLeaveApplications";
    public static final String deleteAlerts = "/deleteAlerts";

    public static final String changeLeaveStatusToConsumed = "/changeLeaveStatusToConsumed";

    public static final String sprintRoute ="/sprint";

    public static final String sendSprintTasksMail = "/sendSprintTasksMail";

    public static final String geoFenceRoute = "/geo-fencing";
    public static final String notifyBeforeShiftStart = "/notifyBeforeShiftStart";
    public static final String autoCheckout = "/autoCheckout";
    public static final String missedPunch = "/missedPunch";

    public static final String retryFailedAiRegistration = "/ai/retryFailedUserRegistration";
}
