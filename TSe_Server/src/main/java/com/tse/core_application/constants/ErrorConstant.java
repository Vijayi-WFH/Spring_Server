package com.tse.core_application.constants;

import com.tse.core_application.model.ActionItem;

public final class ErrorConstant {

	private ErrorConstant() {}

	public static final String PRIMARY_EMAIL_ERROR = "Primary email is missing";
	public static final String IS_PRIMARY_EMAIL_ERROR = "Is primary email is missing";
	public static final String EMAIL_CHECK_ERROR = "Email should be in correct format";
	public static final String FIRST_NAME_ERROR = "First name is missing";
	public static final String LAST_NAME_ERROR = "Last name is missing";
	public static final String FIRST_NAME_LENGTH = "First name length should be between 1 to 50";
	public static final String LAST_NAME_LENGTH = "Last name length should be between 1 to 50";
	public static final String MIDDLE_NAME_LENGTH = "Middle name length should be between 1 to 50";
	public static final String GENDER_ERROR = "Gender is missing";
	public static final String AGE_RANGE_ERROR = "Age range is missing";
    public static final String EMAIL_LENGTH = "Email length can't be more than 70";

	public static final String CITY = "City is missing";
	public static final String HIGHEST_EDUCATION_ERROR = "Highest education is missing";
	public static final String DEVICE_OS_ERROR = "Device OS is missing";
	public static final String DEVICE_OS_VER_ERROR = "Device OS version is missing";
	public static final String DEVICE_MAKE_ERROR = "Device make is missing";
	public static final String DEVICE_MODEL_ERROR = "Device model is missing";
	public static final String DEVICE_UNIQUE_ID_ERROR = "Device unique id is missing";
	public static final String ORG_NAME_ERROR = "Organization name is missing";
	public static final String MIN_MAX_ORG_NAME = "Organization name must be between 2 to 100 characters long";
	public static final String USERNAME_ERROR = "Username is missing";
	public static final String OTP_ERROR = "Otp is missing";
	public static final String OTP_MISMATCH_ERROR = "Otp mismatched";
	public static final String USER_ALREADY_EXISTS="User Already Exists";
	public static final String Country_Error = "Country is missing";

	public static final String ORG_ID_ERROR = "Org Id is missing";

	public static final String TEAM_ID_ERROR = "Team Id is missing";

	public static final String PROJECT_ID_ERROR = "Project Id is mandatory";
	public static final String LABEL_NAME = "Label name is missing";

	public static final String LABEL_NAME_SIZE = "Max length of label name is 30 character";

	public static final String ENTITY_DETAILS = "Entity details are missing.";
	public static final String ORG_List_ERROR = "Org List is missing";

	public static final String DOMAIN = "Domain is a mandatory field";
	public static final String ENTITY_TYPE_ID = "Entity type Id is required";
	public static final String ENTITY_ID = "Entity Id is required";
	public static final String FROM_DATE = "From date is required";
	public static final String TO_DATE = "To date is required";
	public static final String BLOCK_REASON_TYPE_ID = "Block Reason Type Id Required";


	public static final class InviteError {
		public static final String FIRST_NAME = "First name is required";
		public static final String LAST_NAME = "Last name is required";
		public static final String ENTITY_ID = "Entity ID is required";
		public static final String ENTITY_TYPE_ID = "Entity type ID is required";
		public static final String SENT_DATE = "Sent date is required";
		public static final String VALIDITY_DURATION_REQUIRED = "Validity duration is required";
		public static final String VALIDITY_DURATION_RANGE = "Validity duration must be at least 0 day and max 14 days";
		public static final String EMAIL = "Email is required";
		public static final String EMAIL_FORMAT = "Email should be in correct format";

	}

	public static final class Task {
		public static final String TASK_TITLE = "task title is missing";
		public static final String TASK_DESC = "task description is missing";
		public static final String TASK_WORKFLOW_ID = "task workflow id is missing";
		public static final String fk_WORK_FLOW_TASK_STATUS_ID = "workflow task status id is missing";
		public static final String CURRENT_ACTIVITY_INDICATOR = "current activity indicator is missing";
		public static final String TASK_TYPE_ID = "task type id must not be null";
		public static final String REMINDER_LIMIT="Reminder must be at least 1 day and max 30 day";

