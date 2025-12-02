# Complete API Catalog

## Table of Contents
1. [Overview](#overview)
2. [TSe_Server APIs (Port 8080)](#tse_server-apis-port-8080)
3. [TSEHR APIs (Port 8081)](#tsehr-apis-port-8081)
4. [Chat-App APIs (Port 8082)](#chat-app-apis-port-8082)
5. [Scheduled Task APIs](#scheduled-task-apis)
6. [Common Headers](#common-headers)
7. [Authentication](#authentication)
8. [Error Responses](#error-responses)

---

## Overview

| Module | Base URL | Port | Context Path |
|--------|----------|------|--------------|
| TSe_Server | `http://localhost:8080/api` | 8080 | `/api` |
| TSEHR | `http://localhost:8081` | 8081 | - |
| Chat-App | `http://localhost:8082` | 8082 | - |

**Total Endpoints:** 200+ across all modules

---

## TSe_Server APIs (Port 8080)

### Authentication APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login with email/password |
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/verifyOtp` | Verify OTP for registration |
| POST | `/api/auth/forgotPassword` | Request password reset |
| POST | `/api/auth/resetPassword` | Reset password with token |
| POST | `/api/auth/refreshToken` | Refresh JWT token |
| POST | `/api/auth/validateTokenAccount` | Validate token and account IDs |
| POST | `/api/auth/google` | Google OAuth login |
| POST | `/api/auth/logout` | Logout user |

### User Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/user/profile` | Get current user profile |
| PUT | `/api/user/profile` | Update user profile |
| GET | `/api/user/{userId}` | Get user by ID |
| GET | `/api/user/accounts` | Get user's accounts |
| POST | `/api/user/switchAccount` | Switch active account |
| GET | `/api/user/search` | Search users |
| PUT | `/api/user/preferences` | Update user preferences |
| POST | `/api/user/uploadProfilePic` | Upload profile picture |

### Organization APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/org/create` | Create organization |
| GET | `/api/org/{orgId}` | Get organization details |
| PUT | `/api/org/{orgId}` | Update organization |
| DELETE | `/api/org/{orgId}` | Delete organization |
| GET | `/api/org/{orgId}/members` | Get organization members |
| POST | `/api/org/{orgId}/invite` | Invite user to organization |
| GET | `/api/org/{orgId}/bu` | Get business units |
| POST | `/api/org/{orgId}/bu` | Create business unit |

### Business Unit APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/bu/{buId}` | Get BU details |
| PUT | `/api/bu/{buId}` | Update BU |
| DELETE | `/api/bu/{buId}` | Delete BU |
| GET | `/api/bu/{buId}/projects` | Get BU projects |
| POST | `/api/bu/{buId}/project` | Create project in BU |

### Project APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/project/{projectId}` | Get project details |
| PUT | `/api/project/{projectId}` | Update project |
| DELETE | `/api/project/{projectId}` | Delete project |
| GET | `/api/project/{projectId}/teams` | Get project teams |
| POST | `/api/project/{projectId}/team` | Create team in project |

### Team APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/team/{teamId}` | Get team details |
| PUT | `/api/team/{teamId}` | Update team |
| DELETE | `/api/team/{teamId}` | Delete team (soft) |
| GET | `/api/team/{teamId}/members` | Get team members |
| POST | `/api/team/{teamId}/member` | Add team member |
| DELETE | `/api/team/{teamId}/member/{accountId}` | Remove team member |
| PUT | `/api/team/{teamId}/member/{accountId}/role` | Update member role |

### Task APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/task/createTask` | Create new task |
| GET | `/api/task/{taskId}` | Get task details |
| PUT | `/api/task/{taskId}` | Update task |
| DELETE | `/api/task/{taskId}` | Delete task (soft) |
| GET | `/api/task/team/{teamId}` | Get tasks by team |
| GET | `/api/task/sprint/{sprintId}` | Get tasks by sprint |
| GET | `/api/task/assignee/{accountId}` | Get tasks by assignee |
| POST | `/api/task/{taskId}/assign` | Assign task |
| PUT | `/api/task/{taskId}/status` | Update task status |
| PUT | `/api/task/{taskId}/estimate` | Update task estimate |
| POST | `/api/task/{taskId}/comment` | Add comment |
| GET | `/api/task/{taskId}/comments` | Get task comments |
| POST | `/api/task/{taskId}/attachment` | Add attachment |
| GET | `/api/task/{taskId}/attachments` | Get task attachments |
| DELETE | `/api/task/{taskId}/attachment/{attachmentId}` | Delete attachment |
| POST | `/api/task/{taskId}/dependency` | Add task dependency |
| GET | `/api/task/{taskId}/dependencies` | Get task dependencies |
| GET | `/api/task/{taskId}/history` | Get task history |
| POST | `/api/task/search` | Search tasks |
| GET | `/api/task/backlog/{teamId}` | Get team backlog |

### Sprint APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/sprint/create` | Create sprint |
| GET | `/api/sprint/{sprintId}` | Get sprint details |
| PUT | `/api/sprint/{sprintId}` | Update sprint |
| DELETE | `/api/sprint/{sprintId}` | Delete sprint |
| POST | `/api/sprint/{sprintId}/start` | Start sprint |
| POST | `/api/sprint/{sprintId}/complete` | Complete sprint |
| GET | `/api/sprint/team/{teamId}` | Get team sprints |
| GET | `/api/sprint/{sprintId}/burndown` | Get burndown data |
| GET | `/api/sprint/{sprintId}/velocity` | Get velocity metrics |
| POST | `/api/sprint/{sprintId}/addTask` | Add task to sprint |
| DELETE | `/api/sprint/{sprintId}/removeTask/{taskId}` | Remove task from sprint |

### Meeting APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/meeting/create` | Create meeting |
| GET | `/api/meeting/{meetingId}` | Get meeting details |
| PUT | `/api/meeting/{meetingId}` | Update meeting |
| DELETE | `/api/meeting/{meetingId}` | Cancel meeting |
| GET | `/api/meeting/team/{teamId}` | Get team meetings |
| GET | `/api/meeting/user/{accountId}` | Get user meetings |
| POST | `/api/meeting/{meetingId}/rsvp` | RSVP to meeting |
| POST | `/api/meeting/{meetingId}/start` | Start meeting |
| POST | `/api/meeting/{meetingId}/end` | End meeting |
| PUT | `/api/meeting/{meetingId}/mom` | Update minutes of meeting |
| POST | `/api/meeting/{meetingId}/actionItem` | Add action item |
| GET | `/api/meeting/{meetingId}/jitsiToken` | Get Jitsi JWT token |

### Epic APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/epic/create` | Create epic |
| GET | `/api/epic/{epicId}` | Get epic details |
| PUT | `/api/epic/{epicId}` | Update epic |
| DELETE | `/api/epic/{epicId}` | Delete epic |
| GET | `/api/epic/team/{teamId}` | Get team epics |
| POST | `/api/epic/{epicId}/addTask` | Add task to epic |

### Workflow APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/workflow` | Get all workflows |
| GET | `/api/workflow/{workflowId}` | Get workflow details |
| GET | `/api/workflow/{workflowId}/statuses` | Get workflow statuses |
| POST | `/api/workflow/create` | Create custom workflow |
| PUT | `/api/workflow/{workflowId}` | Update workflow |

### Geo-Fencing APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/geo-fence/create` | Create geo-fence |
| GET | `/api/geo-fence/{fenceId}` | Get fence details |
| PUT | `/api/geo-fence/{fenceId}` | Update fence |
| DELETE | `/api/geo-fence/{fenceId}` | Delete fence |
| GET | `/api/geo-fence/org/{orgId}` | Get org fences |
| POST | `/api/geo-fence/assign` | Assign users to fence |
| POST | `/api/geo-fence/validate` | Validate location against fence |

### Attendance APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/attendance/punchIn` | Punch in |
| POST | `/api/attendance/punchOut` | Punch out |
| GET | `/api/attendance/today` | Get today's attendance |
| GET | `/api/attendance/history` | Get attendance history |
| POST | `/api/attendance/correction` | Request attendance correction |
| GET | `/api/attendance/summary` | Get attendance summary |

### Notification APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notification` | Get notifications |
| PUT | `/api/notification/{notificationId}/read` | Mark as read |
| PUT | `/api/notification/readAll` | Mark all as read |
| DELETE | `/api/notification/{notificationId}` | Delete notification |
| GET | `/api/notification/preferences` | Get notification preferences |
| PUT | `/api/notification/preferences` | Update preferences |
| POST | `/api/notification/fcmToken` | Register FCM token |

### GitHub Integration APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/github/link` | Link GitHub account |
| DELETE | `/api/github/unlink` | Unlink GitHub account |
| GET | `/api/github/repos` | Get linked repositories |
| POST | `/api/github/repo/link` | Link repository |
| GET | `/api/github/branches/{taskId}` | Get task branches |
| POST | `/api/github/branch/create` | Create branch for task |

### File APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/file/upload` | Upload file |
| GET | `/api/file/{fileId}` | Download file |
| DELETE | `/api/file/{fileId}` | Delete file |
| GET | `/api/file/task/{taskId}` | Get task files |

### Report APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/report/team/{teamId}/summary` | Team summary report |
| GET | `/api/report/sprint/{sprintId}` | Sprint report |
| GET | `/api/report/user/{accountId}/effort` | User effort report |
| POST | `/api/report/export` | Export report to Excel |

---

## TSEHR APIs (Port 8081)

### Leave Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/leave/apply` | Apply for leave |
| GET | `/leave/{applicationId}` | Get leave application |
| PUT | `/leave/{applicationId}` | Update leave application |
| DELETE | `/leave/{applicationId}` | Cancel leave application |
| GET | `/leave/my` | Get my leave applications |
| GET | `/leave/pending` | Get pending approvals (manager) |
| POST | `/leave/{applicationId}/approve` | Approve leave |
| POST | `/leave/{applicationId}/reject` | Reject leave |
| GET | `/leave/balance` | Get leave balance |
| GET | `/leave/history` | Get leave history |
| GET | `/leave/calendar` | Get team leave calendar |

### Leave Policy APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/leave/policy/{orgId}` | Get org leave policy |
| PUT | `/leave/policy/{orgId}` | Update leave policy |
| POST | `/leave/policy/{orgId}/reset` | Reset annual leave balances |

### Timesheet APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/timesheet/log` | Log effort |
| GET | `/timesheet/task/{taskId}` | Get task timesheet |
| GET | `/timesheet/user/{accountId}` | Get user timesheet |
| GET | `/timesheet/date/{date}` | Get timesheet by date |
| PUT | `/timesheet/{entryId}` | Update timesheet entry |
| DELETE | `/timesheet/{entryId}` | Delete timesheet entry |
| GET | `/timesheet/summary` | Get timesheet summary |

---

## Chat-App APIs (Port 8082)

### Chat Group APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/group/create` | Create chat group |
| GET | `/group/{groupId}` | Get group details |
| PUT | `/group/{groupId}` | Update group |
| DELETE | `/group/{groupId}` | Delete group |
| GET | `/group/my` | Get my groups |
| POST | `/group/{groupId}/member` | Add member to group |
| DELETE | `/group/{groupId}/member/{userId}` | Remove member |
| PUT | `/group/{groupId}/member/{userId}/admin` | Toggle admin |
| POST | `/group/{groupId}/leave` | Leave group |
| GET | `/group/{groupId}/members` | Get group members |

### Message APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/message/send` | Send message |
| GET | `/message/group/{groupId}` | Get group messages |
| PUT | `/message/{messageId}` | Edit message |
| DELETE | `/message/{messageId}` | Delete message |
| POST | `/message/{messageId}/react` | React to message |
| GET | `/message/search` | Search messages |
| POST | `/message/{messageId}/forward` | Forward message |

### Direct Message APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/dm/send` | Send direct message |
| GET | `/dm/{userId}` | Get DM conversation |
| GET | `/dm/list` | Get DM conversations |

### File Attachment APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/attachment/upload` | Upload attachment |
| GET | `/attachment/{attachmentId}` | Download attachment |
| DELETE | `/attachment/{attachmentId}` | Delete attachment |

### Presence APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/presence/status` | Update presence status |
| GET | `/presence/user/{userId}` | Get user presence |
| GET | `/presence/group/{groupId}` | Get group members presence |

### WebSocket Endpoints

| Endpoint | Description |
|----------|-------------|
| `/ws` | WebSocket connection endpoint |
| `/topic/group/{groupId}` | Subscribe to group messages |
| `/topic/dm/{conversationId}` | Subscribe to DM messages |
| `/topic/presence` | Subscribe to presence updates |
| `/app/message` | Send message via WebSocket |
| `/app/ack` | Send message acknowledgment |

---

## Scheduled Task APIs

### Internal Scheduler Endpoints (TSe_Server)

These are called by `Notification_Reminders` module:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/schedule/meeting/reminder` | Trigger meeting reminders |
| GET | `/api/schedule/meeting/startMeetingConfirmation` | Meeting start confirmation |
| GET | `/api/schedule/leave/leaveRemainingReset` | Annual leave reset |
| GET | `/api/schedule/leave/leaveRemainingMonthlyUpdate` | Monthly pro-rata update |
| GET | `/api/schedule/timesheet/timesheetPreReminder` | Timesheet pre-reminder |
| GET | `/api/schedule/timesheet/timesheetPostReminder` | Timesheet post-reminder |
| GET | `/api/schedule/alert/dependencyAlert` | Task dependency alerts |
| GET | `/api/schedule/alert/deleteAlerts` | Clean old alerts |
| GET | `/api/schedule/sprint/sendSprintTasksMail` | Sprint summary email |
| GET | `/api/schedule/geo-fencing/notifyBeforeShiftStart` | Shift start reminder |
| GET | `/api/schedule/geo-fencing/autoCheckout` | Auto punch-out |
| GET | `/api/schedule/ai/retryFailedUserRegistration` | Retry AI registration |

---

## Common Headers

### Required Headers

| Header | Description | Example |
|--------|-------------|---------|
| `Authorization` | JWT Bearer token | `Bearer eyJhbGci...` |
| `accountIds` | Active account ID(s) | `1` or `1,2,3` |
| `timezone` | User's timezone | `Asia/Kolkata` |
| `screenName` | Current screen identifier | `taskDetail` |
| `Content-Type` | Request content type | `application/json` |

### Optional Headers

| Header | Description | Example |
|--------|-------------|---------|
| `X-Request-ID` | Request tracking ID | `uuid` |
| `Accept-Language` | Preferred language | `en-US` |

---

## Authentication

### JWT Token Structure

```json
{
  "sub": "user@example.com",
  "accountIds": [1, 2, 3],
  "chatPassword": "encrypted_password",
  "iat": 1701456000,
  "exp": 1704048000
}
```

### Token Flow

```
1. POST /api/auth/login
   Request: { "email": "...", "password": "..." }
   Response: { "token": "eyJ...", "accountIds": [1,2] }

2. Include token in subsequent requests
   Header: Authorization: Bearer eyJ...

3. For multi-account users, specify active account
   Header: accountIds: 1

4. Token expires after 30 days
   Use /api/auth/refreshToken to get new token
```

---

## Error Responses

### Standard Error Format

```json
{
  "status": 400,
  "message": "Validation failed: Title is required",
  "timestamp": "2024-12-02 14:30:45:123 +0000"
}
```

### Error with Data

```json
{
  "status": 403,
  "message": "Meeting reference limit exceeded",
  "timestamp": "2024-12-02 14:30:45:123 +0000",
  "data": {
    "isNotificationOnCooldown": true
  }
}
```

### Common HTTP Status Codes

| Code | Meaning | When Used |
|------|---------|-----------|
| 200 | OK | Successful GET/PUT |
| 201 | Created | Successful POST |
| 400 | Bad Request | Invalid input |
| 401 | Unauthorized | Invalid/missing token |
| 403 | Forbidden | Access denied |
| 404 | Not Found | Resource not found |
| 406 | Not Acceptable | Validation failed |
| 409 | Conflict | Duplicate resource |
| 422 | Unprocessable Entity | Business rule violation |
| 500 | Internal Server Error | Unexpected error |

---

## API Documentation

### Swagger UI

Access interactive API documentation:
- **TSe_Server:** `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI Spec:** `http://localhost:8080/api/v3/api-docs`

### Example Requests

#### Create Task

```bash
curl -X POST 'http://localhost:8080/api/task/createTask' \
  -H 'Authorization: Bearer eyJ...' \
  -H 'accountIds: 1' \
  -H 'timezone: Asia/Kolkata' \
  -H 'screenName: createTask' \
  -H 'Content-Type: application/json' \
  -d '{
    "taskTitle": "Implement login feature",
    "taskDescription": "Add JWT-based authentication",
    "teamId": 5,
    "assigneeAccountId": 10,
    "taskTypeId": 1,
    "taskWorkflowId": 1,
    "taskEstimate": 8.0,
    "priority": 2
  }'
```

#### Apply for Leave

```bash
curl -X POST 'http://localhost:8081/leave/apply' \
  -H 'Authorization: Bearer eyJ...' \
  -H 'accountIds: 1' \
  -H 'timezone: Asia/Kolkata' \
  -H 'Content-Type: application/json' \
  -d '{
    "leaveTypeId": 1,
    "fromDate": "2024-12-20",
    "toDate": "2024-12-22",
    "reason": "Family vacation",
    "approverAccountId": 5
  }'
```

#### Send Chat Message

```bash
curl -X POST 'http://localhost:8082/message/send' \
  -H 'Authorization: Bearer eyJ...' \
  -H 'accountIds: 1' \
  -H 'Content-Type: application/json' \
  -d '{
    "groupId": 10,
    "content": "Hello team!",
    "messageType": "TEXT"
  }'
```
