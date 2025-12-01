package com.tse.core_application.model;

import com.tse.core_application.constants.RoleEnum;
import lombok.Getter;
import org.springframework.security.core.parameters.P;

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class Constants {

    public static final String SCHEMA_NAME = "tse";

    //ToDo: get these constants from database
    public static final List<Integer> HIGHER_ROLE_IDS = List.of(9,10,11,12,14,15);
    public static final List<Integer> TEAM_ROLE_IDS = List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,101,102,91);
    public static final List<Integer> ROLE_IDS_FOR_DELETE_ACTION = List.of(11, 12, 14, 15, 91);
    public static final List<Integer> NOTIFICATION_CATEGORY_IDS = List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21);
    public static final List<Integer> ROLE_IDS_FOR_MEETING_ANALYSIS = List.of(11,12,14,15);

    public static final String ALL_FIELDS_MAPPING_STR = "1,2,3,4,5,6,7,8,9,10,11,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35";

    public static final Integer MAX_COMMENT_LENGTH = 5000;
    public static final Integer FIELD_MAPPING_ID_FOR_ATTACHMENT = 31;

    public static final List<Integer> ROLE_IDS_FOR_DELETE_TEAM_ACTION = List.of(132,131,122,121,112,111,102);
    public static final List<Integer> ROLE_IDS_FOR_VIEW_DELETED_TEAM_REPORT = List.of(112,111,132);
    public static final List<Integer> ROLE_IDS_FOR_DELETE_PROJECT_ACTION = List.of(112, 121, 122,131,132);
    public static final List<Integer> ROLE_IDS_FOR_VIEW_PROJECT_TEAM_REPORT = List.of(121, 122, 131, 132);
    public static final List<Integer> ROLE_IDS_FOR_LEAVE = List.of(11, 12, 14, 15, 112, 132);
    public static final List<Integer> ROLE_IDS_FOR_CREATE_UPDATE_GITHUB_IN_PREFERENCE = List.of(132);

    public static final List<Integer> rolesToViewPeopleOnLeave = List.of(102,14,15);

    public static final Integer MAX_GC_MESSAGE_LENGTH = 1000;
    public static final List<Integer> ROLES_WITH_TEAM_ATTENDANCE_ACCESS = List.of(11,12,14,15);
    public static final List<Integer> ROLES_WITH_PROJECT_ATTENDANCE_ACCESS = List.of(111,112);
    public static final List<Integer> ROLES_WITH_ORG_ATTENDANCE_ACCESS = List.of(131,132);
    public static final List<Integer> ROLES_WITH_BU_ATTENDANCE_ACCESS = List.of(121,122);
    public static final List<Integer> daysOfWeek = List.of(1,2,3,4,5,6,7);

    public static final Integer DEFAULT_WORKFLOW_TYPE_PERSONAL_TASK = 1;

    public static final List<Integer> WORK_ITEM_STATUS_ID_LIST = List.of(15, 16, 17, 18, 19, 20, 21);

    public static final Long UNASSIGNED_ACCOUNT_ID = 0L;

    public static final List<Integer> ROLE_IDS_FOR_UPDATE_EPIC_TEAM_ACTION = List.of(10, 11, 12, 14, 15);
    public static final List<Integer> ROLE_IDS_FOR_UPDATE_EPIC_PROJECT_ACTION = List.of(14, 15);

    public static final List<Integer> ROLE_IDS_FOR_PROJECT_MANAGER_ON_TEAM_CREATION = List.of(16, 17);
    public static final List<Integer> ROLE_IDS_FOR_RELEASE_VERSION = List.of(11, 12, 14, 15);

    public static class EntityPreferenceConstants {
        public static final Long DEFAULT_ALLOWED_FILE_SIZE = 15728640L; // in bytes
        public static final Integer TASK_EFFORT_EDIT_DURATION = 15; // in minutes
        public static final Integer MEETING_EFFORT_EDIT_DURATION = 15; // in minutes
        public static final Integer BREAK_DURATION = 30; // in minutes
        public static final List<Integer> OFF_DAYS = Arrays.asList(6,7);
        public static final LocalTime OFFICE_START_TIME = LocalTime.of(9,0,0);
        public static final LocalTime OFFICE_END_TIME = LocalTime.of(17,0,0);
    }

    public static class WorkFlowTaskStatusConstants {
        public static final String STATUS_NOT_STARTED = "not-started";
        public static final String STATUS_STARTED = "started";
        public static final String STATUS_BLOCKED = "blocked";
        public static final String STATUS_COMPLETED = "completed";
        public static final String STATUS_BACKLOG = "backlog";
        public static final String STATUS_ON_HOLD = "on-hold";
        public static final String STATUS_DELETE = "deleted";

        public static final String STATUS_BACKLOG_TITLE_CASE = "Backlog";
        public static final String STATUS_STARTED_TITLE_CASE = "Started";
        public static final String STATUS_NOT_STARTED_TITLE_CASE = "Not-Started";
        public static final String STATUS_COMPLETED_TITLE_CASE = "Completed";
        public static final String STATUS_DELETE_TITLE_CASE = "Deleted";
        public static final String STATUS_BLOCKED_TITLE_CASE = "Blocked";
        public static final String STATUS_ON_HOLD_TITLE_CASE = "On-Hold";
        public static final String STATUS_DUE_DATE_NOT_PROVIDED = "Due Date Not Provided"; // backlog status in case of workflow type 'Personal Task'
    }

    // in minutes
    public static class statDelayTimes{
        public static final long statDelayTime_PriorityP0 = 30L;
        public static final long statDelayTime_PriorityP1 = 60L;
    }


    public static class WorkFlowStatusIds {
        public static final List<Integer> COMPLETED = new ArrayList<>(List.of(6,13,20,27,34,41,48,55));
        public static final List<Integer> DELETED = new ArrayList<>(List.of(7,14,21,28,35,42,49,56));
            public static final List<Integer> BLOCKED = new ArrayList<>(List.of(4,11,18,25,32,49,46,53));
        public static final List<Integer> BACKLOG = new ArrayList<>(List.of(1, 8, 15, 22, 29, 36, 43, 50));
    }

    public static class Priorities {
        public static final String PRIORITY_P0 = "P0";
        public static final String PRIORITY_P1 = "P1";
        public static final String PRIORITY_P2 = "P2";
        public static final String PRIORITY_P3 = "P3";
        public static final String PRIORITY_P4 = "P4";
    }

    public static final String PRIORITY_P0 = "P0";
    public static final String PRIORITY_P1 = "P1";
    public static final String PRIORITY_P4 = "P4";
    public static final String PRIORITY_P3 = "P3";

    public static final List<String> PRIORITY_LIST = List.of("P0", "P1", "P2", "P3", "P4");

    public static class DefaultStatsCalDateRange {
        public static final LocalDateTime FROM_DATE = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
//        public static final LocalDateTime TO_DATE = LocalDateTime.of(2030, 12, 31, 0, 0, 0);
    }

    /* @deprecated: This is used by getStats (version 1) api which is deprecated. */
    @Deprecated(since = "2022-06-23")
    public static class OtherConstants {
        public static final Time eodTime = Time.valueOf("01:00:00");
    }


    public static class TaskWorkFlowIds {
        public static final int PERSONAL_TASK = 1;
        public static final int TO_DO_OFFICE_NON_HIERARCHICAL_TEAM = 6;
        public static final int TEAM_WORK_ITEM = 3;
        public static final int SPRINT = 8;
    }

    public static class DependencyRelationType {
        public static final int FS_RELATION_TYPE = 1;
        public static final int SS_RELATION_TYPE = 2;
        public static final int FF_RELATION_TYPE = 3;
        public static final int SF_RELATION_TYPE = 4;
    }

    public static class OfficeHoursKeys {
        public static final String OFFICE_START_TIME = "OFFICE_START_TIME";
        public static final String EOD_TIME = "EOD_TIME";
    }

    //    Discussed during morning call of 23rd june: converting all IST constants to UTC time
//    public static final int HOURS_IN_DAY = 8;

    //    Discussed between sir and ankit on 2022-08-01
    public static final int HOURS_IN_DAY = 7;

    //    public static final Time OFFICE_START_TIME = Time.valueOf("10:00:00");
    /* @deprecated: The office start time in stats algo will come from the database instead of taking it from the constant file. */