		public static final String fk_TEAM_ID = "team id is missing";
		public static final String fk_ORG_ID = "organization id is missing";
		public static final String ACCOUNT_ID = "account id is missing";
		public static final String DESC_LIMIT = "Description Length should be between 3 and 5000 characters";
		public static final String DESC_LIMIT_SPACES = "Description Length should be between 3 and 5000 characters without leading/trailing spaces.";
		public static final String EXPLANATION_LIMIT = "Explanation Length should be between 3 and 1000 characters";
		public static final String TITLE_LIMIT = "Title should be between 3 and 70 characters";
		public static final String TITLE_LIMIT_SPACES = "Title should be between 3 and 70 characters without leading/trailing spaces.";

		public static final String COMMENT_LIMIT = "Comment should be between 1 and 1000 characters";

		public static final String MESSAGE_LIMIT = "Message must be less than 1000 characters";
		public static final String ACCEPTANCE_CRITERIA = "Acceptance Criteria must not exceed 1000 characters";
		public static final String ESTIMATE_TIME_LOG = "Estimate_Time_Log Length should not exceed 20 chars";
		public static final String KEY_DECISIONS = "Key Decisions must not exceed 1000 characters";
		public static final String REASON_CRITERIA = "Immediate Attention Reason should be a minimum of 2 characters and less than 250 character";
		public static final String DELETION_REASON = "Deletion Reason should be minimum of 2 characters and less than or equal to 250 character";

		public static final String STEPS_TAKEN_TO_COMPLETE = "Steps Taken To Complete must not exceed 1000 characters";
		public static final String PARKING_LOT = "Parking Lot must not exceed 1000 characters";
		public static final String TASK_PRIORITY = "Task_priority Length should between 2 and 255 characters";
		public static final String NEW_EFFORT = "New effort cannot be null or zero";
		public static final String NEW_EFFORT_DATE = "New effort date cannot be null";

		public static final String WORKFLOW_STATUS = "Workflow Status is a mandatory field";
		public static final String PRIORITY = "Task Priority is a required field";
		public static final String TASK_ID = "Task id is missing";
	}

	public static final class Epic {
		public static final String EPIC_ID = "Epic id is missing";
		public static final String EPIC_TITLE = "Epic title is missing";
		public static final String EPIC_DESC = "Epic description is missing";
		public static final String ENTITY_TYPE_ID = "Entity type is missing";

		public static final String TASK_LIST = "Task list is empty";
		public static final String TEAM_LIST = "Team list is missing";
		public static final String PROJECT_ID = "Project id is missing";
		public static final String ORG_ID = "Organization id is missing";
		public static final String WORK_FLOW_EPIC_STATUS_ID = "Workflow epic status id is missing";
		public static final String  EPIC_LIMIT = "Epic should be between 3 and 70 characters";
		public static final String DESC_LIMIT = "Description length should be between 3 and 5000 characters";
		public static final String VALUE_LIMIT = "Value Area length should be less than 5000 characters";
		public static final String FUNCTIONAL_LIMIT = "Functional Area length should be less than 5000 characters";
		public static final String QUARTERLY_LIMIT = "Quarterly Area length should be less than 5000 characters";
		public static final String YEARLY_LIMIT = "Yearly Area length should be less than 5000 characters";
	}

	public static final class Preferences {

		public static final String USER_ID = "user Id must not be null";
	}

