# Project Terminology Glossary

## Table of Contents
1. [Organization Hierarchy](#organization-hierarchy)
2. [Work Item Terminology](#work-item-terminology)
3. [Workflow & Status Terms](#workflow--status-terms)
4. [Leave & Time-Off Terms](#leave--time-off-terms)
5. [Attendance & Geo-fencing Terms](#attendance--geo-fencing-terms)
6. [Role Hierarchy](#role-hierarchy)
7. [Technical Terms](#technical-terms)
8. [Meeting Terms](#meeting-terms)
9. [GitHub Integration Terms](#github-integration-terms)

---

## Organization Hierarchy

### Organization (Org)
The top-level tenant in the system. Each organization is a separate company or business entity.
- **Entity Type ID:** 2
- **Key Fields:** orgId, organizationName, ownerAccountId
- **Contains:** Multiple BUs, Users
- **Example:** "Acme Corporation"

### Business Unit (BU)
A division or department within an organization.
- **Entity Type ID:** 3
- **Key Fields:** buId, buName, orgId
- **Contains:** Multiple Projects
- **Hierarchy:** Org → BU
- **Example:** "Engineering Department", "Sales Division"

### Project
A project within a business unit, representing a specific initiative or product.
- **Entity Type ID:** 4
- **Key Fields:** projectId, projectName, buId, orgId
- **Contains:** Multiple Teams
- **Hierarchy:** Org → BU → Project
- **Example:** "Mobile App v2.0", "Website Redesign"

### Team
The working unit where tasks are created and sprints are run.
- **Entity Type ID:** 5
- **Key Fields:** teamId, teamName, teamCode, projectId
- **Contains:** Tasks, Sprints, Members
- **Hierarchy:** Org → BU → Project → Team
- **Team Code:** Short alphanumeric identifier (e.g., "DEV", "QA")
- **Example:** "Backend Team", "QA Team"

### User
A person registered in the system.
- **Entity Type ID:** 1
- **Key Fields:** userId, primaryEmail, firstName, lastName
- **Can have:** Multiple accounts across organizations
- **Contains:** Profile information, preferences

### UserAccount / Account
A user's membership in a specific organization.
- **Key Fields:** accountId, userId, orgId, email
- **One user can have multiple accounts** (multi-org support)
- **Used for:** All business operations, authentication
- **isDefault:** Primary account flag
- **isActive:** Account enabled/disabled

---

## Work Item Terminology

### Task
The basic unit of work in the system.
- **Entity Type ID:** 6
- **Key Fields:** taskId, taskNumber, taskTitle
- **Task Number Format:** `{TEAM_CODE}-{IDENTIFIER}` (e.g., "DEV-123")

### Parent Task
A task that contains child tasks (breakdown).
- **Task Type ID:** 2
- **Has:** childTaskIds array
- **Rollup:** Effort, completion from children

### Child Task
A task that belongs to a parent task.
- **Task Type ID:** 3
- **Has:** parentTaskId reference
- **Inherits:** Sprint, priority from parent

### Bug
A defect or issue to be fixed.
- **Task Type ID:** 4
- **Special Fields:** severity, resolution, environment, customerImpact
- **RCA Fields:** isRcaDone, rcaId, rcaReason

### Epic
A large body of work that spans multiple tasks.
- **Entity Type ID:** 5 (as entity type)
- **Key Fields:** epicId, epicNumber, epicTitle
- **Contains:** Multiple tasks via fkEpicId
- **Tracks:** backlogWorkItemList, notStartedWorkItemList, startedWorkItemList, completedWorkItemList

### Initiative
A strategic goal that contains multiple epics.
- **Task Type ID:** 6
- **Higher level than Epic**

### Risk
A potential problem or issue to track.
- **Task Type ID:** 7

### Personal Task
A task for individual use, not part of team workflow.
- **Task Type ID:** 8
- **Separate entity:** PersonalTask

### Work Item
Generic term for any trackable item (Task, Bug, Epic).
- Used in: GitHub branch linking, dependencies

### Sprint
A time-boxed iteration for completing work.
- **Key Fields:** sprintId, sprintTitle, sprintExpStartDate, sprintExpEndDate
- **Sprint Status:** 1=Created, 2=Started, 3=Completed
- **Entity Scope:** Can be at team, project, or BU level

### Sprint Capacity
The total available working hours in a sprint.
- Calculated from: member availability, office hours, leaves
- **capacityAdjustmentDeadline:** Date after which capacity is locked

### Estimate
Expected effort to complete a task in hours.
- **Field:** taskEstimate
- **Ballpark Estimate:** Rough estimate flag (isBallparkEstimate)

### Recorded Effort / Logged Effort
Actual time spent on a task in minutes.
- **Field:** recordedEffort
- **Logged via:** Timesheet entries

### Earned Value / Earned Time
Credit given for completed work.
- **Field:** earnedTimeTask, earnedEfforts
- Compared to estimate for progress

---

## Workflow & Status Terms

### Workflow
A defined set of statuses for task progression.
- **Field:** taskWorkflowId
- **Example Statuses:** To Do → In Progress → Review → Done

### Workflow Task Status
A specific status within a workflow.
- **Entity:** WorkFlowTaskStatus
- **Key Fields:** workFlowTaskStatusId, workFlowTaskStatusName

### Task State
The current status of a task.
- **Field:** taskState
- **Values:** BACKLOG, TODO, IN_PROGRESS, REVIEW, DONE, BLOCKED

### Current Activity Indicator
Progress percentage (0-100).
- **Field:** currentActivityIndicator
- 0 = Not started, 100 = Complete

### Blocked
A task that cannot proceed due to dependencies or other issues.
- **Fields:** blockedReason, fkAccountIdBlockedBy
- **taskDependency:** BLOCKED enum value

### Dependency
Relationship between tasks.
- **Types:** BLOCKED_BY, BLOCKS, RELATED_TO
- **Field:** dependencyIds (array)

### StatType / Task Progress
System-calculated or user-set task status.
- **DELAYED (0):** Behind schedule
- **WATCHLIST (1):** At risk of delay
- **ONTRACK (2):** Progressing normally
- **NOTSTARTED (3):** Not yet begun
- **LATE_COMPLETION (4):** Completed after deadline
- **COMPLETED (5):** Completed on time

### Starred Task
A task marked as important/favorite.
- **Field:** isStarred
- **By:** fkAccountIdStarredBy

### Immediate Attention
A task requiring urgent focus.
- **Field:** immediateAttention (boolean)
- **From:** immediateAttentionFrom (who flagged)
- **Reason:** immediateAttentionReason

---

## Leave & Time-Off Terms

### Leave Type
Category of leave.
- **ID 1:** Time Off (vacation, personal)
- **ID 2:** Sick Leave

### Time Off Alias
Custom name for Time Off leave type.
- **Field:** EntityPreference.timeOffAlias
- **Example:** "Annual Leave", "PTO"

### Sick Leave Alias
Custom name for Sick Leave.
- **Field:** EntityPreference.sickLeaveAlias
- **Example:** "Medical Leave", "Illness"

### Leave Application
A request for leave.
- **Key Fields:** leaveApplicationId, fromDate, toDate, numberOfLeaveDays

### Leave Application Status
State of a leave request:
- **1 - WAITING_APPROVAL:** Pending manager review
- **2 - WAITING_CANCEL:** Cancellation pending
- **3 - APPROVED:** Manager approved
- **4 - REJECTED:** Manager rejected
- **5 - CANCELLED:** Employee cancelled before approval
- **6 - CANCELLED_AFTER_APPROVAL:** Cancelled after being approved
- **7 - APPLICATION_EXPIRED:** Auto-expired (not actioned in time)
- **8 - CONSUMED:** Leave taken/used

### Leave Policy
Rules for leave allocation.
- **initialLeaves:** Annual allocation
- **isLeaveCarryForward:** Allow unused leaves to roll over
- **maxLeaveCarryForward:** Maximum carry-forward days
- **isNegativeLeaveAllowed:** Allow negative balance
- **maxNegativeLeaves:** Maximum negative days allowed

### Leave Remaining
Current leave balance.
- **leaveRemaining:** Available days
- **leaveTaken:** Used days
- **Calculation:** initialLeaves - leaveTaken - approved pending

### Pro-Rata
Proportional leave allocation for mid-year joiners.
- **isMonthlyLeaveUpdateOnProRata:** Monthly accrual mode
- **Example:** Join July, get 6/12 of annual leaves

### Half-Day Leave
Leave for half a work day.
- **halfDayLeaveType:** 1=First Half, 2=Second Half
- **numberOfLeaveDays:** 0.5

### Doctor Certificate
Medical documentation for sick leave.
- **Fields:** doctorCertificate (blob), doctorCertificateFileName

---

## Attendance & Geo-fencing Terms

### Geo-Fence
A virtual geographic boundary.
- **Key Fields:** centerLat, centerLng, radiusM
- **locationKind:** OFFICE, REMOTE

### Fence Assignment
Assignment of users to geo-fences.
- **effectiveFrom / effectiveTo:** Assignment period

### Punch-In
Clock in for work.
- **Event Type:** PUNCH_IN
- Records: timestamp, location, fence

### Punch-Out
Clock out from work.
- **Event Type:** PUNCH_OUT
- Records: timestamp, calculates worked time

### Attendance Day
Daily attendance summary.
- **firstInUtc:** First punch-in
- **lastOutUtc:** Last punch-out
- **workedSeconds:** Total worked time
- **breakSeconds:** Break time
- **status:** PRESENT, ABSENT, HALF_DAY

### Attendance Event
Individual punch event.
- Tracks every in/out action with timestamps

### Punch Request
Manual correction request for attendance.
- **requestType:** PUNCH_IN or PUNCH_OUT
- **status:** PENDING, APPROVED, REJECTED

### Auto-Checkout
System automatic punch-out.
- Triggered when user misses punch-out
- **anomalies:** {"auto_checkout": true}

### Missed Punch
Attendance entry with missing punch.
- Requires manual correction

### Shift
Scheduled working hours.
- Based on EntityPreference office hours

---

## Role Hierarchy

### Org Admin
Organization administrator with full access.
- Can manage all BUs, Projects, Teams
- Can manage organization settings
- Can add/remove users

### Backup Org Admin
Secondary organization administrator.
- Same permissions as Org Admin
- Fallback administrator

### BU Admin
Business Unit administrator.
- Can manage BU settings
- Can manage projects within BU
- Cannot access other BUs

### Project Admin
Project administrator.
- Can manage project settings
- Can manage teams within project

### Team Admin
Team administrator.
- Can manage team settings
- Can manage team members

### Team Member
Regular team member.
- Can work on tasks
- Can log effort
- Can apply for leave

### Observer
View-only access.
- Can view tasks, meetings
- Cannot modify

### Mentor
Advisory role for tasks.
- **Fields:** fkAccountIdMentor1, fkAccountIdMentor2
- Receives notifications, can guide

### Manager
Approves leaves, manages team.
- **Field:** User.managingUserId
- **Approver:** LeaveApplication.approverAccountId

---

## Technical Terms

### JWT (JSON Web Token)
Authentication token.
- **Algorithm:** RS512 (RSA with SHA-512)
- **Claims:** username, accountIds, chatPassword
- **Expiration:** 30 days

### Account IDs Header
Required header for multi-account support.
- **Format:** Comma-separated (e.g., "1,2,3")
- **Validation:** Must match token claims

### Screen Name
Request identifier for logging.
- **Format:** Alphanumeric, no spaces
- **Example:** "taskDetail", "dashboard"

### Time Zone
User's timezone for date/time conversion.
- **Format:** IANA timezone (e.g., "Asia/Kolkata")
- **Required:** For all date operations

### Data Encryption Converter
JPA converter for encrypted database fields.
- **Algorithm:** AES/CBC/PKCS5Padding
- **Applied to:** emails, names, descriptions

### Optimistic Locking
Concurrency control using version field.
- **Field:** version (Integer)
- Prevents lost updates

### Soft Delete
Logical deletion without removing data.
- **Fields:** isDeleted, deletedOn, deletedBy
- Data retained for audit

---

## Meeting Terms

### Meeting
A scheduled gathering.
- **Entity Type ID:** 7
- **Meeting Number:** {TEAM_CODE}-MTG-{ID}

### Meeting Key
Unique identifier for video conference.
- Used for Jitsi room URL

### MOM (Minutes of Meeting)
Meeting notes and decisions.
- **Field:** minutesOfMeeting

### Action Item
Task assigned during meeting.
- **Fields:** description, assignedToAccountId, dueDate

### Attendee
Meeting participant.
- **RSVP Status:** 1=Yes, 2=No, 3=Maybe
- **Actual Attendance:** Did they join?

### Recurring Meeting
Template for repeating meetings.
- Generates child meeting instances

### Meeting Progress / MeetingStats
Meeting lifecycle status:
- **MEETING_SCHEDULED:** Planned
- **MEETING_STARTED:** In progress
- **MEETING_DELAYED:** Started late
- **MEETING_ENDED:** Finished
- **MEETING_OVER_RUN:** Exceeded time
- **MEETING_COMPLETED:** Done

---

## GitHub Integration Terms

### GitHub Account
Linked GitHub credentials.
- **githubAccessToken:** OAuth token (encrypted)
- **githubUserName:** GitHub username
- **isLinked:** Active link status

### Work Item GitHub Branch
Task-to-branch mapping.
- **branchName:** Git branch name
- **repositoryName:** Repository name
- **repositoryOwner:** Repository owner

### Base Branch
Source branch for new branches.
- **Example:** main, develop

### Feature Branch
Task-specific branch.
- **Naming:** feature/{TASK_NUMBER}
- **Example:** feature/dev-123

---

## Abbreviations

| Abbreviation | Full Form |
|--------------|-----------|
| BU | Business Unit |
| DM | Direct Message |
| DTO | Data Transfer Object |
| FCM | Firebase Cloud Messaging |
| JWT | JSON Web Token |
| MOM | Minutes of Meeting |
| PTO | Paid Time Off |
| RCA | Root Cause Analysis |
| RSVP | Répondez s'il vous plaît (Please respond) |
| SSO | Single Sign-On |
| TSE | Team Software Excellence |
| UTC | Coordinated Universal Time |