//    @Deprecated(since = "2022-07-29")
    public static final Time OFFICE_START_TIME = Time.valueOf("10:00:00");           // subtracting 5 hours 30 minutes from IST time

    public static final Time OFFICE_END_TIME = Time.valueOf("20:00:00");

    public static final long CAPACITY_LIMIT = 2;


    /* @deprecated: The office end time of various workflowTypes will come from database in stats algo instead of taking it from the constant file. */
    @Deprecated(since = "2022-07-29")
    public static final HashMap<Integer, Time> EOD_TIME = new HashMap<Integer, Time>() {{
//        put(TaskWorkFlowIds.PERSONAL_TASK, Time.valueOf("17:30:00"));
        put(TaskWorkFlowIds.PERSONAL_TASK, Time.valueOf("12:00:00"));     // subtracting 5 hours 30 minutes from IST time
//        put(TaskWorkFlowIds.TO_DO_OFFICE_NON_HIERARCHICAL_TEAM, Time.valueOf("20:00:00"));
        put(TaskWorkFlowIds.TO_DO_OFFICE_NON_HIERARCHICAL_TEAM, Time.valueOf("14:30:00"));       // subtracting 5 hours 30 minutes from IST time
//        put(TaskWorkFlowIds.TEAM_WORK_ITEM, Time.valueOf("20:00:00"));
        put(TaskWorkFlowIds.TEAM_WORK_ITEM, Time.valueOf("14:30:00"));          // subtracting 5 hours 30 minutes from IST time
    }};

    public static final HashMap<TaskPriority, Long> ESTIMATES = new HashMap<>() {{
        put(TaskPriority.P0, 3 * 60 * 60 * 1000L);  // 3 hours
        put(TaskPriority.P1, 6 * 60 * 60 * 1000L);  // 6 hours
        put(TaskPriority.P2, 2 * 60 * 60 * 1000L);  // 2 hours
        put(TaskPriority.P3, 2 * 60 * 60 * 1000L);  // 2 hours
        put(TaskPriority.P4, 2 * 60 * 60 * 1000L);  // 2 hours
    }};

    public static final Long DEFAULT_ESTIMATE = 60 * 60 * 1000L;

    public static class TaskStatConstants {
        public static final Integer END_TIME_P0_BUFFER = 3 * 60 * 60 * 1000;      // 3 hours
        public static final Integer END_TIME_P0_BUFFER_EXTRA = 30 * 60 * 1000;    // 0.5 hour
        public static final Integer END_TIME_P1_BUFFER = 6 * 60 * 60 * 1000;      //  6 hours
        public static final Integer END_TIME_P1_BUFFER_EXTRA = 60 * 60 * 1000;    //  1 hour
    }

    public static class EstimateThresholdsDifferences {
        public static final long THRESHOLD_1 = 2 * 60 * 60 * 1000;
        public static final long THRESHOLD_2 = 4 * 60 * 60 * 1000;
        public static final long THRESHOLD_3 = 6 * 60 * 60 * 1000;
        public static final long THRESHOLD_4 = 8 * 60 * 60 * 1000;
        public static final long THRESHOLD_5 = 16 * 60 * 60 * 1000;
    }

    public static class EstimateThresholdComparisonDifference {
        public static final long THRESHOLD_1 = 2 * 60 * 60 * 1000;
    }

    // change 07-04-2023: between sir and mohan added bufferTimeLarge_3 = 4 hours
    public static final HashMap<String, Long> BUFFER_TIME_FOR_ESTIMATE = new HashMap<>() {{
        put("bufferTimeSmall", 0L);                            // 0
        put("bufferTimeMedium", (long) (30 * 60 * 1000));      // 30 minutes
        put("bufferTimeLarge", (long) (60 * 60 * 1000));       //  1 hour
        put("bufferTimeLarge_1", (long) (90 * 60 * 1000));     //  1 hour 30 minutes
        put("bufferTimeLarge_2", (long) (2 * 60 * 60 * 1000));  // 2 hours
        put("bufferTimeLarge_3", (long) (4 * 60 * 60 * 1000));  // 4 hours
    }};


    public static class TaskFields {
        public static final String TASK_PROGRESS_SYSTEM = "taskProgressSystem";
        public static final String WORKFLOW_STATUS_TASK_ID = "workflowTaskStatusId";
        public static final String CHILD_TASK_IDS = "childTaskIds";
        public static final String TASK_PROGRESS_SYSTEM_LAST_UPDATED = "taskProgressSystemLastUpdated";
        public static final String WORKFLOW_TASK_STATUS = "fkWorkflowTaskStatus";
        public static final String CURRENTLY_SCHEDULED_TASK_INDICATOR = "currentlyScheduledTaskIndicator";
        public static final String CURRENT_ACTIVITY_INDICATOR = "currentActivityIndicator";
        public static final String ACTUAL_START_DATE = "taskActStDate";
        public static final String ACTUAL_END_DATE = "taskActEndDate";
        public static final String RECORDED_EFFORT = "recordedEffort";
        public static final String USER_PERCEIVED_PERCENTAGE = "userPerceivedPercentageTaskCompleted";
        public static final String BUG_TASK_RELATION = "bugTaskRelation";
        public static final String EXP_START_DATE = "taskExpStartDate";
        public static final String EXP_END_DATE = "taskExpEndDate";
        public static final String EXP_END_TIME = "taskExpEndTime";
        public static final String EXP_START_TIME = "taskExpStartTime";
        public static final String DEPENDENCY_IDS = "dependencyIds";
        public static final String PRIORITY = "taskPriority";
        public static final String ESTIMATE = "taskEstimate";
        public static final String TASK_STATE = "taskState";
        public static final String BLOCKED_REASON = "blockedReason";
        public static final String BLOCKED_REASON_TYPE_ID = "blockedReasonTypeId";
        public static final String ACCOUNT_ID_RESPONDENT = "fkAccountIdRespondent";
        public static final String TOTAL_EFFORT = "totalEffort";
        public static final String TOTAL_MEETING_EFFORT = "totalMeetingEffort";
        public static final String RECORDED_TASK_EFFORT = "recordedTaskEffort";
        public static final String BILLED_MEETING_EFFORT = "billedMeetingEffort";
        public static final String SPRINT_ID = "sprintId";
        public static final String ACCOUNT_ID_ASSIGNED = "fkAccountIdAssigned";
        public static final String ACCOUNT_ID_ASSIGNEE = "fkAccountIdAssignee";
        public static final String MENTOR_1 = "fkAccountIdMentor1";
        public static final String MENTOR_2 = "fkAccountIdMentor2";
        public static final String OBSERVER_1 = "fkAccountIdObserver1";
        public static final String OBSERVER_2 = "fkAccountIdObserver2";

        public static final String IMMEDIATE_ATTENTION = "immediateAttention";

        public static final String IMMEDIATE_ATTENTION_FROM = "immediateAttentionFrom";
        public static final String IMMEDIATE_ATTENTION_REASON = "immediateAttentionReason";
        public static final String TASK_NUMBER = "taskNumber";
        public static final String STEPS_TAKEN_TO_COMPLETE = "stepsTakenToComplete";
        public static final String RESOLUTION_ID = "resolutionId";
        public static final String REFERENCE_WORK_ITEM_ID = "referenceWorkItemId";

        public static final String EPIC_ID = "fkEpicId";
        public static final String LABELS = "taskLabels";
        public static final String REFERENCE_MEETING = "meetingList";

        public static final String CHILD_TASK_INTERNAL_DEPENDENCIES = "countChildInternalDependencies";
        public static final String CHILD_TASK_EXTERNAL_DEPENDENCIES = "countChildExternalDependencies";
    }


    public static long BUFFER_TIME_FOR_NON_DELAYED_IN_FLOW_XY_COMPARABLE_TIME_IN_SECOND = 30 * 60;
    public static long BUFFER_TIME_FOR_NON_DELAYED_IN_FLOW_XY_COP_IN_SECOND = 4 * 60 * 60;

    //Used to compare curent time and tast actual started time to determine whether task is just started.
    // This was in milliseconds but we needed in seconds -- change: 05-04-2023 as discussed b/w sir and Mohan
    public static long TASK_JUST_STARTED_THRESHOLD = 60;
