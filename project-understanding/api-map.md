# API Documentation & Endpoint Map

## Table of Contents
1. [API Overview](#api-overview)
2. [Authentication Endpoints](#authentication-endpoints)
3. [Task Management APIs](#task-management-apis)
4. [Meeting APIs](#meeting-apis)
5. [Leave APIs](#leave-apis)
6. [Sprint APIs](#sprint-apis)
7. [User & Organization APIs](#user--organization-apis)
8. [Notification APIs](#notification-apis)
9. [Geo-Fencing APIs](#geo-fencing-apis)
10. [Chat APIs (WebSocket & REST)](#chat-apis)
11. [Shared DTOs](#shared-dtos)
12. [Error Handling](#error-handling)

---

## API Overview

### Base URLs

| Module | Base URL | Port |
|--------|----------|------|
| TSe_Server | `http://localhost:8080/api` | 8080 |
| TSEHR | `http://localhost:8081` | 8081 |
| Chat-App | `http://localhost:8082/api` | 8082 |
| Notification_Reminders | Internal Only | 8081 |

### Required Headers

| Header | Required | Format | Description |
|--------|----------|--------|-------------|
| `Authorization` | Yes | `Bearer <JWT>` | JWT token |
| `accountIds` | Yes | `1,2,3` | Comma-separated account IDs |
| `timeZone` | Yes | `Asia/Kolkata` | Valid timezone |
| `screenName` | Yes | `dashboard` | Alphanumeric identifier |

### Response Format

**Success Response:**
```json
{
    "status": 200,
    "message": "Success",
    "data": { ... },
    "timestamp": "2025-12-02T10:30:45.123+0000"
}
```

**Error Response:**
```json
{
    "status": 400,
    "message": "Validation failed: field 'title' is required",
    "timestamp": "2025-12-02T10:30:45.123+0000"
}
```

---

## Authentication Endpoints

### Base Path: `/auth`

#### Generate OTP
```http
POST /auth/generateotp
Content-Type: application/json

Headers:
  screenName: registration
  timeZone: Asia/Kolkata

Body:
{
    "email": "user@example.com",
    "alternateEmail": "alt@example.com",  // optional
    "isPersonalRegistration": true
}

Response: 200 OK
{
    "message": "OTP sent successfully"
}
```

#### Login with OTP
```http
POST /auth/login
Content-Type: application/json

Headers:
  screenName: login
  timeZone: Asia/Kolkata

Body:
{
    "username": "user@example.com",
    "password": "427586"  // OTP
}

Response: 200 OK
{
    "token": "eyJhbGciOiJSUzUxMiJ9...",
    "user": {
        "userId": 1,
        "primaryEmail": "user@example.com",
        "firstName": "John",
        "lastName": "Doe"
    },
    "accounts": [
        {
            "accountId": 1,
            "orgId": 1,
            "email": "user@example.com",
            "isDefault": true,
            "isActive": true
        }
    ]
}
```

#### Google OAuth2 Login (Mobile)
```http
POST /auth/login
Content-Type: application/json

Headers:
  screenName: login
  timeZone: Asia/Kolkata

Body:
{
    "authToken": "google_id_token_here"
}

Response: 200 OK (same as OTP login)
```

#### Sign Up
```http
POST /auth/signup
Content-Type: application/json

Headers:
  screenName: registration
  timeZone: Asia/Kolkata

Body:
{
    "email": "newuser@example.com",
    "password": "427586",  // OTP
    "firstName": "John",
    "lastName": "Doe",
    "timeZone": "Asia/Kolkata"
}

Response: 200 OK
{
    "token": "eyJhbGciOiJSUzUxMiJ9...",
    "user": { ... },
    "accounts": [ ... ]
}
```

#### Validate Token
```http
GET /auth/validateToken

Headers:
  Authorization: Bearer <token>
  screenName: dashboard

Response: 200 OK
{
    "valid": true,
    "expiresAt": "2025-12-02T10:30:45.123+0000"
}
```

---

## Task Management APIs

### Base Path: `/task`

#### Get Tasks with Filters
```http
POST /task/getTasks
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1,2,3
  timeZone: Asia/Kolkata
  screenName: taskList

Body:
{
    "teamIds": [1, 2],
    "projectIds": [1],
    "assigneeAccountIds": [1],
    "statuses": ["INPROGRESS", "TODO"],
    "priorities": ["P0", "P1"],
    "sprintId": 5,
    "fromDate": "2025-01-01",
    "toDate": "2025-12-31",
    "searchQuery": "bug",
    "page": 0,
    "size": 20,
    "sortBy": "taskExpEndDate",
    "sortDirection": "ASC"
}

Response: 200 OK
{
    "content": [
        {
            "taskId": 123,
            "taskNumber": "TEAM-123",
            "taskTitle": "Fix login bug",
            "taskDesc": "...",
            "taskTypeId": 1,
            "taskPriority": "P0",
            "taskState": "INPROGRESS",
            "currentActivityIndicator": 50,
            "taskExpStartDate": "2025-01-01T09:00:00",
            "taskExpEndDate": "2025-01-05T18:00:00",
            "taskEstimate": 8,
            "recordedEffort": 240,
            "fkAccountId": {
                "accountId": 1,
                "email": "user@example.com"
            },
            "fkTeamId": {
                "teamId": 1,
                "teamName": "Dev Team"
            },
            "sprintId": 5,
            "labels": ["urgent", "frontend"]
        }
    ],
    "totalElements": 50,
    "totalPages": 3,
    "number": 0
}
```

#### Create Task
```http
POST /task/addNewTask
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: createTask

Body:
{
    "taskTitle": "Implement user authentication",
    "taskDesc": "Add JWT-based authentication to the API",
    "taskTypeId": 1,
    "taskPriority": "P1",
    "taskEstimate": 16,
    "taskExpStartDate": "2025-01-10T09:00:00",
    "taskExpEndDate": "2025-01-15T18:00:00",
    "fkTeamId": { "teamId": 1 },
    "fkAccountId": { "accountId": 2 },
    "sprintId": 5,
    "labels": ["backend", "security"]
}

Response: 201 Created
{
    "taskId": 124,
    "taskNumber": "TEAM-124",
    "taskTitle": "Implement user authentication",
    ...
}
```

#### Update Task
```http
POST /task/updateTask
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: taskDetail

Body:
{
    "taskId": 123,
    "taskTitle": "Fix login bug (updated)",
    "currentActivityIndicator": 75,
    "fkWorkflowTaskStatus": { "workFlowTaskStatusId": 2 },
    "recordedEffort": 360
}

Response: 200 OK
{
    "taskId": 123,
    "taskNumber": "TEAM-123",
    ...
}
```

#### Delete Task
```http
DELETE /task/deleteTask/{taskId}

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: taskDetail

Path Parameters:
  taskId: 123

Query Parameters:
  deletionReasonId: 1
  deletedReason: "Duplicate task"

Response: 200 OK
{
    "message": "Task deleted successfully"
}
```

#### Get Task by ID
```http
GET /task/getTaskByTaskId/{taskId}

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: taskDetail

Path Parameters:
  taskId: 123

Response: 200 OK
{
    "taskId": 123,
    "taskNumber": "TEAM-123",
    "taskTitle": "Fix login bug",
    "taskDesc": "Description here...",
    ...
}
```

#### Add Comment
```http
POST /task/addTaskComment
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: taskDetail

Body:
{
    "taskId": 123,
    "commentContent": "Found the root cause, fixing now.",
    "taggedAccountIds": [2, 3]
}

Response: 201 Created
{
    "commentId": 456,
    "commentContent": "Found the root cause, fixing now.",
    "createdDateTime": "2025-01-05T10:30:00",
    "createdBy": { ... }
}
```

#### Get Task History
```http
POST /task/getTaskHistory/{taskId}
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: taskDetail

Path Parameters:
  taskId: 123

Body:
{
    "page": 0,
    "size": 50
}

Response: 200 OK
{
    "content": [
        {
            "historyId": 789,
            "changeType": "UPDATE",
            "changedField": "currentActivityIndicator",
            "oldValue": "50",
            "newValue": "75",
            "changedBy": { ... },
            "changeDateTime": "2025-01-05T10:30:00"
        }
    ]
}
```

#### Upload Attachment
```http
POST /task/uploadTaskAttachment
Content-Type: multipart/form-data

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: taskDetail

Form Data:
  taskId: 123
  file: <binary file>

Response: 201 Created
{
    "attachmentId": 100,
    "fileName": "screenshot.png",
    "fileType": "image/png",
    "fileSize": 102400
}
```

---

## Meeting APIs

### Base Path: `/meeting`

#### Create Meeting
```http
POST /meeting/createMeeting
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: createMeeting

Body:
{
    "title": "Sprint Planning",
    "agenda": "Plan tasks for next sprint",
    "venue": "Conference Room A",
    "startDateTime": "2025-01-10T10:00:00",
    "endDateTime": "2025-01-10T11:00:00",
    "reminderTime": 15,
    "teamId": 1,
    "attendees": [
        { "accountId": 2, "rsvpStatus": 1 },
        { "accountId": 3, "rsvpStatus": 1 }
    ],
    "labels": ["sprint-planning"]
}

Response: 201 Created
{
    "meetingId": 50,
    "meetingNumber": "TEAM-MTG-50",
    "meetingKey": "abc123-def456",
    "title": "Sprint Planning",
    "jitsiUrl": "https://meet.example.com/abc123-def456",
    ...
}
```

#### Get Meetings
```http
POST /meeting/getMeetings
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: calendar

Body:
{
    "teamIds": [1, 2],
    "fromDate": "2025-01-01",
    "toDate": "2025-01-31",
    "organizerAccountIds": [1],
    "statuses": ["MEETING_SCHEDULED", "MEETING_STARTED"]
}

Response: 200 OK
{
    "meetings": [
        {
            "meetingId": 50,
            "meetingNumber": "TEAM-MTG-50",
            "title": "Sprint Planning",
            "startDateTime": "2025-01-10T10:00:00",
            "endDateTime": "2025-01-10T11:00:00",
            "meetingProgress": "MEETING_SCHEDULED",
            "attendees": [ ... ]
        }
    ]
}
```

#### Update Meeting
```http
PUT /meeting/updateMeeting
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: meetingDetail

Body:
{
    "meetingId": 50,
    "title": "Sprint Planning (Updated)",
    "endDateTime": "2025-01-10T11:30:00"
}

Response: 200 OK
```

#### Save Minutes of Meeting
```http
POST /meeting/saveMOM
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: meetingDetail

Body:
{
    "meetingId": 50,
    "minutesOfMeeting": "## Discussion Points\n1. Sprint goals\n2. Capacity planning\n\n## Decisions\n- Start date: Jan 15\n- End date: Jan 28"
}

Response: 200 OK
```

#### Add Action Item
```http
POST /meeting/addActionItem
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: meetingDetail

Body:
{
    "meetingId": 50,
    "description": "Update sprint backlog",
    "assignedToAccountId": 2,
    "dueDate": "2025-01-12"
}

Response: 201 Created
```

---

## Leave APIs

### Base Path: `/leave`

#### Apply Leave
```http
POST /leave/leaveApplication
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: leaveApplication

Body:
{
    "accountId": 1,
    "leaveSelectionTypeId": 2,  // Multiple days
    "fromDate": "2025-01-15",
    "toDate": "2025-01-17",
    "includeLunchTime": true,
    "leaveReason": "Family vacation",
    "approverAccountId": 5,
    "phone": "+91-9876543210",
    "address": "Mumbai, India",
    "notifyTo": [2, 3]
}

Response: 201 Created
{
    "leaveApplicationId": 100,
    "leaveApplicationStatusId": 1,  // WAITING_APPROVAL
    "numberOfLeaveDays": 3.0,
    "applicationDate": "2025-01-10"
}
```

#### Change Leave Status (Approve/Reject)
```http
POST /leave/changeLeaveApplicationStatus
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 5  // Approver's account
  timeZone: Asia/Kolkata
  screenName: leaveApproval

Body:
{
    "leaveApplicationId": 100,
    "leaveApplicationStatusId": 3,  // APPROVED
    "approverReason": "Approved. Have a good trip!"
}

Response: 200 OK
{
    "message": "Leave application approved successfully"
}
```

#### Get Leave Balance
```http
POST /leave/getLeaveRemaining
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: leaveBalance

Body:
{
    "accountId": 1
}

Response: 200 OK
{
    "balances": [
        {
            "leaveTypeId": 1,
            "leaveTypeName": "Time Off",
            "leaveRemaining": 10.0,
            "leaveTaken": 2.0,
            "plannedLeaves": 3.0,
            "pendingLeaves": 0.0
        },
        {
            "leaveTypeId": 2,
            "leaveTypeName": "Sick Leave",
            "leaveRemaining": 5.0,
            "leaveTaken": 0.0,
            "plannedLeaves": 0.0,
            "pendingLeaves": 0.0
        }
    ]
}
```

#### Get Leave Policy
```http
POST /leave/getOrgLeavePolicy
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  orgId: 1
  timeZone: Asia/Kolkata
  screenName: leavePolicy

Response: 200 OK
{
    "policies": [
        {
            "leavePolicyId": 1,
            "leaveTypeId": 1,
            "leaveType": "Time Off",
            "leavePolicyTitle": "Default Time Off Policy",
            "initialLeaves": 12.0,
            "isLeaveCarryForward": true,
            "maxLeaveCarryForward": 5.0,
            "isNegativeLeaveAllowed": false,
            "includeNonBusinessDaysInLeave": false
        }
    ]
}
```

---

## Sprint APIs

### Base Path: `/sprint`

#### Create Sprint
```http
POST /sprint/createSprint
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: sprintPlanning

Body:
{
    "sprintTitle": "Sprint 15",
    "sprintObjective": "Complete user authentication module",
    "sprintExpStartDate": "2025-01-15T00:00:00",
    "sprintExpEndDate": "2025-01-28T23:59:59",
    "entityTypeId": 5,  // TEAM
    "entityId": 1,      // teamId
    "capacityAdjustmentDeadline": "2025-01-14T23:59:59"
}

Response: 201 Created
{
    "sprintId": 15,
    "sprintTitle": "Sprint 15",
    "sprintStatus": 1,  // CREATED
    "hoursOfSprint": null,
    ...
}
```

#### Start Sprint
```http
POST /sprint/startSprint/{sprintId}

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: sprintManagement

Path Parameters:
  sprintId: 15

Response: 200 OK
{
    "sprintId": 15,
    "sprintStatus": 2,  // STARTED
    "sprintActStartDate": "2025-01-15T09:00:00",
    "hoursOfSprint": 80
}
```

#### Complete Sprint
```http
POST /sprint/completeSprint/{sprintId}

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: sprintManagement

Path Parameters:
  sprintId: 15

Response: 200 OK
{
    "sprintId": 15,
    "sprintStatus": 3,  // COMPLETED
    "sprintActEndDate": "2025-01-28T18:00:00",
    "earnedEfforts": 2400,
    "stats": {
        "completedTasks": 15,
        "delayedTasks": 2,
        "notStartedTasks": 1
    }
}
```

#### Get Sprint Capacity
```http
GET /sprint/getCapacity/{sprintId}

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: sprintCapacity

Path Parameters:
  sprintId: 15

Response: 200 OK
{
    "sprintId": 15,
    "totalCapacityHours": 400,
    "usedCapacityHours": 280,
    "remainingCapacityHours": 120,
    "memberCapacities": [
        {
            "accountId": 1,
            "name": "John Doe",
            "dailyHours": 8,
            "workingDays": 10,
            "leaveDays": 0,
            "capacity": 80
        }
    ]
}
```

---

## User & Organization APIs

### User APIs - Base Path: `/user`

#### Get User Profile
```http
GET /user/getUser/{userId}

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: profile

Response: 200 OK
{
    "userId": 1,
    "primaryEmail": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "timeZone": "Asia/Kolkata",
    "accounts": [ ... ]
}
```

#### Update User
```http
PUT /user/updateUser
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: profile

Body:
{
    "userId": 1,
    "firstName": "John",
    "lastName": "Smith",
    "timeZone": "America/New_York"
}

Response: 200 OK
```

### Organization APIs - Base Path: `/organization`

#### Get Organization
```http
GET /organization/getOrganization/{orgId}

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: orgSettings

Response: 200 OK
{
    "orgId": 1,
    "organizationName": "Acme Corp",
    "organizationDisplayName": "Acme",
    "ownerAccountId": 1,
    "maxUserCount": 100,
    "paidSubscription": true
}
```

### Team APIs - Base Path: `/team`

#### Create Team
```http
POST /team/createTeam
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: teamManagement

Body:
{
    "teamName": "Backend Team",
    "teamDesc": "Handles all backend services",
    "projectId": 1,
    "orgId": 1,
    "ownerAccountId": 1
}

Response: 201 Created
{
    "teamId": 5,
    "teamName": "Backend Team",
    "teamCode": "BACK"
}
```

---

## Notification APIs

### Base Path: `/notification`

#### Get Notifications
```http
GET /notification/getNotifications

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: notifications

Query Parameters:
  page: 0
  size: 20
  categoryId: 1  // optional

Response: 200 OK
{
    "content": [
        {
            "notificationId": 500,
            "notificationTitle": "Task Assigned",
            "notificationBody": "You have been assigned task TEAM-123",
            "categoryId": 1,
            "taskNumber": "TEAM-123",
            "createdDateTime": "2025-01-05T10:30:00",
            "isRead": false
        }
    ],
    "totalElements": 50,
    "unreadCount": 10
}
```

#### Mark as Read
```http
PUT /notification/markAsRead/{notificationId}

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: notifications

Response: 200 OK
```

---

## Geo-Fencing APIs

### Base Path: `/geo-fencing`

#### Punch In
```http
POST /geo-fencing/punch/in
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: attendance

Body:
{
    "lat": 19.0760,
    "lng": 72.8777,
    "accuracy": 10.5
}

Response: 200 OK
{
    "eventType": "PUNCH_IN",
    "timestamp": "2025-01-05T09:00:00",
    "fenceId": 1,
    "fenceName": "Main Office",
    "status": "SUCCESS"
}
```

#### Punch Out
```http
POST /geo-fencing/punch/out
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: attendance

Body:
{
    "lat": 19.0760,
    "lng": 72.8777,
    "accuracy": 10.5
}

Response: 200 OK
{
    "eventType": "PUNCH_OUT",
    "timestamp": "2025-01-05T18:00:00",
    "workedHours": 9.0,
    "status": "SUCCESS"
}
```

#### Get Attendance
```http
GET /geo-fencing/attendance

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: attendance

Query Parameters:
  fromDate: 2025-01-01
  toDate: 2025-01-31

Response: 200 OK
{
    "attendance": [
        {
            "date": "2025-01-05",
            "status": "PRESENT",
            "firstIn": "09:00:00",
            "lastOut": "18:00:00",
            "workedSeconds": 32400,
            "breakSeconds": 3600
        }
    ],
    "summary": {
        "presentDays": 20,
        "absentDays": 2,
        "totalHours": 160.5
    }
}
```

#### Submit Punch Request
```http
POST /geo-fencing/punch-request
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: attendance

Body:
{
    "requestType": "PUNCH_IN",
    "requestedTime": "2025-01-05T09:00:00",
    "reason": "Forgot to punch in, was in office"
}

Response: 201 Created
{
    "requestId": 10,
    "status": "PENDING"
}
```

---

## Chat APIs

### WebSocket Endpoint

```
URL: ws://localhost:8082/chat?userId={userId}&timeZone={tz}&accountIds={ids}&authorization={token}
```

#### Message Actions

**Send Message:**
```json
{
    "action": "MESSAGE_SENT",
    "data": {
        "senderId": 1,
        "receiverId": 2,  // for DM
        "groupId": null,  // for group message
        "content": "Hello!",
        "replyId": null,
        "mentionedUserIds": []
    }
}
```

**Delivery Acknowledgment:**
```json
{
    "action": "DELIVERY_ACK",
    "data": {
        "messageIds": [100, 101, 102],
        "contextType": "GROUP",  // or "USER"
        "contextId": 5
    }
}
```

**Read Acknowledgment:**
```json
{
    "action": "READ_ACK",
    "data": {
        "messageIds": [100, 101, 102],
        "contextType": "GROUP",
        "contextId": 5
    }
}
```

**Typing Indicator:**
```json
{
    "action": "TYPING_INDICATOR",
    "data": {
        "userId": 1,
        "contextType": "GROUP",
        "contextId": 5,
        "isTyping": true
    }
}
```

**Reaction:**
```json
{
    "action": "REACTION",
    "data": {
        "messageId": 100,
        "reaction": "👍"
    }
}
```

### REST APIs - Base Path: `/api`

#### Get Chats
```http
GET /api/chats/all

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: chatList

Response: 200 OK
{
    "chats": [
        {
            "entityType": 2,  // GROUP
            "entityId": 5,
            "entityName": "Dev Team",
            "lastMessage": "Meeting at 3pm",
            "lastMessageTimestamp": "2025-01-05T14:00:00",
            "unreadMessageCount": 3,
            "isPinned": true,
            "isFavourite": false
        },
        {
            "entityType": 1,  // USER
            "entityId": 2,
            "entityName": "Jane Smith",
            "lastMessage": "Thanks!",
            "lastMessageTimestamp": "2025-01-05T13:00:00",
            "unreadMessageCount": 0,
            "isPinned": false,
            "isFavourite": false
        }
    ]
}
```

#### Get Messages (Paginated)
```http
GET /api/message/v2/group/{messageId}?accountId=1&groupId=5&size=25&pageNo=0

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: chat

Response: 200 OK
{
    "messages": [
        {
            "messageId": 100,
            "senderId": 2,
            "content": "Hello team!",
            "timestamp": "2025-01-05T10:00:00",
            "isEdited": false,
            "isDeleted": false,
            "tickStatus": "DOUBLE_BLUE_TICK",
            "reply": null,
            "fileMetadataList": []
        }
    ],
    "hasMore": true
}
```

#### Create Group
```http
POST /api/groups/create
Content-Type: application/json

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: createGroup

Body:
{
    "name": "Project Alpha Team",
    "description": "Discussion group for Project Alpha",
    "groupIconCode": "ENGINEERING_TEAM",
    "groupIconColor": "#3498DB",
    "orgId": 1,
    "users": [2, 3, 4]
}

Response: 201 Created
{
    "groupId": 10,
    "name": "Project Alpha Team",
    "users": [ ... ]
}
```

#### Upload Attachment
```http
POST /api/attachments/upload-attachments
Content-Type: multipart/form-data

Headers:
  Authorization: Bearer <token>
  accountIds: 1
  timeZone: Asia/Kolkata
  screenName: chat

Form Data:
  files: <binary file(s)>
  message: {"senderId":1,"groupId":5}
  uploaderAccountId: 1

Response: 200 OK
{
    "attachmentIds": [200, 201],
    "status": "SUCCESS"
}
```

---

## Shared DTOs

### UserDTO
```json
{
    "userId": 1,
    "accountId": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "orgId": 1,
    "isActive": true
}
```

### TaskDTO
```json
{
    "taskId": 123,
    "taskNumber": "TEAM-123",
    "taskTitle": "Title",
    "taskDesc": "Description",
    "taskTypeId": 1,
    "taskPriority": "P0",
    "taskState": "INPROGRESS",
    "currentActivityIndicator": 50,
    "taskExpStartDate": "2025-01-01T09:00:00",
    "taskExpEndDate": "2025-01-05T18:00:00",
    "taskEstimate": 8,
    "recordedEffort": 240,
    "sprintId": 5,
    "fkAccountId": { ... },
    "fkTeamId": { ... },
    "fkWorkflowTaskStatus": { ... },
    "labels": ["label1"]
}
```

### MeetingDTO
```json
{
    "meetingId": 50,
    "meetingNumber": "TEAM-MTG-50",
    "meetingKey": "abc123",
    "title": "Meeting Title",
    "agenda": "Agenda",
    "venue": "Room A",
    "startDateTime": "2025-01-10T10:00:00",
    "endDateTime": "2025-01-10T11:00:00",
    "duration": 60,
    "meetingProgress": "MEETING_SCHEDULED",
    "organizerAccountId": 1,
    "attendees": [ ... ],
    "actionItems": [ ... ]
}
```

### LeaveApplicationDTO
```json
{
    "leaveApplicationId": 100,
    "accountId": 1,
    "leaveTypeId": 1,
    "leaveApplicationStatusId": 1,
    "fromDate": "2025-01-15",
    "toDate": "2025-01-17",
    "numberOfLeaveDays": 3.0,
    "leaveReason": "Family vacation",
    "approverAccountId": 5,
    "applicationDate": "2025-01-10"
}
```

### SprintDTO
```json
{
    "sprintId": 15,
    "sprintTitle": "Sprint 15",
    "sprintObjective": "Objective",
    "sprintExpStartDate": "2025-01-15T00:00:00",
    "sprintExpEndDate": "2025-01-28T23:59:59",
    "sprintActStartDate": null,
    "sprintActEndDate": null,
    "sprintStatus": 1,
    "entityTypeId": 5,
    "entityId": 1,
    "hoursOfSprint": null,
    "earnedEfforts": null
}
```

---

## Error Handling

### HTTP Status Codes

| Code | Meaning | When Used |
|------|---------|-----------|
| 200 | OK | Successful GET, PUT |
| 201 | Created | Successful POST (create) |
| 400 | Bad Request | Validation errors |
| 401 | Unauthorized | Invalid/expired token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 406 | Not Acceptable | Business rule violation |
| 500 | Internal Server Error | Server error |
| 502 | Bad Gateway | External service error |

### Error Response Structure

```json
{
    "status": 400,
    "message": "Validation failed",
    "errors": [
        {
            "field": "taskTitle",
            "message": "Title must be between 3 and 70 characters"
        },
        {
            "field": "taskExpEndDate",
            "message": "End date must be after start date"
        }
    ],
    "timestamp": "2025-01-05T10:30:45.123+0000"
}
```

### Common Error Messages

| Error | Description |
|-------|-------------|
| `Token expired` | JWT token has expired |
| `Invalid token` | JWT signature invalid |
| `Account not active` | User account deactivated |
| `Insufficient permissions` | User lacks required role |
| `Resource not found` | Entity doesn't exist |
| `Validation failed` | Input validation error |
| `Circular dependency detected` | Task dependency loop |
| `Overlapping sprint dates` | Sprint date conflict |
| `Insufficient leave balance` | Not enough leave days |