	public static final class FirebaseTokenDTO {
		public static final String TOKEN = "token is missing.";
		public static final String DEVICE_TYPE = "device type is missing.";
		public static final String DEVICE_ID = "device id is missing.";
		public static final String TIMESTAMP = "token timestamp is missing.";
	}
    public static final class Meeting{
		public static final String MEETING_ID = "Meeting id can't be null";
		public static final String MEETING_KEY="Meeting Key cannot be null, for online meeting, provide a Link else provide address to the meeting.";
        public static final String AGENDA="Agenda of the meeting is missing." ;
        public static final String ATTENDEE_LIST=" Attendee List Cannot be null, invite someone.";
		public static final String MEETING_NUMBER="Meeting Number cannot be null";
		public static final String ORGANISATION_ID=" OrgId cannot be null for a meeting";
		public static final String ATTENDEE_LIST_EMPTY=" Attendees list  cannot be empty for a meeting";
		public static final String REFERENCED_MEETING_LIMIT = "Referenced meeting reason should be between 3 and 255 characters";
		public static final String MOM_LIMIT = "Minutes of Meeting length should be less than 5000 characters";
		public static final String AGENDA_LIMIT="Agenda of Meeting length should be less than 252";
		public static final String MEETING_KEY_LIMIT="Meeting Key length should must be between 3 to 252";
		public static final String RECUR_DAYS_LIMIT = "Frequency of meeting can't be exceeded 30 days ";
		public static final String MODEL_ID = "Model id can't be null";
		public static final String IS_PROCESSING = "Is processing can't be null";
		public static final String UPLOADER_ACCOUNT_ID = "Uploader account id can't be null";
		public static final String UPLOADED_DATE_TIME = "Uploaded date time can't be null";
		public static final String MEETING_FILE_METADATA_LIST = "Meeting file metadata list can't be null";
		public static final String MINUTES_OF_MEETING = "Summary can't be more than 5000 character";
		public static final String MEETING_NOTE_LENGTH = "Meeting note length should be between 3 to 1000";
	}

	public static final class Leave{
		public static final String ACCOUNT_ID_ERROR ="Account ids can not be null or empty";
		public static final String ORG_ID_ERROR ="Organization is a mandatory field";
		public static final String INITIAL_LEAVE_ERROR ="Initial leave can not be null or empty";
		public static final String IS_LEAVE_CARRY_FORWARD_ERROR ="Is leave carry forward can not be null or empty";
        public static final String LEAVE_TYPE_ID_ERROR = "Leave type can not be null or empty.";
		public static final String LEAVE_POLICY_ID = "Leave policy id is a mandatory field";
		public static final String APPLICATION_ID = "Please provide application information";
		public static final String LEAVE_REASON_LENGTH = "Leave reason length should be between 3 to 1000";
		public static final String ADDRESS_LENGTH = "Address length should be less than 1000";
		public static final String APPROVAL_REASON = "Approval or Cancellation reason should be between 3 to 1000";

		public static final String CANCELLATION_REASON_LENGTH = "Lenght of cancellation reason should be between 3 to 70 characters";
		public static final String CANCELLATION_REASON = "Cancellation reason is mandatory to cancel a leave application.";
		public static final String PHONE_NUMBER_LENGTH = "Invalid phone number";
		public static final String LEAVE_ALIAS_LIMIT="Leave Alias length should be between 3 to 30";
	}
	public static final class AttentionRequest{
		public static final String USERNAME_ERROR ="userName can not be null or empty";
		public static final String TEAMLIST_ERROR = "teamList can not be null or empty.";
	}

	public static final class Team {
		public static final String TEAM_DESC = "Team Description should be between 3 and 1000 characters";
		public static final String TEAM_NAME = "Team Name should be between 3 and 50 characters";

		public static final String TEAM_DESC_NOT_NULL = "Team Description is mandatory";
		public static final String TEAM_NAME_NOT_NULL = "Team Name is mandatory";

		public static final String PROJECT = "Please provide project information";

		public static final String ORG = "Please provide organization information";

		public static final String OWNER_ACCOUNT = "Please provide owner information";
		public static final String TEAM_ADMIN = "Please provide team admin information to create team";
		public static final String TEAM_CODE = "Team code can not be null";
	}

	public static final class Sprint {
		public static final String SPRINT_TITLE = "Sprint title is a mandatory field.";
		public static final String TITLE_LIMIT = "Sprint title should be between 3 to 70 characters";
		public static final String SPRINT_OBJECTIVE = "Sprint Objective is a mandatory field.";
		public static final String OBJECTIVE_LIMIT = "Sprint objective should be between 3 to 1000 characters";
		public static final String START_DATE = "Sprint expected start date is a mandatory field.";
		public static final String END_DATE = "Sprint expected end date is a mandatory field.";
		public static final String ENTITY_TYPE_ID = "Entity Type Id is a mandatory field.";
		public static final String ENTITY_ID = "Entity ID is a mandatory field.";
		public static final String CREATOR = "Creator is a mandatory field";
		public static final String SPRINT = "Sprint id is a mandatory field";
		public static final String TASK_LIST = "Please provide task list to add tasks";
		public static final String ORG = "Please provide organization information";
	}
	public static final class Project {
		public static final String PROJECT_NAME = "Project Name is a mandatory field.";
		public static final String PROJECT_NAME_LENGTH = "The character length of project name should be between 3 and 50 characters.";
		public static final String PROJECT_DESC_LENGTH = "The character length of project description should be between 3 and 255 characters";
		public static final String ORG_ID = "Org Id is a required field.";
		public static final String BU_ID = "Bu Id is a required field.";