//    public static long TASK_JUST_STARTED_THRESHOLD = 60 * 1000;


    public static long TIME_THRESHOLD_FOR_CREATE_TS_P0 = 30 * 60 * 1000;

    public static long TIME_THRESHOLD_FOR_CREATE_TS_P1 = 60 * 60 * 1000;

    public static long TIME_THRESHOLD_FOR_CREATE_TS_EARLIER_DAY_P0 = 60 * 60 * 1000;

    public static long TIME_THRESHOLD_FOR_CREATE_TS_EARLIER_DAY_P1 = 4 * 60 * 60 * 1000;


    public static class FIELDS_FOR_FILTERS {
        public static final String FIELD_ORGANIZATION = "organizationId";
        public static final String FIELD_PROJECT = "projectId";
        public static final String FIELD_TEAM = "teamId";

    }

    public static List<String> statAffectingFields = List.of("taskPriority", "taskEstimate", "newEffortTracks", "userPerceivedPercentageTaskCompleted", "taskExpStartDate", "taskExpEndDate", "taskActStDate", "taskActEndDate", "fkWorkflowTaskStatus");

    public static long TASK_SYSTEM_UPDATED_TIMSTAMP_COMPARISION_THRESHOLD = 750;
    public static long BREAK_TIME_IN_DAY = 3600;

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

        private EntityTypes() {
            // Utility class
        }

        public static boolean isValid(int entityTypeId) {
            return entityTypeId == USER || entityTypeId == ORG ||
                    entityTypeId == PROJECT || entityTypeId == TEAM;
        }

        public static String getTypeName(int entityTypeId) {
            switch (entityTypeId) {
                case USER: return "user";
                case ORG: return "org";
                case PROJECT: return "project";
                case TEAM: return "team";
                default: return "unknown";
            }
        }
    }

    public  static class Task_Type{
        public static final Integer TASK_TYPE_TASK = 0;
        public static final Integer TASK_TYPE_SUB_TASK = 1;
        public static final Integer TASK_TYPE_BUG_TASK = 2;
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

    public static final HashMap<Integer, String> taskTypeMessages = new HashMap<>() {
        {
            put(1, "Task");
            put(2, "Parent Task");
            put(3, "Child Task");
            put(4, "Bug");
            put(5, "Epic");
            put(6, "Initiative");
            put(7, "Risk");
            put(8, "Personal Task");
        }
    };
    public static Integer BUFFER_DAYS_FOR_EXPECTED_END_DATE = 2;

    public static String[] publicPaths = {"/ws-notification/.*", "/auth/login", "/swagger-ui/.*",
            "/swagger-resources/.*", "/v3/api-docs", "/swagger-apis/.*", "/register/getAllOptionsForRegistration", "/auth/generateotp", "/auth/signup", "/auth/sendotp",
            "/swagger-ui.html", "/task/tempSendEmail/.*","/schedule/.*","/schedule/meeting/reminder","/auth/googlesso", "/swagger-ui.html", "/swagger-ui/.*", "/v3/api-docs/.*", "/webjars/.*",
            "/userInvite/getInviteDetails/.*", "/organization/exists", "/schedule/blockedTask/blockedTaskReminder", "/auth/validateToken","/converter/encryptData", "/converter/decryptData",
            "/batch/updateTeamInitials", "/auth/validateTokenAccount", "/jira/verifyJiraUserAndSendOtp", "/jira/verifyOtpAndActivateUser", "/jitsi/.*", "/comment/updateCommentsTag",
            "/meeting/updateFetchButtonInMeeting", "/groupConv/systemGroupMigration", "/ai/scheduleTaskDataMigration"};

    public static String[] privatePaths = {"/api/accessDomain/createAndEditAccessDomain/.*", "/api/accessDomain/removeTeamMember", "/api/accessDomain/getTeamMembers/.*",
            "/api/comment/getUserAllTaskComments", "/api/comment/getTaskAllComments/.*", "/api/comment/addComment", "/api/organization/userAllOrganizationList",
            "/api/organization/ownerAccountId/.*", "/api/project/projectListByOrgId/.*", "/api/project/getProject/.*", "/api/getStats", "/api/getStatsV2", "/api/stats/getStatsV3",
            "/api/task/getAllTaskStatusDetails/.*", "/api/task/tempAdd", "/api/task/getTaskHistory", "/api/task/getTask", "/api/task/addTask", "/api/task/getTasks", "/api/task/updateTask/.*",
            "/api/task/getHistory/.*", "/api/task/deleteTask/.*", "/api/task/getAllUsersToAssignTask/.*", "/api/task/getAllUsersToObserveTask/.*", "/api/task/getAllUsersToMentorTask/.*",
            "/api/task/getAllImmediateAttentionUsers/.*", "/api/task/getTaskByFilter", "/api/task/getUsersByAllTeamAndOrg", "/api/team/createTeam", "/api/team/getTeamsForAdmins/.*", "/api/team/updateTeam",
            "/api/team/getTeamById/.*", "/api/team/getParentTeams/.*", "/api/team/getTeamListForCreateTask/.*", "/api/userAccount/userAccountId/.*", "/api/userAccount/userList/.*",
            "/api/userAccount/getUserDetailsByAccountId/.*", "/api/team/getAllTeamsByUserId/.*", "/api/role/getAllRoles", "/api/taskMedia/uploadFile/.*", "/api/taskMedia/downloadFile/.*",
            "/api/user/getUserDetails/.*", "/api/user/getOrgTeamDropdownStructure/.*", "/api/workflowType/getAllWorkflowsAndPriorities", "/api/dashboardButtons/getAllUIButtons", "/api/user/logout",
            "/api/firebase-token/insert-token", "/api/firebase-token/validate-token/.*", "/api/firebase-token/get-token/.*", "/api/timesheet/getTimeSheet", "/api/task-attachment/download-attachment",
            "/api/task-attachment/upload-attachments", "/api/task-attachment/delete-attachments", "/api/field-mapping/getAllFieldsAndMappings", "/api/task-history/getAllHistoryByMappings/.*", "/api/test/view",
            "/api/task-history-mapping/getAllFieldsAndMappings", "/api/task-history/getAllHistoryByMappings/.*",
            "/api/task-attachment/upload-attachments", "/api/task-attachment/delete-attachments", "/api/test/checkforcycles/.*", "/api/board/view", "/api/board/update",
            "/api/task-history/getAllHistoryByMappings/.*", "/api/task/getScheduledTasks",
            "/api/task-history/getAllHistoryByMappings/.*", "/api/task/getScheduledTasks/", "/api/meeting/createMeeting",
            "/api/meeting/getAllScheduledMeeting", "/api/meeting/updateAttendeeResponse", "/api/meeting/getMeeting/.*",
            "/api/meeting/updateMeeting/.*", "/api/notification/getAllNotifications", "/api/notification/markNotificationRead/.*",
            "/api/sticky-note/add", "/api/sticky-note/getAllNotesByUserId/.*", "/api/sticky-note/update", "/api/task/getTaskRecordedEffort/.*",
            "/api/meeting/createRecurringMeeting", "/api/meeting/getRecurringMeeting/.*", "/api/meeting/getMeetingCondensedView",
            "/api/meeting/getMeetingExpandedView", "/api/meeting/updateRecurringMeeting/.*",
            "/api/leave/.*", "/api/converter/encryptData", "/api/meeting/getMeetingByFilters", "/api/converter/decryptData",
            "/api/meeting/updateOrganizerResponse", "/api/notification/markAllNotificationRead", "/api/notification/clearNotification",
            "/api/board/active-tasks", "/api/task/getAllTaskWithAttention", "/api/task/quickCreateTask",
            "/api/organization/getOrgMembers/.*", "/api/organization/getOrgToEdit", "/api/organization/removeMemberFromOrg",
            "/api/organization/addNewMemberToOrg", "/api/organization/getOrgRequest", "/api/organization/updateOrgRequest/.*",
            "/api/task/setCurrentlyActiveTasks", "/api/comment/getCommentsByTaskId", "/api/task/getUpdatedTaskPreview",
            "/api/task/getAllEnvironmentSeverityResolution/.*", "/api/task/addChildTask", "/api/groupConv/getEntityAllMessages/.*",
            "/api/userPreference/addUserPreference", "/api/userPreference/getUserPreference/.*", "/api/groupConv/getEntitiesOrderByMessage",
            "/api/groupConv/getMessageAttachment/.*", "/api/user/getUserProfileDetails/.*", "/api/user/editUserProfile",
            "/api/groupConv/addMessage", "/api/task/getOpenTasksAssignedToUser", "/api/task/getDependencyGraphForTask/.*",
            "/api/dependency/removeDependency/.*", "/api/label/getLabelForTeam/.*", "/api/label/removeLabelFromTask/.*",
            "/api/label/removeLabelFromMeeting/.*", "/api/task/getTasksByLabel", "/api/team/getAllTeamsByOrgId/.*",
            "/api/groupConv/getEntityMessagesBetweenGc/.*", "/api/task/getTasksByLabel", "/api/team/getAllTeamsByOrgId/.*",
            "/api/getApprovedLeaves/.*", "/api/role/getTeamRoles", "/api/notification/category/getAllCategories",
            "/api/task/createDuplicateTask", "/api/label/removeLabelFromTask/.*", "/api/label/removeLabelFromMeeting/.*",
            "/api/label/removeLabelFromRecurringMeeting/.*", "/api/organization/inviteUserToOrg", "/api/userInvite/revoke/.*",
            "/api/userInvite/editValidity/.*", "/api/userInvite/invitees/.*", "/api/task/getTaskAllEfforts/.*", "/api/task/editRecordedEffort/.*",
            "/api/entity-preferences/upsert/orgPreference", "/api/entity-preferences/getOrgPreference/.*", "/api/task/exportToCSV",
            "/api/project/createProject", "/api/project/getProjectsListByBu/.*", "/api/project/updateProject", "/api/label/getLabelForEntity/.*",
            "/api/sprint/createSprint", "/api/sprint/getAllSprint", "/api/sprint/getSprint/.*", "/api/sprint/addTaskToSprint", "/api/sprint/updateSprint/.*",
            "/api/task/searchTasks", "/api/team/addToTeamByInvite", "/api/userInvite/teamInvitees/.*", "/api/userInvite/getInviteDetailsForTeam/.*",
            "/api/team/inviteUserToTeam", "/api/attendance/getAllUserAttendance", "/api/capacity/getSprintCapacityDetails/.*",
            "/api/capacity/getUserSprintCapacityDetails/.*", "/api/capacity/updateLoadedCapacityRatios", "/api/sprint/deleteTaskFromSprint",
            "/api/user/getUserAllOrgAccess/.*", "/api/organization/getOrgAllBU/.*", "/api/attendance/exportToCSV", "/api/user/getUserAllOrgAccessStructures/.*",
            "/api/project/getProjectAllMember/.*", "/api/sprint/getAllSprintForEntity", "/api/sprint/getAllSprintInEntity", "/api/task/quickCreateBug", "/api/accessDomain/getEntityMembers/.*",
            "/api/sticky-note/pin/.*", "/api/sticky-note/markImportant/.*", "/api/sticky-note/unpin/.*", "/api/sticky-note/unmarkImportant/.*",
            "/api/task/getRecurringDatesForTask", "/api/task/createRecurringTasks", "/api/user/getAllOrgTeamDropdownStructure/.*", "/api/template/addTemplate", "/api/template/getAllUserCreatedTemplate/.*",
            "/api/template/getTemplateForEntity/.*", "/api/template/getUserTemplates", "/api/template/updateTemplate/.*", "/api/sprint/changeSprintStatus/.*", "/api/sprint/moveTaskFromSprint", "/api/sprint/getAllSprintInEntity",
            "/api/stats/getTodayFocus", "/api/alert/addAlert", "/api/alert/getAlert/.*", "/api/alert/markAlertAsViewed/.*", "/api/alert/getUserReceivedAlerts", "/api/alert/getUserAlertsToView", "/api/alert/getUserSentAlerts",
            "/api/register/getDefaultUserImage", "/api/sticky-note/getUserDashboardNote", "/api/sticky-note/unpinFromDashboard/.*", "/api/sticky-note/pinToDashboard/.*", "/api/reminder/addReminder", "/api/reminder/updateReminder/.*",
            "/api/reminder/getReminderById/.*", "/api/reminder/deleteReminder/.*", "/api/reminder/getUserAllReminder", "/api/reminder/getRemindersForDate", "/api/capacity/getTeamCapacity/.*", "/api/stats/getUserProgress", "/api/template/getTemplateById/.*",
            "/api/personal-task/addTask", "/api/personal-task/updateTask", "/api/personal-task/getPersonalTask/.*", "/api/personal-task/upload-attachments", "/api/personal-task/download-attachment", "/api/personal-task/delete-attachments",
            "/api/personal-task/createDuplicatePersonalTask/.*", "/api/personal-task/createPersonalRecurringTasks", "/api/personal-task/deletePersonalTask/.*", "/api/auth/completeRegistration",
            "/api/team/teamCodeExists/.*", "/api/team/generateTeamCode", "/api/sprint/getSprintTaskByFilter", "/api/user/sendOtpForManagingUserVerification", "/api/user/verifyOtpAndAddManagedUser", "/api/user/sendOtpToVerifyManagedUserRemoval",
            "/api/user/verifyOtpAndRemoveManagedUser", "/api/sprint/getBurnDownDetails", "/api/sprint/getVelocityDetails", "/api/sprint/getWorkItemsRemaining", "/api/converter/encryptValues", "/api/converter/decryptValues", "/api/task/getEstimateDrillDown/.*",
            "/api/task/getAllSubTaskStatus/.*", "/api/task/updateTaskFields", "/api/task/askForStatus", "/api/task/updateBugDetails", "/api/personalTemplate/addTemplate", "/api/personalTemplate/updateTemplate/.*", "/api/personalTemplate/getTemplateById/.*",
            "/api/task/deleteTasks", "/api/sprint/getSprintTasksWithoutEstimates", "/api/perfNote/addPerfNote", "/api/perfNote/updatePerfNote/.*", "/api/perfNote/getPerfNoteForTask/.*", "/api/perfNote/getPerfNoteByFilter",
            "/api/perfNoteHistory/getPerfNoteHistory/.*", "/api/perfNote/getAllTaskRating", "/api/entity-preferences/getWorkflowStatusPreferenceForQuickCreate/.*","/api/task/validateRelation", "/api/accessDomain/getAllEntityMembers/.*", "/api/admin/deactivateAccounts",
            "/api/admin/reactivateAccounts", "/api/admin/getAllUsersForAdmin", "/api/admin/deactivateOrganization/.*", "/api/admin/reactivateOrganization/.*", "/api/admin/getAllOrgDetails", "/api/admin/addExceptionalUser", "/api/admin/removeExceptionalUser/.*",
            "/api/admin/getAllExceptionalRegistration", "/api/admin/getAllActiveExceptionalRegistration", "/api/admin/addBlockedUser", "/api/admin/removeBlockedUser/.*", "/api/admin/getAllBlockedRegistration",
            "/api/admin/getAllActiveBlockedRegistration", "/api/sprint/getAllSprintInfoForEntity", "/api/admin/getDefaultEntitiesCount", "/api/admin/updateLimitsInOrganization/.*", "/api/admin/updateExceptionalUser/.*", "/api/admin/reAddExceptionalUser/.*",
            "/api/admin/getOrgReportByEmail/.*", "/api/admin/getOrgReportByOrgName/.*", "/api/admin/getApplicationReport",
            "/api/perfNoteHistory/getPerfNoteHistory/.*", "/api/perfNote/getAllTaskRating", "/api/entity-preferences/getPreferenceForQuickCreateCreateUpdate/.*","/api/task/validateRelation", "/api/accessDomain/getAllEntityMembers/.*", "/api/sprint/getAllSprintInfoForEntity",
            "/api/task/checkDeleteForChildTask/.*", "/api/workflowType/getAllWorkflowsAndPrioritiesForEpic", "/api/epic/createEpic", "/api/epic/getEpic/.*", "/api/epic/addTaskToEpic", "/api/epic/removeTaskFromEpic", "/api/epic/updateEpic", "/api/sprint/getAllSprintForListing", "/api/team/deleteTeam/.*",
            "/api/project/deleteProject/.*", "/api/team/getAllTeamsByProjectId/.*", "/api/team/getAllDeletedTeamReport/.*", "/api/project/getAllDeletedProjectReport/.*", "/api/epic/getAllEpic", "/api/epic/getTeamForEpic", "/api/epic/getEpicForListing", "/api/epic/getEpicForListingInFilter", "/api/entity-preferences/getHolidaysForOrg/.*",
            "/api/sprint-history/getSprintHistory", "/api/epic/getMemberForEpic", "/api/task/getOpenTasksAssignedToUserInSprint", "/api/sprint/removeMemberFromSprint", "/api/sprint/addMemberInSprint", "/api/sprint/recalculateCapacity", "/api/admin/updateRestrictedDomain", "/api/admin/addRestrictedDomain",
            "/api/admin/getAllRestrictedDomains", "/api/entity-preferences/getLeaveTypeAlias/.*", "/api/task/getDependencyGraphByFilter", "/api/sprint/bulkMoveTaskFromSprint",
            "/api/admin/deleteRestrictedDomain/.*", "/api/jira/getUserFromJiraFile", "/api/jira/addJiraTask", "/api/jira/getJiraTaskIdAndTitle", "/api/project/getAllProjectOfUserWithAdminRole", "/api/project/getAdminRoleAccessDomain", "/api/register/getUserImageByUserId/.*", "/api/register/getUserImageByAccountId/.*", "/api/register/getUserImageByEmail/.*",
            "/api/meeting/searchMeeting", "/api/project/getPMRoleOfProject", "/api/gitrepo/link", "/api/gitrepo/status", "/api/gitrepo/unlink", "/api/gitrepo/repositories", "/api/gitrepo/create-branch", "/api/gitrepo/work-item-branches/.*", "/api/userInvite/resend/.*", "/api/user/editUserName", "/api/jira/getJiraCustomWorkflowStatus",
            "/api/jira/importUserFromJiraUserFile", "/api/entity-preferences/getTeamPreference/.*", "/api/entity-preferences/saveTeamPreference", "/api/meeting/v2/getMeeting","/api/label/getEntityTypeLabels/.*", "/api/firebase-token/conversation-notification", "/api/attendance/getMemberOnLeaveForAttendance",
            "/api/task/getReleaseVersionOfEntity/.*", "/api/capacity/fetchAnotherSprintLoadedCapacityRatios/.*", "/api/jira/getCustomJiraIssueType", "/api/task/markWorkItemStarred", "/api/task/markWorkItemUnStarred", "/api/gitrepo/addGithubAccountAndItsRepo", "/api/gitrepo/removeGithubAccountAndItsRepo", "/api/gitrepo/getAllGithubAccountAndItsRepo/.*",
            "/api/gitrepo/crossOrgGithubLink", "/api/leave/getLeaveAttachment/.*", "/api/gitrepo/getAllOrgConnectedToGithub", "/api/timesheet/exportToCSV", "/api/jira/token/getJiraProjects", "/api/jira/token/getUserFromJira", "/api/jira/token/getCustomJiraIssueType", "/api/jira/token/getJiraCustomWorkflowStatus", "/api/jira/token/importUserFromJira",
            "/api/jira/token/getJiraTaskIdAndTitle", "/api/jira/token/importJiraUsersManually", "/api/jira/token/importJiraTask", "/api/organization/createCustomEnvironment", "/api/organization/updateCustomEnvironment/.*", "/api/organization/getCustomEnvironment/.*", "/api/organization/getActiveCustomEnvironment/.*", "/api/meeting/notifyAndPutAttendeesEffort", 
            "/api/meeting/bulkUpdateAttendeeResponse","/api/organization/addFeatureAccess","/api/organization/updateFeatureAccess","/api/organization/getAllFeatureAccess/.*","/api/organization/deleteFeatureAccess","/api/organization/getFeatureAccessActions/.*", 
            "/api/attendance/v2/getAllUserAttendance", "/api/attendance/v2/getMemberOnLeaveForAttendance", "/api/sprint/fetchSprintCardFieldActions", "/api/meeting/uploadFileMetadataForModel", "/api/meeting/uploadMeetingAnalysis","/api/userAccount/orgMembersExcludedByCurrentTeam/.*",
            "/api/geo-fence-policy/.*/createGeoFencingPolicy", "/api/geo-fence-policy/.*/getGeoFencePolicy", "/api/geo-fence-policy/.*/updateGeoFencePolicy", "/api/geo-fence-policy/getAllGeoFencePolicies", "/api/fence/.*/createFence", "/api/fence/.*/updateFence", "/api/fence/.*/getFence", "/api/fence/allFence",
            "/api/fenceAssignment/.*/assignFenceToEntity", "/api/fenceAssignment/.*/getAssignedEntityOfFence", "/api/fenceAssignment/.*/getUserFences", "/api/punch-event/.*/punch", "/api/punchRequest/.*/requestPunchForEntity", "/api/punch-event/.*/punched", "/api/punchRequest/.*/getPendingRequest", "/api/punchRequest/.*/getPunchRequestById",
            "/api/punchRequest/.*/getPendingRequestHistory", "/api/notification/getAllTaskUpdationDetails", "/api/geo-fence-policy/getGeoFenceActiveDetails", "/api/punch-event/.*/getGeoFencingAttendanceData", "/api/punch-event/.*/getUserEventDetails", "/api/ai/.*", "/api/geo-fence-policy/.*/deactivateGeoFencingPolicy", "/api/userAccount/blockedByMembers", "/api/organization/activateAccountIdsInOrg", "/api/organization/deactivateAccountIdsInOrg"};


    public static String[] validatePrivatePathsForSingleHeaderAccountId = {"/accessDomain/createAndEditAccessDomain/.*", "/comment/addComment",
            "/task/addTask", "/task/updateTask/.*", "/task/deleteTask/.*", "/team/createTeam", "/team/updateTeam", "/task/tempAdd", "/task-attachment/upload-attachments",
            "/task-attachment/delete-attachments", "/sticky-note/add", "/sticky-note/update", "/label/removeLabelFromTask/.*", "/label/removeLabelFromMeeting/.*",
            "/label/removeLabelFromRecurringMeeting/.*", "/organization/inviteUserToOrg", "/userInvite/revoke/.*", "/userInvite/editValidity/.*", "/userInvite/invitees/.*",
            "/entity-preferences/upsert/orgPreference", "/entity-preferences/getOrgPreference/.*", "/task/editRecordedEffort/.*", "/project/createProject", "/project/getProjectsListByBu/.*",
            "/project/updateProject", "/team/addToTeamByInvite", "/userInvite/teamInvitees/.*", "/team/inviteUserToTeam", "/sprint/deleteTaskFromSprint"
            ,"/task/getRecurringDatesForTask", "/task/createRecurringTasks", "/personal-task/addTask", "/personal-task/updateTask", "/personal-task/getPersonalTask/.*"
            ,"/personal-task/upload-attachments", "/personal-task/download-attachment", "/personal-task/delete-attachments",
            "/personal-task/createDuplicatePersonalTask/.*", "/personal-task/createPersonalRecurringTasks", "/personal-task/deletePersonalTask/.*",
            "/task/getUpdatedTaskPreview", "/task/getTask", "/user/sendOtpForManagingUserVerification", "/sprint/getBurnDownDetails", "/sprint/getVelocityDetails", "/sprint/getWorkItemsRemaining",
            "/task/updateBugDetails", "/personalTemplate/addTemplate", "/personalTemplate/updateTemplate/.*", "/sprint/getSprintTasksWithoutEstimates", "/perfNote/addPerfNote", "/perfNote/updatePerfNote/.*",
            "/perfNote/getPerfNoteForTask/.*", "/perfNote/getPerfNoteByFilter", "/team/deleteTeam/.*", "/team/getAllTeamsByProjectId/.*", "/team/getAllDeletedTeamReport/.*", "/project/deleteProject/.*", "/project/getAllDeletedProjectReport/.*",
            "entity-preferences/getTeamPreference/.*", "entity-preferences/saveTeamPreference"};

    public static final String Task_Add = "Task Add Without Assignment";
    public static final String Self_Created_Self_Assignment = "Self-created Task Self-Assignment";
    public static final String Self_Created_Assignment_Others = "Self-created Task Assignment To Others";

    public static class UpdateTeam {
        public static final String Task_Basic_Update = "Task Basic Update";
        public static final String Task_Essential_Update = "Task Essential Update";
        public static final String All_Task_Basic_Update = "All Task Basic Update";
        public static final String All_Task_Essential_Update = "All Task Essential Update";
        public static final String Team_Task_View = "Team Task View";
    }

    public static class CommentsWithPage {
        public static final Integer pageSize = 15;
    }

    public static class EntityTypeNames {
        public static final String Entity_Type_TEAM = "Team";
    }

    public static class ActionId {
        public static final Integer TASK_ADD_WITHOUT_ASSIGNMENT = 9;
        public static final Integer TASK_BASIC_UPDATE = 1;
        public static final Integer TEAM_TASK_VIEW = 2;
        public static final Integer ALL_TASK_BASIC_UPDATE = 4;
        public static final Integer ALL_TASK_ESSENTIAL_UPDATE = 5;
        public static final Integer DEACTIVATE_ACTIVATE_USER_ACCOUNT =  901;
        public static final Integer DEACTIVATE_ACTIVATE_USER_ORGANIZATION =  902;
        public static final Integer MANAGE_REGISTRATION =  903;
        public static final Integer MANAGE_LEAVE = 1001;
        public static final Integer VIEW_TIMESHEET = 1002;
        public static final Integer VIEW_ATTENDENCE = 1003;
        public static final Integer MANAGE_GEOFENCE_ADMIN_PANEL = 1004;
        public static final Integer VIEW_GEOFENCE_ATTENDENCE = 1005;
    }

    public static class flags {
        public static final String TASK_MASTER = "task master";
        public static final String STATS_MY_TASKS = "my tasks";
        public static final String STATS_ALL_TASKS = "all tasks";
    }

    public static class WebSocket {
        public static final String USER_DESTINATION_PREFIX = "/ws-user/";
        public static final String SIMPLE_BROKER1 = "/ws-topic";
        public static final String SIMPLE_BROKER2 = "/ws-user";
        public static final String APPLICATION_DESTINATION_PREFIXES = "/ws-app";
        public static final String STOMP_END_POINTS = "/ws-notification";
        public static final String SEND_MESSAGE_DESTINATION = "/greetings";
    }

    public static class OrgIds {
        public static final Integer PERSONAL = 0;
    }

    public static final Long PERSONAL_TEAM_ID = 0L;

    public static final Long PERSONAL_PROJECT_ID = 0L;

    public static final Long PERSONAL_BU_ID = 0L;
    public static class NotificationType{
        public static final List<Integer> TASK= List.of(1,2,3,4,5,6,7,8,9,10,11);
        public static final List<Integer> MEETING= List.of(12,13,14);
        public static final List<Integer> TIMESHEET= List.of(15);
        public static final List<Integer> WORKFLOW_TASK_STATUS=List.of(1,8,15,22,29,36,43,50);
        public static final String GROUP_MESSAGE = "GROUP_MESSAGE";
        public static final String CREATE_TASK="CREATE_TASK";
        public static final String NEW_TASK_TITLE="New Task Created";
        public static final String UPDATE_TASK_TITLE = "UPDATE_TASK_TITLE";
        public static final String UPDATE_TASK_DESC = "UPDATE_TASK_DESC";
        public static final String UPDATE_TASK_ESTIMATE="UPDATE_TASK_ESTIMATE";
        public static final String UPDATE_TASK_PERCEIVED_TIME_COMPLETION="UPDATE_TASK_PERCEIVED_TIME_COMPLETION";
        public static final String UPDATE_TASK_SCHEDULE="UPDATE_TASK_SCHEDULE";
        public static final String UPDATE_TASK_CURRENT_ACTIVITY="UPDATE_TASK_CURRENT_ACTIVITY";
        public static final String UPDATE_TASK_PROGRESS="UPDATE_TASK_PROGRESS";
        public static final String UPDATE_TASK_EFFORT="UPDATE_TASK_EFFORT";
        public static final String UPDATE_TASK_STAKEHOLDERS="UPDATE_TASK_STAKEHOLDERS";
        public static final String UPDATE_TASK_PRIORITY="UPDATE_TASK_PRIORITY";
        public static final String UPDATE_TASK_KEY_DECISIONS="UPDATE_TASK_KEY_DECISIONS";
        public static final String UPDATE_TASK_COMMENTS="UPDATE_TASK_COMMENTS";
        public static final String UPDATE_TASK_NOTES="UPDATE_TASK_NOTES";
        public static final String UPDATE_TASK_ASSIGNMENT="UPDATE_TASK_ASSIGNMENT";
        public static final String TASK_GETTING_DELAYED="TASK_GETTING_DELAYED";
        public static final String TASK_GETTING_WATCHLISTED="TASK_GETTING_WATCHLISTED";
        public static final String MEETING_INVITE="MEETING_INVITE";
        public static final String MEETING_UPDATE="MEETING_UPDATE";
        public static final String MEETING_REMINDER="MEETING_REMINDER";
        public static final String MEETING_FOLLOW_UP="MEETING_FOLLOW_UP";
        public static final String TIMESHEET_REMINDER="TIMESHEET_REMINDER";
        public static final String UPDATE_TASK_WORKFLOW_STATUS="UPDATE_TASK_WORKFLOW_STATUS";
        public static final String UPDATE_TASK_MARK_COMPLETED="UPDATE_TASK_MARK_COMPLETED";
        public static final String UPDATE_TASK_CURR_SCHEDULED_TASK_IND="UPDATE_TASK_CURR_SCHEDULED_TASK_IND";
        public static final String UPDATE_TASK_RECORD_VOICE_STATUS = "UPDATE_TASK_RECORD_VOICE_STATUS";
        public static final String UPDATE_TASK_CURR_ACTIVITY_IND="UPDATE_TASK_CURR_ACTIVITY_IND";
        public static final String UPDATE_TASK_IN_BACKLOG="UPDATE_TASK_IN_BACKLOG";
        public static final String UPDATE_TASK_TIMESHEET="UPDATE_TASK_TIMESHEET";
        public static final String MEETING_INVITE_TITLE = "You have received a meeting invitation.";
        public static final String UPDATE_TASK_OTHER_ACTIVITIES = "UPDATE_TASK_OTHER_ACTIVITIES";
        public static final String START_MEETING_CONFIRMATION = "START_MEETING_CONFIRMATION";
        public static final String END_MEETING_CONFIRMATION = "END_MEETING_CONFIRMATION";

        public static final String LEAVE_APPROVAL = "LEAVE_APPROVAL";
        public static final String LEAVE_CANCELLED = "LEAVE_CANCELLED";
        public static final String LEAVE_REJECTED = "LEAVE_REJECTED";
        public static final String LEAVE_APPLIED = "LEAVE_APPLIED";
        public static final String LEAVE_UPDATED = "LEAVE_UPDATED";
        public static final String LEAVE_APPROVAL_TITLE = " Leave has been approved.";
        public static final String LEAVE_CANCELLED_TITLE = " Leave has been cancelled.";
        public static final String LEAVE_APPLIED_TITLE = " Leave has been applied";
        public static final String LEAVE_UPDATED_TITLE = " Leave has been updated";

        public static final String LEAVE_CANCELLATION_REQUEST = " Leave cancellation request.";
        public static final String LEAVE_REJECTED_TITLE = " Leave has been rejected.";

        public static final String NEW_ACCESS_DOMAIN = "NEW_ACCESS_DOMAIN";
        public static final String UPDATED_ACCESS_DOMAIN = "UPDATED_ACCESS_DOMAIN";
        public static final String UPDATED_USER_NAME = "UPDATED_USER_NAME";
        public static final String IMMEDIATE_ATTENTION = "IMMEDIATE_ATTENTION";
        public static final String ORG_REQUEST = "ORG_REQUEST";
        public static final String TASK_REMINDER = "TASK_REMINDER";
        public static final String MENTION_ALERTS_GC = "MENTION_ALERTS_GC";
        public static final String MENTION_ALERTS_TC = "MENTION_ALERTS_TC";
        public static final String EFFORT_ALERTS = "EFFORT_ALERTS";
        public static final String USER_REMOVE_UPDATE = "USER_REMOVE_UPDATE";
        public static final String USER_PREFERENCE_UPDATE = "USER_PREFERENCE_UPDATE";
        public static final String USER_REMINDER = "USER_REMINDER";
        public static final String TEMPLATE_UPDATE = "TEMPLATE_UPDATE";
        public static final String STATUS_INQUIRY = "STATUS_INQUIRY";
        public static final String IMPORT_JIRA = "IMPORT_JIRA";
        public static final String JIRA_INVITE_DECLINE = "JIRA_INVITE_DECLINE";
        public static final String CHECK_IN_NOTIFICATION = "CHECK_IN_NOTIFICATION";
        public static final String PUNCH_REQUEST = "PUNCH_REQUEST";
        public static final String OTHERS = "OTHERS";

        public static final String LEAVE_EXPIRED = "LEAVE_EXPIRED";

        public static final String LEAVE_APPROVAL_REMINDER = "LEAVE_APPROVAL_REMINDER";

        public static final String EPIC_STARTED = "EPIC_STARTED";
        public static final String MEETING_ANALYSIS = "MEETING_ANALYSIS";
        public static final String FLAGGED_UNFLAGGED_NOTIFICATION = "FLAGGED_UNFLAGGED_NOTIFICATION";

        public static final List<String> NotificationTaskUpdationDetails = List.of(NotificationType.UPDATE_TASK_TITLE, NotificationType.UPDATE_TASK_DESC, NotificationType.UPDATE_TASK_ESTIMATE, NotificationType.IMMEDIATE_ATTENTION, NotificationType.MENTION_ALERTS_TC, NotificationType.UPDATE_TASK_SCHEDULE, NotificationType.UPDATE_TASK_PRIORITY, NotificationType.UPDATE_TASK_PROGRESS);

        public static class NewAccessDomain{
            public static String title(String teamName) {
                teamName = !teamName.toLowerCase().contains("team") ? teamName + " Team" : teamName;
                return "You've been added to "+teamName;
            }
            public static String body(String teamName,String orgName, String firstName, String lastName, String roleName) {
                String temp = !teamName.toLowerCase().contains("team") ? " Team" : "";
                return "You've been added to \""+ teamName+"\" "+ temp +" in \""+orgName+"\" Organization  by "+firstName+" "+lastName+" as "+roleName+" role";
            }
        }
        public static class updateAccessDomain{
            public static String title(String teamName) {
                teamName = !teamName.toLowerCase().contains("team") ? teamName + " Team" : teamName;
                return "You've been removed from "+teamName;
            }
            public static String body(String roleName,String teamName) {
                return " Your role as \""+roleName+ "\" has been removed from team \""+teamName+"\".";
            }
        }

        public static class updateAccessDomainForAddRole{
            public static String title(String teamName) {
                teamName = !teamName.toLowerCase().contains("team") ? teamName + " Team" : teamName;
                return "Your role is updated in "+teamName;
            }
            public static String body(String teamName,String orgName, String firstName, String lastName, String roleName) {
                String temp =  !teamName.toLowerCase().contains("team") ? " Team" : "";
                return "Your role is updated in \""+ teamName+"\""+ temp +" in \""+orgName+"\" Organization  by "+firstName+" "+lastName+" as "+roleName+" role";
            }
        }
    }

    public static class EntityPreference {
        public static final String DATE_NOT_NULL = "Holiday date is required";
        public static final String DESCRIPTION_NOT_NULL = "Holiday description is required";

        public static final String ENTITY_TYPE_ID = "Entity type Id can not be null";

        public static final String ENTITY_ID = "Entity id can not be null";
    }

    public static class ScrollToType{
        public static final String SCROLL_NOT_REQUIRED = "SCROLL_NOT_REQUIRED";
        public static final String TASK_ESTIMATE = "TASK_ESTIMATE";

        public static final String TASK_PERCEIVED_TIME_TO_COMPLETION = "TASK_PERCEIVED_TIME_TO_COMPLETION";
        public static final String TASK_EXP_END_DATETIME = "TASK_EXP_END_DATETIME";
        public static final String TASK_EXP_START_DATETIME="TASK_EXP_START_DATETIME";
        public static final String TASK_CURRENT_ACTIVITY = "TASK_CURRENT_ACTIVITY";
        public static final String TASK_ACT_START_DATETIME = "TASK_ACT_START_DATETIME";
        public static final String TASK_ACT_END_DATETIME = "TASK_ACT_END_DATETIME";
        public static final String TASK_WORKFLOW_STATUS = "TASK_WORKFLOW_STATUS";
        public static final String TASK_RECORDED_EFFORT = "TASK_RECORDED_EFFORT";
        public static final String TASK_OBSERVER = "TASK_OBSERVER";
        public static final String TASK_MENTOR = "TASK_MENTOR";
        public static final String TASK_PRIORITY = "TASK_PRIORITY";
        public static final String TASK_KEY_DECISIONS="TASK_KEY_DECISIONS";
        public static final String TASK_COMMENTS="TASK_COMMENTS";
        public static final String TASK_NOTES="TASK_NOTES";
        public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
        public static final String TASK_TITLE = "TASK_TITLE";
        public static final String TASK_DESC = "TASK_DESC";
        public static final String TASK_USER_PERCEIVED_PERCENTAGE = "TASK_USER_PERCEIVED_PERCENTAGE";
        public static final String TASK_LIST_OF_DELIVERABLES_DELIVERED = "TASK_LIST_OF_DELIVERABLES_DELIVERED";
        public static final String TASK_CURRENTLY_SCHEDULED_TASK_INDICATOR ="TASK_CURRENTLY_SCHEDULED_TASK_INDICATOR";
        public static final String TASK_STATE = "TASK_STATE";

        public static final String TASK_RECORD_VOICE_STATUS = "TASK_RECORD_VOICE_STATUS";
        public static final String TEMPLATE = "TEMPLATE";
        public static final String STATUS_INQUIRY = "STATUS_INQUIRY";

    }
    public static class MeetingMode{
        public static final String ONLINE="ONLINE";
        public static final String OFFLINE="OFFLINE";
        public static final String HYBRID="HYBRID";
    }
    public static class MeetingReminder{

        public static String title(int reminderTime, String meetingNumber){
            return "Meeting "+meetingNumber+" starting in "+reminderTime+((reminderTime==1)?" minute.":" minutes.");
        }
        public static String body(String venue,int reminderTime, String meetingNumber){

            if(venue == null) {
                return "Your online meeting "+meetingNumber+" will be starting in "+reminderTime+((reminderTime==1)?" minute ":" minutes ");
            }
            return "Your meeting "+meetingNumber+" will be starting in "+reminderTime+((reminderTime==1)?" minute ":" minutes ")+" at \""+venue+"\". ";
        }
    }
    public static class MeetingFollowUp{
        public static final String title = "Meeting follow up!";

        public static String body(String meetingNumber){
            return "Did you attend the meeting "+meetingNumber+" ?";
        }
    }

    public static class TimeSheetReminder{
        public static final String title = "Timesheet Reminder";
        public static final String body = "It looks like you have not filled your timesheet yet! Please fill it soon.";
    }
    public static class DayBeforeTimeSheetReminder{
        public static final String title = "Timesheet Reminder";
        public static final String body = "It looks like you have not filled your timesheet yesterday! Please fill it before office starts.";
    }

    public static class TaskState{
        public static String title(String notificationType){
            if(notificationType.equals(NotificationType.TASK_GETTING_DELAYED)){
                return "Work Item getting Delayed";
            }
            if(notificationType.equals(NotificationType.TASK_GETTING_WATCHLISTED)){
                return "Work Item in Watchlist";
            }
            return notificationType;
        }

        public static String body(String taskNumber,String notificationType){
            if(notificationType.equals(NotificationType.TASK_GETTING_DELAYED))
                return "State for work item "+taskNumber+" is changed to delayed.";
            else
                return "State for work item "+taskNumber+" is changed to watch-listed.";
        }
    }

    public static class ConstantForGettingOfficeHourEndTimeForOrg{
        public static LocalTime beforeOfficeStartTime = LocalTime.of(5,30,0);
        public static LocalTime beforeOfficeStartTimeLimit = LocalTime.of(5, 31,0);
        public static LocalTime beforeOfficeEndTime = LocalTime.of(15,30,0);
        public static LocalTime beforeOfficeEndTimeLimit = LocalTime.of(15, 31,0);
        public static LocalTime afterOfficeEndTime = LocalTime.of(16,15,0);
        public static LocalTime afterOfficeEndTimeLimit = LocalTime.of(16, 16,0);
    }

    public static class StartMeetingConfirmation {
//        public static final String body = "Is meeting started?";
        public static String body(String meetingNumber){
            return "Is meeting "+meetingNumber+" started?";
        }
    }
    public static class EndMeetingConfirmation {
        public static String body(String meetingNumber){
            return "Is meeting "+meetingNumber+" ended?";
        }
    }

    public static List<Integer> DEFAULT_WORKFLOW=new ArrayList<>(List.of(1,3));
    public static ArrayList<String> nonEditableFieldsInUserProfile = new ArrayList<>(List.of(
            "primaryEmail",
            "isPrimaryEmailPersonal",
//            "alternateEmail",
//            "isAlternateEmailPersonal",
//            "personalEmail",
            "currentOrgEmail"
    ));

    public static class BlockedMessages {
        public static final String INPUT_FROM_EXTERNAL_SOURCE = "waiting for input from an external source";
        public static final String INPUT_FROM_INTERNAL_SOURCE = "waiting for input from a team member";
        public static final String PARENT_TASK_BLOCKED = "parent task is blocked";
        public static final Integer INPUT_FROM_EXTERNAL_SOURCE_ID = 1;
        public static final Integer INPUT_FROM_INTERNAL_SOURCE_ID = 2;
        public static final Integer OTHER_REASON_ID = 3;
        public static final Integer PARENT_TASK_BLOCKED_ID = 4;
    }

    public static class BlockedType{
        public static final Integer EXTERNAL_SOURCE = 1;  //waiting from input from external source
        public static final Integer INTERNAL_TEAM_MEMBER = 2;   //waiting for input from internal team member
        public static final Integer OTHER_REASON_ID = 3;   //other reason
        public static final Integer PARENT_TASK_BLOCKED_ID = 4;   //parent
        public static final Integer INTERNAL_TO_ORG = 5;    //internal to org external to team
    }

    public static class TaskReminder {

        public static String EMAIL = "email";
        public static String NOTIFICATION = "notification";
        public static String BOTH_EMAIL_AND_NOTIFICATION = "both";

        public static String title(String TaskNumber){
            return "Work Item "+TaskNumber+" is blocked ";
        }
        public static String body(String taskNumber, String text){
            return text;
        }
    }

    public static final class BooleanValues {
        public static final Integer BOOLEAN_TRUE = 1;
        public static final Integer BOOLEAN_FALSE = 0;
    }

    @Getter
    public static enum MeetingPreferenceEnum {
        NO_EFFORTS(1, "No billing for attendee meeting efforts"),
        ALL_MEETING_EFFORTS(2, "Billing for all attendee meeting efforts"),
        ONLY_ASSIGNED_TO_EFFORTS(3, "Billing restricted to assigned-to meeting efforts only"),
        HYBRID_EFFORTS(4,"Billing for assigned-to and mentor efforts combined");

        private final Integer meetingPreferenceId;
        private final String meetingPreference;

        MeetingPreferenceEnum(Integer meetingPreferenceId, String meetingPreference) {
            this.meetingPreferenceId = meetingPreferenceId;
            this.meetingPreference = meetingPreference;
        }

        public static MeetingPreferenceEnum getById(Integer id) {
            for (MeetingPreferenceEnum preference : MeetingPreferenceEnum.values()) {
                if (Objects.equals(preference.getMeetingPreferenceId(), id)) {
                    return preference;
                }
            }
            return null; // Return null if no match is found
        }
    }

    @Getter
    public static enum ReferencedMeetingReasonEnum {
        UNCLEAR_REQUIREMENTS(1, "Requirements are not clear"),
        TECHNICAL_ISSUES(2, "Facing some technical issues"),
        COLLABORATION(3, "Collaboration meeting"),
        OTHER(4,"Other");

        private final Integer meetingReasonId;
        private final String meetingReason;

        ReferencedMeetingReasonEnum(Integer meetingReasonId, String meetingReason) {
            this.meetingReasonId = meetingReasonId;
            this.meetingReason = meetingReason;
        }

        public static ReferencedMeetingReasonEnum getById(Integer id) {
            for (ReferencedMeetingReasonEnum reason : ReferencedMeetingReasonEnum.values()) {
                if (Objects.equals(reason.getMeetingReasonId(), id)) {
                    return reason;
                }
            }
            return null; // Return null if no match is found
        }

    }

    @Getter
    public static enum SprintStatusEnum {
        NOT_STARTED(1, "Not Started"),
        STARTED(2, "Started"),
        COMPLETED(3,"Completed"),
        DELETED(4, "Deleted");

        private final Integer sprintStatusId;
        private final String sprintStatus;

        SprintStatusEnum(Integer sprintStatusId, String sprintStatus) {
            this.sprintStatusId = sprintStatusId;
            this.sprintStatus = sprintStatus;
        }

        public static SprintStatusEnum getById(Integer id) {
            for (SprintStatusEnum status : SprintStatusEnum.values()) {
                if (Objects.equals(status.getSprintStatusId(),id)) {
                    return status;
                }
            }
            return null; // Return null if no match is found
        }

        public static Integer getIdByName(String name) {
            for (SprintStatusEnum status : SprintStatusEnum.values()) {
                if (Objects.equals(status.getSprintStatus(),name)) {
                    return status.sprintStatusId;
                }
            }
            return 0; // Return null if no match is found
        }
    }

    public static final int FILE_NAME_MAX_LENGTH = 100;

    @Getter
    public static enum AttendanceTypeEnum {
        HOLIDAY(1, "Holiday"),
        LEAVE(2, "Leave"),
        OFF_DAY(3, "Off Day"),
        PRESENT(4,"Present"),
        PARTIAL(5,"Partial"),
        ABSENT(6,"Absent"),
        EMPTY(7,"Empty"),
        PARTIAL_LESS_THAN_50(8, "< 50% Partial Present"),
        PARTIAL_MORE_THAN_50(9, "> 50% Partial Present"),
        SICK_LEAVE(10, "Sick Leave"),
        TIME_OFF_LEAVE(11, "Time Off Leave");

        private final Integer attendanceTypeId;
        private final String attendanceType;

        AttendanceTypeEnum(Integer attendanceTypeId, String attendanceType) {
            this.attendanceTypeId = attendanceTypeId;
            this.attendanceType = attendanceType;
        }

    }

    public static final Integer DEFAULT_OFFICE_MINUTES = 540;

    public static final String PERSONAL_ORG_DEFAULT_TEAM_NAME = "Wf#h#V#i#j_Ayi_Team_DFLT";
    public static final String PERSONAL_ORG = "Personal";
    public static final String PERSONAL_ORG_TEAM_DISPLAY_NAME = "Personal";
    public static final String PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS = "Personal Tasks";

    public static final HashMap<String, String> nonEditableFieldsForCompletedTask = new HashMap<>() {
        {
            put("taskExpStartDate", "expected start date");
            put("taskActStDate", "actual start date");
            put("taskExpEndDate", "expected end date");
            put("taskActEndDate", "actual end date");
            put("taskTitle", "task title");
            put("taskPriority", "task priority");
            put("taskWorkflowId", "work flow");
            put("dependencyIds", "dependency");
            put("taskEstimate", "task estimates");
            put("isBallparkEstimate", "estimate ball park");
            put("placeOfIdentification", "place of identification");
            put("environmentId", "environment");
            put("taskDesc", "task description");
        }
    };

    public static final String DEFAULT_INDICATOR = "_DFLT";

    @Getter
    public static enum ReminderStatusEnum {
        PENDING(1, "Pending"),
        COMPLETED(2,"Completed"),
        DELETED(3, "Deleted");

        private final Integer statusId;
        private final String status;

        ReminderStatusEnum(Integer statusId, String status) {
            this.statusId = statusId;
            this.status = status;
        }

    }

    public static class UserReminder {

        public static String EMAIL = "email";
        public static String NOTIFICATION = "notification";
        public static String BOTH_EMAIL_AND_NOTIFICATION = "both";

        public static String title(String reminderTitle){
            return reminderTitle;
        }
        public static String body(String reminderDescription){
            return reminderDescription;
        }
    }

    @Getter
    public static enum AlertStatusEnum {
        UNVIEWED(1, "Unviewed"),
        VIEWED(2, "Viewed");

        private final Integer statusId;
        private final String status;

        AlertStatusEnum(Integer statusId, String status) {
            this.statusId = statusId;
            this.status = status;
        }

    }

    @Getter
    public static enum AlertTypeEnum {
        TASK(1, "Task"),
        USER(2, "User"),
        DEPENDENCY(3, "Dependency"),
        LEAVE(4, "Leave");

        private final Integer typeId;
        private final String type;

        AlertTypeEnum(Integer typeId, String type) {
            this.typeId = typeId;
            this.type = type;
        }

    }

    public static final List<String> AlertTypeList = List.of("Task", "User", "Dependency");

    @Getter
    public static enum ScreenRoleEnum {
        DASHBOARD("dashboard", null),
        ATTENDANCE("attendance", List.of(11,12,14,15,111,112,131,132,121,122)),
        CREATEQUICKTASK("createquicktask", List.of(7,8,9,10,11,12,14,15,91)),
        CREATETASK("createtask", List.of(7,8,9,10,11,12,14,15,91)),
        VIEW_TASK("viewtask", List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,91)),
        TASKMASTER("taskmaster", null),
        MYTIMESHEET("mytimesheet", null),
        TEAMTIMESHEET("teamtimesheet", List.of(9,10,11,12,13,14,15,101,102,111,112,121,122,131,132)),
        MYBOARDVIEW("myboardview", List.of(1,2,3,4,5,6,7,8,9,10,11,12,14,15,91)),
        ALLBOARDVIEW("allboardview", List.of(9,10,11,12,14,15)),
        MEETINGS("meetings", null),
        CREATEMEETINGS("createmeetings", List.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,101,102)),
        STICKYNOTES("stickynotes", null),
        LEAVES("leaves", null),
        TIMELINE("timeline", null),
        TEMPLATES("templates", List.of(9,10,11,12,14,15,131,132)),
        CREATETEAM("createteam", List.of(111,112,132,122)),
        EDITTEAM("editteam", List.of(101,102,111,112,132,122)),
        MANAGEORGANIZATION("manageorganization", List.of(131,132)),
        MANAGEPROJECTS("manageprojects", List.of(122,123,111,112,113,131,132)),
        MANAGESPRINTS("managesprints", null),
        PERFNOTE("perfnote", null),
        CREATEPERSONALTASK("createpersonaltask", List.of(91)),
        PERSONAL("personal", null),
        CREATEEPIC("createepic", List.of(10,11,12,14,15)),
        VIEWEPIC("viewepic", List.of(2,3,4,5,6,7,8,9,10,11,12,13,14,15,101,102,111,112,113)),
        LEAVEREPORT("leaveReport", List.of(11,12,14,15,16,17,132,112)),
        LEAVECONTROLS("leavecontrols", List.of(132,112)),
        MYLEAVES("myleaves", null),
        ONBOARDING("onboarding", List.of(132)),
        USERLEAVEPOLICY("userleavepolicy", List.of(132)),
        DEPENDENCIES("dependencies", List.of(2,3,4,5,6,7,8,9,10,11,12,13,14,15)),
        JIRAMIGRATION("jiramigration", List.of(112,132)),
        VIEWSPRINT("viewsprint", List.of(1,2,3,4,5,6,7,8,9,10,11,12,14,15)),
        FEATUREACCESS("featureaccess", List.of(132));

        private final List<Integer> list;
        private final String type;

        ScreenRoleEnum(String type, List<Integer> list) {
            this.list = list;
            this.type = type;
        }

        public static ScreenRoleEnum getByType(String type) {
            for (ScreenRoleEnum roles : ScreenRoleEnum.values()) {
                if (Objects.equals(roles.getType(),type)) {
                    return roles;
                }
            }
            return null; // Return null if no match is found
        }

    }

    public static final HashMap<String, String> nonEditableFieldsForTaskInNotStartedSprint = new HashMap<>() {
        {
            put("taskActStDate", "actual start date");
            put("taskActEndDate", "actual end date");
            put("taskWorkflowId", "work flow");
            put("fkWorkflowTaskStatus", "workflow status");
        }
    };

    public static final HashMap<String, String> editableFieldsForTaskInNotStartedSprintForProjectManager = new HashMap<>() {
        {
            put("taskExpStartDate", "expected start date");
            put("taskExpEndDate", "expected end date");
            put("taskTitle", "task title");
            put("taskDesc", "task desc");
            put("taskPriority", "task priority");
            put("fkAccountIdAssigned", "assigned to");
            put("isBallparkEstimate", "estimate ball park");
        }
    };

    public static final HashMap<String, String> editableFieldsForTaskInStartedSprintForProjectManager = new HashMap<>() {
        {
            put("taskExpStartDate", "expected start date");
            put("taskExpEndDate", "expected end date");
            put("taskTitle", "task title");
            put("taskPriority", "task priority");
            put("fkAccountIdAssigned", "assigned to");
        }
    };
    public static final List<Integer> rolesWithStatusInquiryRights = List.of(9, 10, 11, 12, 14, 15);

    public static final List<Integer> defaultRolesWithPerfNoteRights = List.of(10, 11, 12, 14, 15);

    public static final List<Integer> DEFAULT_ROLE_IDS_FOR_STARRING_WORK_ITEM = List.of(11, 12, 14, 15);

    public static class LeaveApplicationStatusIds {
        public static final Short WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID = 1;
        public static final Short WAITING_CANCEL_LEAVE_APPLICATION_STATUS_ID = 2;
        public static final Short APPROVED_LEAVE_APPLICATION_STATUS_ID = 3;
        public static final Short REJECTED_LEAVE_APPLICATION_STATUS_ID = 4;
        public static final Short CANCELLED_LEAVE_APPLICATION_STATUS_ID = 5;

        public static final Short CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID = 6;
        public static final Short LEAVE_APPLICATION_EXPIRED_STATUS_ID = 7;
        public static final Short CONSUMED_LEAVE_APPLICATION_STATUS_ID = 8;
    }

    public static final String NOTIFY_FOR_LEAVE_EXPIRY = "EXPIRY";

    public static final String NOTIFY_FOR_LEAVE_CANCELLED = "CANCELLED_AFTER_APPROVAL";

    public static final String NOTIFY_FOR_APPROVED = "APPROVED";

    public static final String WAITING_FOR_CANCEL = "WAITING_CANCEL";

    public static final String LEAVE_CANCELLED = "CANCELLED";
    public static final String LEAVE_EXPIRED_TITLE = " Leave application expired.";

    public static final String LEAVE_APPROVAL_REMINDER_TITLE = "Unapproved leave application is waiting for you approval.";
    public static final LocalTime NOTIFICATION_TIME_FOR_LEAVE_APPROVAL_REMINDER = LocalTime.of(15,30,0);

    public static class DefaultEntitiesCount {

        public static final Integer maxOrgCount = 1;

        public static final Integer maxBuCount = 1;

        public static final Integer maxProjectCount = 2;

        public static final Integer maxTeamCount = 5;

        public static final Integer maxUserCount = 10;

        public static final Long maxMemoryQuota = 1572864000L; //in bytes
    }
    @Getter
    public static enum EpicStatusEnum {
        STATUS_BACKLOG(1, "Backlog"),
        STATUS_IN_REVIEW(2, "In-Review"),
        STATUS_REVIEWED(3, "Reviewed"),
        STATUS_IN_PROGRESS(4, "In-Progress"),
        STATUS_BLOCKED(5, "Blocked"),
        STATUS_COMPLETED(6, "Completed"),
        STATUS_DELETED(7, "Deleted"),
        STATUS_ON_HOLD(8, "On-Hold");

        private final Integer workflowEpicStatusId;
        private final String workflowEpicStatus;

        EpicStatusEnum(Integer workflowEpicStatusId, String workflowEpicStatus) {
            this.workflowEpicStatusId = workflowEpicStatusId;
            this.workflowEpicStatus = workflowEpicStatus;
        }

        public static EpicStatusEnum getById(Integer id) {
            for (EpicStatusEnum status : EpicStatusEnum.values()) {
                if (Objects.equals(status.getWorkflowEpicStatusId(),id)) {
                    return status;
                }
            }
            return null; // Return null if no match is found
        }
    }

    public static class WorkflowEpicStatusConstants {
        public static final String STATUS_REVIEWED = "Reviewed";
        public static final String STATUS_IN_REVIEW = "In-Review";
        public static final String STATUS_COMPLETED = "Completed";
        public static final String STATUS_BACKLOG = "Backlog";
        public static final String STATUS_IN_PROGRESS = "In-Progress";
        public static final String STATUS_DELETED = "Deleted";
        public static final String STATUS_BLOCKED = "Blocked";
        public static final String STATUS_ON_HOLD = "On-Hold";
    }

    public static final List<Integer> ROLES_WITH_TEAM_EFFORT_EDIT_ACCESS = List.of(11,12,14,15);

    public static final List<Short> LEAVE_TYPE = List.of((short)1,(short)2);
    public static final Short TIME_OFF_LEAVE_TYPE_ID = 1;
    public static final Short SICK_LEAVE_TYPE_ID = 2;
    public static final Float HalfDayLeaveDuration = 0.5f;

    public static class SprintField {
        public static final String SPRINT_TITLE = "sprintTitle";
        public static final String SPRINT_OBJECTIVE = "sprintObjective";
        public static final String EXP_START_DATE = "sprintExpStartDate";
        public static final String EXP_END_DATE = "sprintExpEndDate";
        public static final String CAPACITY_ADJUSTMENT_DEADLINE= "capacityAdjustmentDeadline";
        public static final String NEXT_SPRINT= "nextSprintId";
        public static final String PREVIOUS_SPRINT = "previousSprintId";
        public static final String MODIFY_ESTIMATE= "canModifyEstimates";
        public static final String ACTIVE_INDICATOR = "canModifyIndicatorStayActiveInStartedSprint";
    }

    public static final List<Integer> ACTIVE_AND_FUTURE_SPRINT_STATUS_LIST = List.of(1, 2);

    public static class LeaveTypeNameConstant {
        public static final String SICK_LEAVE = "Sick Leave";
        public static final String TIME_OFF = "Time Off Leave";
    }

    public static HashMap<Integer, String> LeaveTypeIdToNameMap = new HashMap<Integer, String>() {{
        put(1, "One Day ");
        put(2, "Multiple Days ");
        put(3, "Half Day ");
    }};

    public static HashMap<Integer, String> LeaveTabIdToNameMap = new HashMap<Integer, String>() {{
        put(0, "leavecontrols");
        put(1, "leaveReport");
        put(2, "myleaves");
        put(3, "onboarding");
        put(4, "userleavepolicy");
    }};

    public static final HashMap<Integer, String> taskTypeMap = new HashMap<>() {
        {
            put(1, "Task");
            put(2, "Parent Task");
            put(3, "Child Task");
            put(4, "Bug Task");
        }
    };

    @Getter
    public static enum AuditStatusEnum {
        ADD(1, "added"),
        UPDATE(2, "updated"),
        REMOVE(3, "removed"),
        RE_ADD(4, "re-added"),
        DEACTIVATE(5, "deactivated"),
        REACTIVATE(6, "reactivated");

        private final Integer typeId;
        private final String type;

        AuditStatusEnum(Integer typeId, String type) {
            this.typeId = typeId;
            this.type = type;
        }

    }

    public static class TaskHandlingStrategy {
        public static final int IGNORE_TASK = 1;
        public static final int IGNORE_LOGGED_TIME = 2;
        public static final int BASED_ON_LOGGED_TIME = 3;
    }

    public static class JiraStatus {
        public static final String TO_DO = "To Do";
        public static final String IN_PROGRESS = "In Progress";
        public static final String DONE = "Done";
        public static final String TESTABLE = "Testable";
        public static final String IN_REVIEW = "In Review";
    }

    public static final List<String> JIRA_STATUS_LIST = List.of("To Do", "In Progress", "Done", "Testable", "In Review");
    public static final List<String> JIRA_USER_FILE_HEADERS = List.of("User id", "User name", "email");
    public static final List<String> JIRA_INVALID_ISSUE_TYPE_LIST = List.of("epic", "initiative", "theme");  // Always enter value in small case

    public static class WorkFlowStatusTeamTaskStatusId {
        public static final Integer BACKLOG = 15;
        public static final Integer NOT_STARTED = 16;
        public static final Integer STARTED = 17;
        public static final Integer BLOCKED = 18;
        public static final Integer ON_HOLD = 19;
        public static final Integer COMPLETED = 20;
        public static final Integer DELETED = 21;
    }

    public static Integer TEAM_WORK_FLOW_TYPE_ID = 3;

    public static class JiraWorkItemIssueType {
        public static final String TASK = "Task";
        public static final String NEW_FEATURE = "New Feature";
        public static final String IMPROVEMENT = "Improvement";
        public static final String BUG = "Bug";
        public static final String CHILD_TASK = "Sub-task";
    }

    public static final List<String> JIRA_TASK_ISSUE_TYPE_LIST = List.of("Task", "New Feature", "Improvement", "Feature Request");

    public static class JiraTaskPriority {
        public static final String HIGHEST = "Highest";
        public static final String HIGH = "High";
        public static final String MEDIUM = "Medium";
        public static final String LOW = "Low";
        public static final String LOWEST = "Lowest";
    }

    public static class HourDistributionEntityTypes {
        public static final String WORK_ITEM = "Task/Child Task";
        public static final String BUG = "Bug";
        public static final String MEETING = "Meeting";
        public static final String OTHER_ENTITY = "Leaves/Holidays";
    }

    public static class ConversationsGroupTypes {
        public static final String ORG = "SYSTEM_ORG";
        public static final String PROJ = "SYSTEM_PROJ";
        public static final String TEAM = "SYSTEM_TEAM";
    }

    public static final List<Integer> TEAM_ADMIN_ROLE = List.of(13, 101, 102);
    public static final List<Integer> PROJECT_ADMIN_ROLE = List.of(111, 112, 113);
    public static final List<Integer> BU_ADMIN_ROLE = List.of(121, 122, 123);
    public static final List<Integer> ORG_ADMIN_ROLE = List.of(131, 132, 133);
    public static final List<Integer> TEAM_NON_ADMIN_ROLE = List.of(1,2,3,4,5,6,7,8,9,10,11,12,14,15);

    @Getter
    public static enum DeleteWorkItemReasonEnum {
        DUPLICATE(1, "Duplicate work item"),
        CHANGE_IN_REQUIREMENT(2, "Change in requirement"),
        NO_LONGER_REQUIRED(3, "No longer required"),
        OTHERS(4, "Others");

        private final Integer typeId;
        private final String type;

        DeleteWorkItemReasonEnum(Integer typeId, String type) {
            this.typeId = typeId;
            this.type = type;
        }

    }
    public static final List<Integer> DELETED_WORK_ITEM_REASON_ID = List.of(1, 2, 3, 4);

    @Getter
    public static enum RCAEnum {
        RCA_REQUIRED(1, "RCA Required"),
        RCA_DOES_NOT_REQUIRED(2, "RCA does not required"),
        MAY_BE_RCA_REQUIRED(3, "May be RCA required");

        private final Integer typeId;
        private final String type;

        RCAEnum(Integer typeId, String type) {
            this.typeId = typeId;
            this.type = type;
        }
    }
    public static final List<Integer> RCA_ID_LIST = List.of(1, 2, 3);

    public static class InviteBaseDomain {
        public static final String VERIFY_DOMAIN = "/auth/verify";
        public static final String INVITE_DOMAIN = "/register";
        public static final String INVITE_TO_TEAM_DOMAIN = "/addToTeamByInvite";
    }

    @Getter
    public static enum Environment {
        QA(1, "QA"),
        PRE_PROD(2, "Pre-Prod"),
        PROD(3, "Prod");

        private final Integer typeId;
        private final String type;

        Environment(Integer typeId, String type) {
            this.typeId = typeId;
            this.type = type;
        }
    }


    public static class ReferenceMeetingDialogBox{
        public static final Integer NOTIFY = 0;
        public static final Integer PUT_EFFORT = 1;
        public static final Integer RESEND_NOTIFICATION_COOLDOWN_TIME = 24;
    }

    public static class DepartmentType {
        public static final Integer HR_DEPARTMENT = 1;
    }
    public static class RoleTypeForFeatureAccess {
        public static final Integer TEAM_VIEWER=13;
    }

    public static class LeaveTabs{
        public static final Integer LEAVE_CONTROLS=0;
        public static final Integer LEAVE_REPORT=1;
        public static final Integer MY_LEAVES=2;
        public static final Integer ONBOARDING=3;
        public static final Integer USER_LEAVE_POLICY=4;
    }
    public static final Integer MAX_NUMBER_OF_RECURRING_MEETING = 100;

    public static final HashMap<String, String> TaskFieldNames = new HashMap<>() {{
        put("taskId", "Task Id");
        put("recordedEffort", "Recorded Effort");
        put("newEffortTracks", "New Effort Tracks");
        put("sprintId", "Sprint");
        put("attachments", "Attachments");
        put("taskTitle", "Work Item Title");
        put("taskNumber", "Work Item Number");
        put("taskIdentifier", "Work Item Identifier");
        put("buId", "Business Unit");
        put("taskDesc", "Work Item Description");
        put("taskExpStartDate", "Expected Start Date");
        put("taskActStDate", "Actual Start Date");
        put("taskActEndDate", "Actual End Date");
        put("taskActStTime", "Actual Start Time");
        put("taskActEndTime", "Actual End Time");
        put("taskExpStartTime", "Expected Start Time");
        put("taskExpEndDate", "Expected End Date");
        put("taskExpEndTime", "Expected End Time");
        put("commentId", "Comment Id");
        put("systemGeneratedExpectedEndTime", "System Generated Expected End Time");
        put("taskCompletionDate", "Work Item Completion Date");
        put("taskCompletionTime", "Work Item Completion Time");
        put("taskWorkflowId", "Workflow Id");
        put("currentActivityIndicator", "Current Activity Indicator");
        put("taskTypeId", "Work Item Type Id");
        put("parentTaskTypeId", "Parent Work Item Type Id");
        put("childTaskList", "Child Work Item List");
        put("parentTaskResponse", "Parent Work Item Response");
        put("childTaskIds", "Child Work Item Ids");
        put("deletedChildTaskIds", "Deleted Child Work Item Ids");
        put("bugTaskRelation", "Bug Task Relation");
        put("meetingList", "Meeting List");
        put("parentTaskId", "Parent Work Item Id");
        put("referenceWorkItemList", "Reference Work Item List");
        put("referenceWorkItemId", "Reference Work Item Ids");
        put("taskEstimate", "Work Item Estimate");
        put("userPerceivedRemainingTimeForCompletion", "User Perceived Remaining Time For Completion");
        put("parkingLot", "Parking Lot");
        put("keyDecisions", "Key Decisions");
        put("acceptanceCriteria", "Acceptance Criteria");
        put("taskProgressSystem", "Work Item Progress System");
        put("taskProgressSystemLastUpdated", "Work Item Progress System Last Updated");
        put("nextTaskProgressSystemChangeDateTime", "Next Work Item Progress System Change Date Time");
        put("taskProgressSetByUser", "Work Item Progress Set By User");
        put("taskProgressSetByAccountId", "Work Item Progress Set By Account Id");
        put("taskProgressSetByAccountIdLastUpdated", "Work Item Progress Set By Account Id Last Updated");
        put("taskProgressSetByUserLastUpdated", "Work Item Progress Set By User Last Updated");
        put("taskState", "Work Item State");
        put("currentlyScheduledTaskIndicator", "Currently Scheduled Work Item Indicator");
        put("unplannedScheduledTaskIndicator", "Unplanned Scheduled Work Item Indicator");
        put("taskPriority", "Work Item Priority");
        put("version", "Version");
        put("createdDateTime", "Created Date Time");
        put("lastUpdatedDateTime", "Last Updated Date Time");
        put("taskDependency", "Work Item Dependency");
        put("systemDerivedEndTs", "System Derived End Time");
        put("immediateAttention", "Immediate Attention");
        put("immediateAttentionFrom", "Immediate Attention From");
        put("immediateAttentionReason", "Immediate Attention Reason");
        put("userPerceivedPercentageTaskCompleted", "User Perceived Percentage Work Item Completed");
        put("userPerceivedPercentageTaskEarnedValue", "User Perceived Percentage Work Item Earned Value");
        put("earnedTimeTask", "Earned Time Work Item");
        put("increaseInUserPerceivedPercentageTaskCompleted", "Increase In User Perceived Percentage Work Item Completed");
        put("accountIdPrevAssigned1", "Previous Assigned 1");
        put("accountIdPrevAssigned2", "Previous Assigned 2");
        put("accountIdPrevAssignee1", "Previous Assignee 1");
        put("accountIdPrevAssignee2", "Previous Assignee 2");
        put("estimateTimeLogEvaluation", "Estimate Time Log Evaluation");
        put("taskCompletionImpact", "Work Item Completion Impact");
        put("isBallparkEstimate", "Ballpark Estimate");
        put("isEstimateSystemGenerated", "Is Estimate System Generated");
        put("environmentId", "Environment");
        put("resolutionId", "Resolution");
        put("severityId", "Severity");
        put("stepsTakenToComplete", "Steps Taken To Complete");
        put("placeOfIdentification", "Place Of Identification");
        put("customerImpact", "Customer Impact");
        put("linkedTaskList", "Linked Work Item List");
        put("fkTeamId", "Team");
        put("fkAccountId", "Account");
        put("fkEpicId", "Epic");
        put("fkProjectId", "Project");
        put("fkAccountIdCreator", "Created By");
        put("fkAccountIdAssignee", "Assignee");
        put("fkAccountIdAssigned", "Assigned To");
        put("fkAccountIdLastUpdated", "Last Updated By");
        put("fkAccountIdMentor1", "Mentor 1");
        put("fkAccountIdMentor2", "Mentor 2");
        put("fkAccountIdObserver1", "Observer 1");
        put("fkAccountIdObserver2", "Observer 2");
        put("fkOrgId", "Organization");
        put("fkWorkflowTaskStatus", "Workflow Work Item Status");
        put("comments", "Comments");
        put("noteId", "Note Id");
        put("listOfDeliverablesDeliveredId", "List Of Deliverables Delivered Id");
        put("notes", "Notes");
        put("listOfDeliverablesDelivered", "List Of Deliverables Delivered");
        put("deliverables", "Deliverables");
        put("blockedReasonTypeId", "Blocked Reason Type");
        put("blockedReason", "Blocked Reason");
        put("fkAccountIdRespondent", "Respondent");
        put("reminderInterval", "Reminder Interval");
        put("nextReminderDateTime", "Next Reminder Date Time");
        put("dependentTaskDetailResponseList", "Dependent Work Item Detail Response List");
        put("dependentTaskDetailRequestList", "Dependent Work Item Detail Request List");
        put("dependencyIds", "Dependency");
        put("labels", "Labels");
        put("labelsToAdd", "Labels To Add");
        put("taskLabels", "Work Item Labels");
        put("recordedTaskEffort", "Recorded Work Item Effort");
        put("totalEffort", "Total Effort");
        put("totalMeetingEffort", "Total Meeting Effort");
        put("billedMeetingEffort", "Billed Meeting Effort");
        put("meetingEffortPreferenceId", "Meeting Effort Preference Id");
        put("isSprintChanged", "Is Sprint Changed");
        put("fkAccountIdBugReportedBy", "Bug Reported By");
        put("isBug", "Is Bug");
        put("prevSprints", "Previous Sprints");
        put("countChildInternalDependencies", "Count Child Internal Dependencies");
        put("countChildExternalDependencies", "Count Child External Dependencies");
        put("statusAtTimeOfDeletion", "Status At Time Of Deletion");
        put("deletionReasonId", "Deletion Reason Id");
        put("duplicateWorkItemNumber", "Duplicate Work Item Number");
        put("deletedReason", "Deleted Reason");
        put("rcaId", "Root Cause Analysis");
        put("isRcaDone", "Is RCA Done");
        put("rcaReason", "RCA Reason");
        put("rcaIntroducedBy", "RCA Introduced By");
        put("rcaMemberAccountIdList", "RCA Member Account Id List");
        put("releaseVersionName", "Release Version Name");
        put("isStarred", "Is Starred");
        put("fkAccountIdStarredBy", "Starred By");
        put("meetingNotificationSentTime", "Meeting Notification Sent Time");
    }};

    @Getter
    public static enum Model {
        MODEL_1(1, "Model-1"),
        MODEL_2(2, "Model-2"),
        MODEL_3(3, "Both model");

        private final Integer typeId;
        private final String type;

        Model(Integer typeId, String type) {
            this.typeId = typeId;
            this.type = type;
        }

        public static String getTypeById(Integer typeId) {
            if (typeId == null) {
                return null;
            }
            for (Model model : Model.values()) {
                if (model.getTypeId().equals(typeId)) {
                    return model.getType();
                }
            }
            return null;
        }
    }

    public static final List<Integer> MODEL_ID_LIST = List.of(1, 2, 3);
    public static final String DEFAULT_TIME_ZONE = "Asia/Calcutta";

    public static List<Integer> projectAccess = List.of(RoleEnum.PROJECT_MANAGER_SPRINT_PROJECT.getRoleId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT_PROJECT.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());

    public static List<Integer> teamAccess = List.of(RoleEnum.FORMAL_TEAM_BASIC_USER.getRoleId(), RoleEnum.FORMAL_TEAM_INTERMEDIATE_USER.getRoleId(), RoleEnum.FORMAL_TEAM_LEAD_LEVEL_1.getRoleId(), RoleEnum.FORMAL_TEAM_SENIOR_USER_LEVEL_1.getRoleId(), RoleEnum.FORMAL_TEAM_SENIOR_USER_LEVEL_2.getRoleId(), RoleEnum.FORMAL_TEAM_SENIOR_USER_LEVEL_2.getRoleId(), RoleEnum.FORMAL_TEAM_LEAD_LEVEL_2.getRoleId(), RoleEnum.TEAM_MANAGER_NON_SPRINT.getRoleId(), RoleEnum.TEAM_MANAGER_SPRINT.getRoleId(), RoleEnum.TEAM_VIEWER.getRoleId());

    public static List<Integer> blockedTypesList = List.of(BlockedType.EXTERNAL_SOURCE, BlockedType.INTERNAL_TEAM_MEMBER, BlockedType.OTHER_REASON_ID, BlockedType.INTERNAL_TO_ORG);

    public static List<Integer> actionList = Arrays.asList(
            Constants.ActionId.MANAGE_LEAVE,
            Constants.ActionId.VIEW_TIMESHEET,
            Constants.ActionId.VIEW_ATTENDENCE,
            ActionId.MANAGE_GEOFENCE_ADMIN_PANEL,
            ActionId.VIEW_GEOFENCE_ATTENDENCE
    );

    public static class AdminRoles {
        public static final Integer ORG_ADMIN = 132;
        public static final Integer BACKUP_ORG_ADMIN = 131;
        public static final Integer SUPER_ADMIN = 900;
    }

    public static final List<Integer> adminRolesList = List.of(AdminRoles.ORG_ADMIN, AdminRoles.BACKUP_ORG_ADMIN, AdminRoles.SUPER_ADMIN);

    public static final Integer Alert_Deletion_Days = 30;
}

