package com.tse.core_application.constants;

public class ControllerConstants {

	public static class CommentsWithPage {
		public static final int pageSize = 15;

	}
	
	public static class TaskNumber {
		public static final Long taskNum = Long.valueOf(1000);
	}

	public static final Long TASK_TEMPLATE_NUMBER_START = 1000L;

	public static class MeetingNumber {
		public  static final Long meetingNum = Long.valueOf(1000);
	}

	public static class TseHr{

		public static final String getTimeSheetUrl = "/getts";

		public static final String rootPathLeave = "/leave";

		public static final String defaultLeavePolicyAssignmentUrl = "/defaultLeavePolicyAssignment";
		public static final String addLeavePolicyUrl = "/addLeavePolicy";
		public static final String getUserLeavePolicy = "/getUserLeavePolicy";
		public static final String getOrgLeavePolicy = "/getOrgLeavePolicy";
		public static final String updateLeavePolicyUrl = "/updateLeavePolicy";
		public static final String assignLeavePolicyToUserUrl = "/assignLeavePolicyToUser";
		public static final String reassignLeavePolicyToUserUrl = "/reassignLeavePolicyToUser";
		public static final String createLeaveUrl = "/createLeave";

		public static final String updateLeaveUrl = "/updateLeave";

		public static final String getLeaveHistoryUrl = "/getLeaveHistory";

		public static final String getLeaveApplicationUrl = "/getLeaveApplication";

		public static final String getDoctorCertificateUrl = "/getDoctorCertificate";

		public static final String applicationStatusUrl = "/applicationStatus";

		public static final String  getLeavesByFilter = "/getLeavesByFilter";

		public static final String changeLeaveStatusUrl = "/changeLeaveStatus";
		public static final String cancelLeaveApplicationUrl = "/cancelLeaveApplication";

		public static final String  getTeamLeaveHistoryUrl = "/getTeamLeaveHistory";

		public static final String  getLeavesRemainingUrl = "/getLeavesRemaining";

		public static final String  getApprovedLeavesUrl = "/getApprovedLeaves";

		public static final String getTeamMembersOnLeaveUrl = "/getTeamMembersOnLeave";

		public static final String assignLeavePolicyToAllUser = "/assignLeavePolicyToAllUser";

		public static final String getPeopleOnLeave = "/getPeopleOnLeave";

		public static final String getEntityLeaveReport = "/getEntityLeaveReport";

		public static final String getAllUsersPolicyReport = "/getAllUsersPolicyReport";

		public static final String updateUserPolicy = "/updateUserPolicy";

		public static final String getUpcomingLeavesCount = "/getUpcomingLeavesCount";

		public static final String getUserLeaveDetails = "/getUserLeaveDetails";
	}

	public static class Conversations {

		public static final String createUser = "/api/users/create";
		public static final String bulkCreateUser = "/api/users/bulkCreate";

		public static final String createGroup = "/api/groups/create";
		public static final String getAllGroups = "/api/groups/allGroups";

		public static final String addUsersToGroup = "/api/groups/bulkAddUsers";

		public static final String removeUsersFromGroup = "/api/groups/bulkRemoveUsers";
		public static final String removeUsersFromGroupV2 = "/api/groups/v2/bulkRemoveUsers";

		public static final String getUserByAccountId = "/api/users/getUser";

		public static final String getUserGroups = "/api/groups/user/{userId}";

		public static final String deleteUserFromOrg = "/api/users/deleteUser/{accountId}";

		public static final String changeUserName = "/api/users/changeUsername/{accountId}";

		public static final String getGroupByEntityIdAndEntityTypeId = "/api/groups/getGroup/{entityId}/{entityTypeId}";

		public static final String updateGroupDetails = "/api/groups/updateGroup/{entityId}/{entityTypeId}";

		public static final String activateDeactivateUser = "/api/users/v2/deleteUser";
		public static final String getAllUsers = "/api/users/all";
	}

	public static class JitsiApi{
		public static final String activeMeetings = "/api/jitsi/active-meetings";
	}

	public static class AiMLApi {

		public static final String registerUser = "/api/v1/registration/register";
		public static final String inactiveUser = "/api/v1/removal/remove";
		public static final String tokenEnquiry = "/api/v1/tokens/info";
		public static final String isWorkItemDuplicate = "/duplicate_task";
	}
}