		public static final String PROJECT_ID = "Project Id is a required field.";

	}

	public static final class BugTask {
		public static final String SEVERITY = "Severity is a mandatory field";
		public static final String ENVIRONMENT = "Environment is a mandatory field";

	}

	public static final class RecurTask {

		public static final String START_DATE = "Start date is a required field";
		public static final String END_DATE = "End date is a required field";
		public static final String EXP_START_TIME = "Expected start time is a required field";
		public static final String EXP_END_TIME = "Expected end time is a required field";
		public static final String TEAM_ID = "Team is a required field";
		public static final String WORKFLOW_STATUS = "Workflow status is a required field";
		public static final String WORKFLOW_ID = "Workflow type is a required field";
		public static final String RECUR_TYPE = "Recurring frequency is a required field";
		public static final String OCCURRENCE_DURATION = "Occurrence duration is a required field";
		public static final String SELECTED_DATE = "Date selection is mandatory";

		public static final String CUSTOM_RECURRENCE_TYPE = "Custom recurrence type must be provided";
		public static final String ORDINAL = "Ordinal must be provided";
		public static final String DAY_TYPE = "Day type must be provided";
		public static final String OCCURENCE_LIMIT = "Number of Occurrence cannot exceed 100";
		public static final String RECURRENCE_SCHEDULE = "Recurrence schedule details are required";
	}

	public static final class Alert {
		public static final String ALERT_TITLE = "Alert title is a mandatory field";
		public static final String ALERT_REASON = "Alert reason is a mandatory field";
		public static final String ALERT_REASON_LENGTH = "Alert reason Length should between 3 and 255 characters";
		public static final String SENDER_ACCOUNT = "Sender account information is required";
		public static final String RECEIVER_ACCOUNT = "Receiver account information is required";
		public static final String TEAM_ID = "Team information is required";
		public static final String ORG_ID = "Organization information is required";
		public static final String PROJECT_ID = "Project information is required";
		public static final String ALERT_TYPE = "Alert type is a mandatory field";
	}

	public static final class Reminder {
		public static final String TITLE = "Reminder title is a mandatory field";
		public static final String DESCRIPTION = "Reminder description is a mandatory field";
		public static final String REMINDER_DATE = "Reminder date is a mandatory field";
		public static final String REMINDER_TIME = "Reminder time is a mandatory field";
		public static final String ACCOUNT_ID = "Creator information is required";
	}

	public static final class PerfNotes {
		public static final String PERF_NOTE = "Performance note is a mandatory field";
		public static final String TASK_ID = "Task info is mandatory";
		public static final String POSTED_BY = "Posted by info is mandatory";
		public static final String ASSIGNED_TO = "Assigned to info is mandatory";
		public static final String PERF_NOTE_SIZE = "Performance note should be between 3 to 1000 characters";

	}

	public static final class Jira {
		public static final String TASK_HANDE_STRATEGY = "Task handle strategy is mandatory";
		public static final String DEFAULT_ASSIGN_TO = "Default assign to is mandatory";
		public static final String DEFAULT_TASK_COMPLETED_PERCENTAGE = "Default task completed percentage is mandatory";
		public static final String DEFAULT_ESTIMATE = "Default estimate is mandatory";
		public static final String SITE_URL_REQUIRED = "Jira site URL is required.";
		public static final String EMAIL_REQUIRED = "Jira email is required.";
		public static final String TOKEN_REQUIRED = "Jira token is required.";
		public static final String PROJECT_ID_REQUIRED = "Project ID must not be blank.";
		public static final String JIRA_USER_LIST = "Jira user list cannot be empty";
	}

	public static class ReleaseVersion {
		public static final String RELEASE_VERSION_NAME = "Release version name must not be blank.";
		public static final String RELEASE_VERSION_NAME_SIZE = "Release version name must be between 1 to 100 character.";
		public static final String INVALID_VERSION_PATTERN = "Release version name is not a valid semantic version.";
	}

