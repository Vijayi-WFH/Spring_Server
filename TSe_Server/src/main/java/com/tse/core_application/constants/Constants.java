package com.tse.core_application.constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class Constants {

    public static final String TOKEN_HASH = "tokens";

    private Constants() {
    }

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String BEARER = "Bearer";

    public static final String PROJECT_NAME = "PROJ_DFLT";

    public static final String BU_NAME = "BU_DFLT";

    public static final long UNASSIGNED_TASK_ACCOUNT_ID_CAPACITY = 0;

	public static class FormattedResponse {
		public static final String SUCCESS = "success";
		public static final String NOTFOUND = "not found";
        public static final String SERVER_ERROR = "server error!";

        public static final String NO_CONTENT = "NO_CONTENT";
        public static final String VALIDATION_ERROR = "Validation Error!";
        public static final String  FORBIDDEN = "Forbidden";
        public static final String WARNING = "WARNING";
        public static final String BAD_REQUEST = "BAD_REQUEST";
        public static final String UNAUTHORIZED = "UNAUTHORIZED";
        public static final Integer DEPENDENCY_VALIDATION_ERROR_CODE = 498;
        public static final String INVALID_TOKEN = "Invalid token";
        public static final Integer REFERENCE_MEETING_ERROR_CODE = 497;
        public static final Integer DUPLICATE_WORK_ITEM_ERROR_CODE = 496;
    }

    public static class SearchCriteriaConstants {
        public static final String FK_ACCOUNT_ID_ASSIGNED = "fkAccountIdAssigned";
        public static final String TASK_PRIORITY = "taskPriority";
        public static final String TASK_PROGRESS_SYSTEM = "taskProgressSystem";
        public static final String FK_WORKFLOW_TASK_STATUS = "fkWorkflowTaskStatus";
        public static final String TASK_EXP_START_DATE = "taskExpStartDate";
        public static final String TASK_EXP_END_DATE = "taskExpEndDate";
        public static final String TASK_ACTUAL_START_DATE = "taskActStDate";
        public static final String TASK_ACTUAL_END_DATE = "taskActEndDate";
        public static final String CRITICAL_PRIORITY_P0 = "P0";
        public static final String CRITICAL_PRIORITY_P1 = "P1";
        public static final String TASK_TYPE_ID = "taskTypeId";
        public static final Long FK_ACCOUNT_ID_NOT_ASSIGNED=0l;
    }

    public static class NotificationRemainder {
        public static final Integer MIN_NOTIFICATION_REMANIDER_DURATION = 0;
        public static final Integer MAX_NOTIFICATION_REMANIDER_DURATION = 1440;
    }

    public enum WorkStatus {
        FULL_TIME, PART_TIME, CONTRACTOR, ON_CALL
    }

    public enum WorkPattern {
        DAILY, WEEKLY, MONTHLY
    }

    public static class Descriptions {
        public static final String Gender_Description = "genderDescription";
        public static final String Education_Description = "educationDescription";
        public static final String AgeRange_Description = "ageRangeDescription";
    }

    public static class FileAttachmentStatus {
        public static final Character A = 'A';
        public static final Character D = 'D';
    }

    public static class FileAttachmentOptionIndicator {
        public static final String OPTION_INDICATOR_ALL = "All";
        public static final String OPTION_INDICATOR_SINGLE = "Single";
    }

    public static class Task_Note_Status {
        public static final Integer NOTE_DELETED = 1;
        public static final Integer NOTE_NOT_DELETED = 0;
    }

    public static class MeetingAttendeeInvitationStatus{
        public static final Integer ATTENDEE_INVITED_ID = 1;
        public static final Integer ATTENDEE_DISINVITED_ID = 0;
        public static final String ATTENDEE_INVITED = "invited";
        public static final String ATTENDEE_DISINVITED = "dis-invited";
    }

    public static class Meeting_Preferrences{
        public static final Integer PAST_MEETING_DAYS_LIMIT = 8;
        public static final Integer BUFFER_TIME_FOR_SCHEDULED_MEETING = 20; //in mins

    }

    public static class Sticky_Note_AccessType {
        public static final Integer PUBLIC_ACCESS = 1;
        public static final Integer PRIVATE_ACCESS = 0;
    }

    public static class Sticky_Note_ModifiedType {
        public static final Integer STICKY_NOTE_MODIFIED = 1;
        public static final Integer STICKY_NOTE_NOT_MODIFIED = 0;
    }

    public static class Sticky_Note_DeleteType {
        public static final Integer STICKY_NOTE_DELETED = 1;
        public static final Integer STICKY_NOTE_NOT_DELETED = 0;
    }

    public static class Meeting_Type_Indicator {
        public static Integer ONLINE = 1;
        public static Integer OFFLINE = 2;
        public static Integer HYBRID = 3;

    }

    public static class TaskUpdatedByIndicator_TaskHistoryTable {
        public static Integer TASK_UPDATED_BY_SYSTEM = 0;
        public static Integer TASK_UPDATED_BY_USER = 1;
    }
	public static class Task_DeliverablesDelivered_Status {
		public static final Integer DELIVERABLES_DELIVERED_DELETED = 1;
		public static final Integer DELIVERABLES_DELIVERED_NOT_DELETED = 0;
	}

    public static class ProjectType {
        public static final Integer DEFAULT_PROJECT = 1;
        public static final Integer USER_PROJECT = 2;
    }

    public static class TaskHistoryMapping_ActiveIndicator {
        public static Integer TASK_HISTORY_MAPPING_KEY_ACTIVE = 1;
        public static Integer TASK_HISTORY_MAPPING_KEY_NOT_ACTIVE = 0;
    }

    public static class Task_History_Columns_Mapping_Key {
        public static Integer MAPPING_KEY_ALL = 0;
        public static Integer MAPPING_KEY_OTHERS = 10;
    }

    public static final HashMap<String, String> TaskHistory_Column_Name = new HashMap<>() {{
        put("taskTitle", "Title");
        put("taskDesc", "Description");
        put("taskExpStartDate", "Expected Start Date");
        put("taskExpEndDate", "Expected End Date");
        put("taskActStDate", "Actual Start Date");
        put("taskActEndDate", "Actual End Date");
        put("fkAccountIdMentor1", "Mentor 1");
        put("fkAccountIdMentor2", "Mentor 2");
        put("fkAccountIdObserver1", "Observer 1");
        put("fkAccountIdObserver2", "Observer 2");
        put("fkAccountIdAssigned", "Assigned User");
        put("fkAccountIdAssignee", "Assignee User");
        put("taskEstimate", "Estimate");
        put("taskWorkflowId", "Workflow");
        put("fkWorkflowTaskStatus", "Workflow Status");
        put("currentActivityIndicator", "Current Activity Indicator");
        put("userPerceivedPercentageTaskCompleted", "User Perceived Percentage Task Completed");
        put("taskProgressSystem", "Work Item Progress");
        put("parkingLot", "Parking Lot");
        put("taskProgressSetByUser", "Work Item Progress Set By User");
        put("taskState", "Work Item State");
        put("immediateAttention", "Immediate Attention");
        put("immediateAttentionFrom", "Immediate Attention From");
        put("taskPriority", "Work Item Priority");
        put("sprintId", "Sprint");
        put("acceptanceCriteria", "Acceptance Criteria");
        put("isBallparkEstimate", "Is Ballpark Estimate");
        put("isEstimateSystemGenerated", "Is Estimate System Generated");
        put("recordedEffort", "Recorded Effort");
        put("childTaskIds", "Child Task");
        put("bugTaskRelation", "Linked Work Item");
        put("currentlyScheduledTaskIndicator", "Currently Scheduled Task Indicator");
        put("dependencyIds", "Dependency");
        put("blockedReason", "Work Item Blocked Reason");
        put("fkAccountIdRespondent", "Blocked Work Item Respondent");
        put("recordedTaskEffort", "Recorded Work Item Effort");
        put("billedMeetingEffort", "Billed Meeting Effort");
        put("totalMeetingEffort", "Total Meeting Effort");
        put("fkAccountIdBugReportedBy", "Bug Reported By");
        put("severityId","Severity");
        put("environmentId","Environment");
        put("resolutionId","Resolution");
        put("placeOfIdentification","Place Of Identification");
        put("stepsTakenToComplete","Steps Taken To Complete");
        put("referenceWorkItemId","Reference Work Item");
        put("fkEpicId","Epic");
        put("taskLabels","Label");
        put("meetingList","Reference Meeting");
        put("releaseVersionName", "Release Version");
    }};

    @Deprecated(since = "2023-02-02")
    public static final HashMap<String, Object> TaskHistory_Fields_Mapping = new HashMap<>() {{
        List<String> titleDesc = new ArrayList<>();
        titleDesc.add("taskTitle");
        titleDesc.add("taskDesc");

        List<String> expActStartEndDateTime = new ArrayList<>();
        expActStartEndDateTime.add("taskExpStartDate");
        expActStartEndDateTime.add("taskActStDate");
        expActStartEndDateTime.add("taskExpEndDate");
        expActStartEndDateTime.add("taskActEndDate");

        List<String> mentorsObservers = new ArrayList<>();
        mentorsObservers.add("fkAccountIdMentor1");
        mentorsObservers.add("fkAccountIdMentor2");
        mentorsObservers.add("fkAccountIdObserver1");
        mentorsObservers.add("fkAccountIdObserver2");

        put("1", titleDesc);
        put("2", expActStartEndDateTime);
        put("3", mentorsObservers);
        put("4", "taskEstimate");
        put("5", "taskWorkflowId");
        put("6", "fkWorkflowTaskStatus");
        put("7", "currentActivityIndicator");
        put("8", "userPerceivedPercentageTaskCompleted");
        put("9", "taskProgressSystem");
        put("0", "all");
    }};

    public static final HashMap<String, String> SprintHistory_Column_Name = new HashMap<>() {{
        put(com.tse.core_application.model.Constants.SprintField.SPRINT_TITLE, "Sprint Title");
        put(com.tse.core_application.model.Constants.SprintField.SPRINT_OBJECTIVE, "Sprint Objective");
        put(com.tse.core_application.model.Constants.SprintField.EXP_START_DATE, "Expected Start Date");
        put(com.tse.core_application.model.Constants.SprintField.EXP_END_DATE, "Expected End Date");
        put(com.tse.core_application.model.Constants.SprintField.CAPACITY_ADJUSTMENT_DEADLINE, "Capacity Adjustment Deadline");
        put(com.tse.core_application.model.Constants.SprintField.NEXT_SPRINT, "Next Sprint");
        put(com.tse.core_application.model.Constants.SprintField.PREVIOUS_SPRINT, "Previous Sprint");
        put(com.tse.core_application.model.Constants.SprintField.MODIFY_ESTIMATE, "Estimate Indicator");
        put(com.tse.core_application.model.Constants.SprintField.ACTIVE_INDICATOR, "Edit Estimate Indicator");
    }};

    public static class AiMlConstants {
        public static Integer MAX_TOKENS = 200;
    }
}