	public static class ActionItem{
		public static final String ACTION_ITEM="Action Item length should be less than 500 characters";
  }

	public static class Github {
		public static final String GITHUB_ACCOUNT_USER_NAME = "GitHub account username is required";
		public static final String GITHUB_ACCOUNT_USER_NAME_LENGTH = "GitHub username must be between 1 and 39 characters";
		public static final String ENTITY_TYPE_ID = "Entity type ID is required";
		public static final String ENTITY_ID = "Entity ID is required";
		public static final String IS_ACTIVE = "isActive flag is required";
		public static final String GITHUB_ACCOUNT_PREFERENCE_ID = "GitHub account preference ID is required";
		public static final String GITHUB_REPOSITORY_NAME = "GitHub repository name is required";
		public static final String GITHUB_REPOSITORY_NAME_LENGTH = "GitHub repository name must be between 1 and 100 characters";
		public static final String GITHUB_REPOSITORY_PREFERENCE_ID = "GitHub repository preference ID is required";

		public static final String GITHUB_USERNAME_REQUIRED = "GitHub account username is required";
		public static final String GITHUB_USERNAME_INVALID = "Invalid GitHub username format";
		public static final String GITHUB_USERNAME_SIZE = "GitHub username must be between 1 and 39 characters";

		public static final String GITHUB_REPO_REQUIRED = "GitHub repository name is required";
		public static final String GITHUB_REPO_INVALID = "Invalid GitHub repository name format";
		public static final String GITHUB_REPO_SIZE = "GitHub repository name must be between 1 and 100 characters";

		public static final String ORG_ID_REQUIRED = "Organization ID is required";

		public static final String DUPLICATE_ACTIVE_ENTRY = "GitHub account and repository already exist in org preference";

		public static final String PREF_ID_REQUIRED = "GitHub Account and Repo Preference ID is required";
		public static final String INSTALLATION_ID_REQUIRED = "Installation ID must not be blank.";
		public static final String GITHUB_CODE_REQUIRED = "GitHub authorization code is required.";
	}

	public static final class CustomEnviornment {
		public static final String ENTITY_ID = "Entity ID is required";
		public static final String ENTITY_TYPE_ID = "Entity type ID is required";
		public static final String CUSTOM_ENVIORNMENT_ID="Enviornment Id is required";
		public static final String IS_ACTIVE="Is Active is required";
		public static final String ENVIORNMENT_DISPLAY_NAME="Enviornment Display Name is required";
		public static final String ENVIORNMENT_DESCRIPTION="Enviornment Discription is required";
		public static final String ENVIORNMENT_DISPLAY_SIZE="Enviornment Display Name length must be between 2 to 70";
		public static final String ENVIORNMENT_DESCRIPTION_SIZE="Enviornment Description length must be between 2 to 255";


	}
	public static final class FeatureAccess{
		public static final String ENTITY_ID = "Entity ID is required";
		public static final String ENTITY_TYPE_ID = "Entity type ID is required";
		public static final String FEATURE_ACCESS_ID = "Feature Access ID is required";
		public static final String ACTIONS = "Actions is required";
		public static final String ORG_ID = "Organization ID is required";
		public static final String USER_ACCOUNT_ID = "User Account ID is required";
	}

	public static final class File {
		public static final String FILE_NAME = "File name can't be empty";
		public static final String FILE_EXTENSION = "File extension can't be empty";
		public static final String FILE_SIZE = "File size can't be empty";
	}

	public static final class TaskUpdationDetails{
		public static final String ENTITY_ID = "Entity ID is required";
		public static final String ENTITY_TYPE_ID = "Entity type ID is required";
		public static final String NOTIFICATION_TYPE_ID = "Notification Type required";
		public static final String ACCOUNT_ID = "Account Id is required";
	}

	public static final class EntityPreference{
		public static final String BREAK_DURATION = "Break Duration cannot exceed 1440 minutes";
		public static final String TASK_DURATION = "Work Item Effort Edit Duration cannot exceed 1440 minutes";
		public static final String MEETING_EFFORT_DURATION = "Meeting Effort edit Duration cannot exceed 1440 minutes";
	}
}
