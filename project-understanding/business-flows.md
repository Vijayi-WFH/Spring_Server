# Business Logic & Domain Workflows

## Table of Contents
1. [Attendance & Geo-Fencing Logic](#attendance--geo-fencing-logic)
2. [Leave Management Rules](#leave-management-rules)
3. [Meeting Workflow](#meeting-workflow)
4. [Sprint & Workflow Logic](#sprint--workflow-logic)
5. [Task Workflows](#task-workflows)
6. [GitHub Integration](#github-integration)
7. [Notification Mechanism](#notification-mechanism)
8. [Stats System (Removed/Refactored)](#stats-system-removedrefactored)

---

## Attendance & Geo-Fencing Logic

### Overview
The geo-fencing system tracks employee attendance based on their physical location relative to defined office boundaries.

### Core Concepts

#### GeoFence Definition
```java
GeoFence {
    id: Long
    orgId: Long
    name: String               // "Main Office", "Remote Hub"
    locationKind: Enum         // OFFICE, REMOTE
    centerLat: Double          // Latitude of center point
    centerLng: Double          // Longitude of center point
    radiusM: Integer           // Radius in meters
    tz: String                 // Timezone (e.g., "Asia/Kolkata")
    isActive: Boolean
}
```

#### Fence Assignment
```java
FenceAssignment {
    fenceId: Long
    accountId: Long
    effectiveFrom: LocalDate
    effectiveTo: LocalDate     // null = indefinite
}
```

### Punch-In/Out Flow

```
1. USER LOCATION CHECK
   User opens app → App gets GPS coordinates

2. FENCE VALIDATION
   For each assigned fence:
       distance = haversine(userLat, userLng, fenceLat, fenceLng)
       if (distance <= radiusM):
           return INSIDE_FENCE
   return OUTSIDE_FENCE

3. PUNCH-IN
   if (INSIDE_FENCE and !alreadyPunchedIn):
       Create AttendanceEvent {
           eventType: PUNCH_IN
           timestamp: now()
           fenceId: matchedFence
           lat, lng: userCoordinates
       }
       Update AttendanceDay.firstInUtc = now()

4. PUNCH-OUT
   if (alreadyPunchedIn):
       Create AttendanceEvent {
           eventType: PUNCH_OUT
           timestamp: now()
       }
       Update AttendanceDay {
           lastOutUtc: now()
           workedSeconds: calculate()
       }
```

### Haversine Distance Formula
```java
// Calculate distance between two lat/lng points
double haversine(lat1, lng1, lat2, lng2) {
    R = 6371000; // Earth radius in meters
    φ1 = toRadians(lat1);
    φ2 = toRadians(lat2);
    Δφ = toRadians(lat2 - lat1);
    Δλ = toRadians(lng2 - lng1);

    a = sin(Δφ/2)² + cos(φ1) * cos(φ2) * sin(Δλ/2)²;
    c = 2 * atan2(√a, √(1-a));

    return R * c; // Distance in meters
}
```

### Auto-Checkout Logic
```
SCHEDULED: Every minute (0 * * * * ?)

1. Find users who:
   - Punched in today
   - Haven't punched out
   - Past their shift end time

2. For each user:
   - Create automatic PUNCH_OUT event
   - Set lastOutUtc = shift_end_time
   - Mark anomalies["auto_checkout"] = true
```

### Missed Punch Handling
```
1. User submits PunchRequest:
   - requestType: PUNCH_IN or PUNCH_OUT
   - requestedTime: desired timestamp
   - reason: explanation

2. Manager reviews and approves/rejects

3. If APPROVED:
   - Create AttendanceEvent with requestedTime
   - Update AttendanceDay accordingly
   - Mark anomalies["manual_correction"] = true
```

### Pre-Shift Notification
```
SCHEDULED: Every minute (0 * * * * ?)

1. Find users with shift starting in next 15 minutes
2. Check if they're inside their assigned fence
3. If OUTSIDE_FENCE:
   - Send push notification: "Your shift starts soon"
```

---

## Leave Management Rules

### Leave Types

| ID | Type | Description |
|----|------|-------------|
| 1 | Time Off | General leave (vacation, personal) |
| 2 | Sick Leave | Medical leave with certificate option |

### Leave Policy Configuration

```java
LeavePolicy {
    // Scope (hierarchical - most specific wins)
    orgId: Long              // Required
    buId: Long               // Optional - overrides org
    projectId: Long          // Optional - overrides BU
    teamId: Long             // Optional - overrides project

    // Allocation
    initialLeaves: Float     // Annual allocation (e.g., 12)

    // Carry-forward
    isLeaveCarryForward: Boolean
    maxLeaveCarryForward: Float

    // Negative balance
    isNegativeLeaveAllowed: Boolean
    maxNegativeLeaves: Float

    // Weekend handling
    includeNonBusinessDaysInLeave: Boolean
}
```

### Pro-Rata Calculation (New Joiners)

```java
// For mid-year joiners
calculateProRataLeaves(joinDate, initialLeaves, isMonthlyProRata) {
    currentYear = Year.now();
    monthsRemaining = 12 - joinDate.getMonthValue() + 1;

    if (!isMonthlyProRata) {
        // Immediate allocation based on remaining months
        return initialLeaves * (monthsRemaining / 12.0);
    } else {
        // Start with 0, monthly updates will add
        return 0;
    }
}

// Example: Join July 15, 12 days annual
// !isMonthlyProRata: 12 * (6/12) = 6 days
// isMonthlyProRata: 0 days (will get 1/month)
```

### Leave Application Workflow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    LEAVE APPLICATION WORKFLOW                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  EMPLOYEE                                                           │
│  ────────                                                           │
│  1. Submit leave application                                        │
│     - Select dates (fromDate, toDate)                               │
│     - Select leave type                                             │
│     - Provide reason                                                │
│     - Upload certificate (sick leave)                               │
│     - Select approver (manager)                                     │
│                                                                     │
│  VALIDATION                                                         │
│  ──────────                                                         │
│  2. System validates:                                               │
│     - fromDate <= toDate                                            │
│     - Sufficient balance (leaveRemaining >= numberOfDays)           │
│     - No overlapping leaves                                         │
│     - Valid approver (is manager)                                   │
│     - Half-day consistency                                          │
│                                                                     │
│  3. Calculate leave days:                                           │
│     - Exclude weekends (based on EntityPreference.offDays)          │
│     - Exclude holidays (from HolidayOffDay)                         │
│     - Handle half-day (0.5 days)                                    │
│     - Subtract lunch hour if includeLunchTime=false                 │
│                                                                     │
│  4. Create application with status = WAITING_APPROVAL (1)           │
│                                                                     │
│  MANAGER                                                            │
│  ───────                                                            │
│  5. Review application                                              │
│     - APPROVE → status = APPROVED (3)                               │
│       - Deduct from leaveRemaining                                  │
│       - Add to leaveTaken                                           │
│       - Notify employee                                             │
│                                                                     │
│     - REJECT → status = REJECTED (4)                                │
│       - No balance change                                           │
│       - Notify employee with reason                                 │
│                                                                     │
│  POST-APPROVAL                                                      │
│  ─────────────                                                      │
│  6. Employee cancellation:                                          │
│     - Before fromDate: status = CANCELLED (5)                       │
│       - Restore balance                                             │
│     - After fromDate: status = CANCELLED_AFTER_APPROVAL (6)         │
│       - Restore balance                                             │
│                                                                     │
│  7. Leave consumption (daily 3 AM job):                             │
│     - If toDate < today and status = APPROVED:                      │
│       - status = CONSUMED (8)                                       │
│                                                                     │
│  8. Application expiry (daily 23:58:59 job):                        │
│     - If status = WAITING_APPROVAL and fromDate < today:            │
│       - status = APPLICATION_EXPIRED (7)                            │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Leave Balance Calculation

```java
calculateLeaveBalance(accountId, leaveTypeId) {
    leaveRemaining = getLeaveRemaining(accountId, leaveTypeId);

    // Available balance
    available = leaveRemaining.leaveRemaining;

    // Planned leaves (approved but future)
    planned = sumOf(
        LeaveApplication
        WHERE accountId = :accountId
        AND leaveTypeId = :leaveTypeId
        AND status IN (WAITING_APPROVAL, APPROVED)
        AND toDate >= today
    );

    // Consumed leaves
    consumed = sumOf(
        LeaveApplication
        WHERE accountId = :accountId
        AND leaveTypeId = :leaveTypeId
        AND status = CONSUMED
    );

    // Pending approval
    pending = sumOf(
        LeaveApplication
        WHERE accountId = :accountId
        AND leaveTypeId = :leaveTypeId
        AND status = WAITING_APPROVAL
    );

    return {
        available: available,
        planned: planned,
        consumed: consumed,
        pending: pending,
        actualRemaining: available - planned
    };
}
```

### Annual Leave Reset (Jan 1st)

```java
// Cron: 0 0 0 1 1 * (Midnight, Jan 1st)
resetAnnualLeaves() {
    for each LeaveRemaining where currentlyActive = true:
        policy = getLeavePolicy(leaveRemaining.leavePolicyId);

        // Calculate carry-forward
        if (policy.isLeaveCarryForward) {
            carryForward = min(
                leaveRemaining.leaveRemaining,
                policy.maxLeaveCarryForward
            );
        } else {
            carryForward = 0;
        }

        // Reset for new year
        leaveRemaining.leaveRemaining = policy.initialLeaves + carryForward;
        leaveRemaining.leaveTaken = 0;
        leaveRemaining.calenderYear = currentYear;
}
```

### Monthly Leave Update (Last Day)

```java
// Cron: 59 58 23 L * ? (Last day of month, 23:58:59)
monthlyLeaveUpdate() {
    for each LeaveRemaining where currentlyActive = true:
        policy = getLeavePolicy(leaveRemaining.leavePolicyId);
        entityPref = getEntityPreference(policy.teamId or orgId);

        if (entityPref.isMonthlyLeaveUpdateOnProRata) {
            monthlyAllocation = policy.initialLeaves / 12.0;
            leaveRemaining.leaveRemaining += monthlyAllocation;
        }
}
```

---

## Meeting Workflow

### Meeting Lifecycle

```
CREATED → SCHEDULED → STARTED → ENDED → COMPLETED
              ↓           ↓        ↓
           DELAYED    OVER_RUN  OVER_RUN
```

### Meeting Creation

```java
createMeeting(request) {
    // Generate meeting number
    sequence = getNextMeetingSequence(teamId);
    meetingNumber = teamCode + "-MTG-" + sequence;

    // Generate Jitsi key
    meetingKey = UUID.randomUUID().toString();

    // Create meeting
    meeting = Meeting {
        meetingNumber: meetingNumber,
        meetingKey: meetingKey,
        title: request.title,
        startDateTime: request.startDateTime,
        endDateTime: request.endDateTime,
        duration: calculateDuration(),
        organizerAccountId: currentUser,
        meetingProgress: MEETING_SCHEDULED
    };

    // Add organizer as attendee
    addAttendee(meeting, currentUser, RSVP_YES);

    // Add other attendees
    for each attendeeId in request.attendees:
        addAttendee(meeting, attendeeId, RSVP_PENDING);

    // Schedule reminder
    scheduleReminder(meeting, request.reminderTime);

    return meeting;
}
```

### Meeting Start Confirmation

```java
// Scheduler: Every 60 seconds
startMeetingConfirmation() {
    // Find meetings starting now (±2 minutes)
    meetings = findMeetingsStartingNow();

    for each meeting:
        if (meeting.meetingProgress == MEETING_SCHEDULED) {
            // Check if past scheduled start
            if (now() > meeting.startDateTime) {
                meeting.meetingProgress = MEETING_DELAYED;
            } else {
                meeting.meetingProgress = MEETING_STARTED;
                meeting.actualStartDateTime = now();
            }
        }
}
```

### Meeting End Confirmation

```java
// Scheduler: Every 60 seconds
endMeetingConfirmation() {
    // Find meetings past their end time
    meetings = findMeetingsPastEndTime();

    for each meeting:
        if (meeting.meetingProgress IN (STARTED, DELAYED)) {
            if (now() > meeting.endDateTime) {
                meeting.meetingProgress = MEETING_OVER_RUN;
            }
        }
}
```

### Meeting Reminders

```java
// Scheduler: Every 60 seconds
meetingReminderScheduler() {
    // Find meetings with reminders due
    meetings = findMeetingsWithRemindersDue();

    for each meeting:
        for each attendee:
            sendNotification({
                type: MEETING_REMINDER,
                title: "Meeting starting soon",
                body: meeting.title + " starts in " + reminderTime + " minutes",
                payload: { meetingId, meetingNumber }
            });
}
```

---

## Sprint & Workflow Logic

### Sprint Lifecycle

```
CREATED (1) ─────────────→ STARTED (2) ─────────────→ COMPLETED (3)
     │                          │                          │
     │ - Plan capacity          │ - Track progress         │ - Calculate metrics
     │ - Add work items         │ - Daily standups         │ - Move incomplete
     │ - Set dates              │ - Update estimates       │ - Archive stats
     │                          │ - Adjust capacity        │
```

### Sprint Creation

```java
createSprint(request) {
    // Validate dates
    if (request.startDate <= today) {
        throw "Start date must be in future";
    }
    if (request.endDate <= request.startDate) {
        throw "End date must be after start date";
    }

    // Check for overlapping sprints
    overlapping = findOverlappingSprints(
        request.entityTypeId,
        request.entityId,
        request.startDate,
        request.endDate
    );
    if (overlapping.isNotEmpty()) {
        throw "Sprint dates overlap with existing sprint";
    }

    // Create sprint
    sprint = Sprint {
        sprintTitle: request.title,
        sprintObjective: request.objective,
        sprintExpStartDate: request.startDate,
        sprintExpEndDate: request.endDate,
        sprintStatus: CREATED,
        entityTypeId: request.entityTypeId,  // e.g., TEAM
        entityId: request.entityId,          // e.g., teamId
        capacityAdjustmentDeadline: request.startDate.minusDays(1)
    };

    return sprint;
}
```

### Sprint Start

```java
startSprint(sprintId) {
    sprint = getSprint(sprintId);

    // Validate
    if (sprint.sprintStatus != CREATED) {
        throw "Can only start a created sprint";
    }
    if (now() < sprint.sprintExpStartDate) {
        // Starting early
        sprint.sprintActStartDate = now();
    } else {
        sprint.sprintActStartDate = sprint.sprintExpStartDate;
    }

    // Update status
    sprint.sprintStatus = STARTED;

    // Calculate sprint hours
    sprint.hoursOfSprint = calculateSprintHours(
        sprint.sprintActStartDate,
        sprint.sprintExpEndDate,
        getOffDays(sprint.entityId)
    );

    // Lock capacity adjustments
    sprint.canModifyEstimates = false;

    return sprint;
}
```

### Sprint Completion

```java
completeSprint(sprintId) {
    sprint = getSprint(sprintId);

    // Validate
    if (sprint.sprintStatus != STARTED) {
        throw "Can only complete a started sprint";
    }

    // Set actual end date
    sprint.sprintActEndDate = now();
    sprint.sprintStatus = COMPLETED;

    // Calculate earned efforts
    completedTasks = getTasksBySprintAndStatus(sprintId, COMPLETED);
    sprint.earnedEfforts = sumOf(task.recordedEffort for task in completedTasks);

    // Save sprint stats
    saveSprintStats(sprint, {
        notStartedTasks: countByStatus(NOTSTARTED),
        onTrackTasks: countByStatus(ONTRACK),
        delayedTasks: countByStatus(DELAYED),
        completedTasks: countByStatus(COMPLETED),
        lateCompletedTasks: countByStatus(LATE_COMPLETION)
    });

    // Move incomplete tasks to next sprint (if configured)
    if (hasNextSprint(sprint)) {
        moveIncompleteTasks(sprint, nextSprint);
    }

    return sprint;
}
```

### Capacity Planning

```java
calculateSprintCapacity(sprintId) {
    sprint = getSprint(sprintId);
    members = sprint.sprintMembers;

    totalCapacity = 0;

    for each member in members:
        // Get member's daily capacity
        dailyHours = getMemberDailyHours(member.accountId);

        // Calculate working days in sprint
        workingDays = countWorkingDays(
            sprint.sprintExpStartDate,
            sprint.sprintExpEndDate,
            getOffDays(member.accountId)
        );

        // Subtract leaves
        leaveDays = getApprovedLeaves(
            member.accountId,
            sprint.sprintExpStartDate,
            sprint.sprintExpEndDate
        );

        // Member capacity
        memberCapacity = (workingDays - leaveDays) * dailyHours;
        totalCapacity += memberCapacity;

    return totalCapacity;
}
```

---

## Task Workflows

### Task States

```
        ┌─────────┐
        │ BACKLOG │
        └────┬────┘
             │ Add to Sprint
             ▼
        ┌─────────┐
        │ TODO    │
        └────┬────┘
             │ Start Work
             ▼
        ┌─────────────┐
        │ IN_PROGRESS │◄────────────┐
        └──────┬──────┘             │
               │                    │ Reopen
               ▼                    │
        ┌─────────┐                 │
        │ REVIEW  │─────────────────┘
        └────┬────┘   Reject
             │ Approve
             ▼
        ┌─────────┐
        │  DONE   │
        └─────────┘
```

### Task Creation

```java
createTask(request) {
    // Generate task number
    identifier = getNextTaskIdentifier(request.teamId);
    taskNumber = teamCode + "-" + identifier;

    // Create task
    task = Task {
        taskNumber: taskNumber,
        taskIdentifier: identifier,
        taskTitle: request.title,
        taskDesc: request.description,
        taskTypeId: request.taskTypeId,
        taskPriority: request.priority,

        // Workflow
        taskWorkflowId: getDefaultWorkflow(teamId),
        fkWorkflowTaskStatus: getInitialStatus(workflowId),
        currentActivityIndicator: 0,

        // Assignments
        fkAccountId: request.assigneeId,
        fkAccountIdCreator: currentUser,

        // Organization
        fkTeamId: request.teamId,
        fkOrgId: request.orgId,
        fkProjectId: request.projectId,

        // Scheduling
        taskExpStartDate: request.expectedStartDate,
        taskExpEndDate: request.expectedEndDate,
        taskEstimate: request.estimate
    };

    // Create history entry
    createTaskHistory(task, "CREATE");

    // Send notification to assignee
    notifyTaskAssigned(task);

    return task;
}
```

### Task Update Flow

```java
updateTask(taskId, changes) {
    existingTask = getTask(taskId);

    // Track changes
    changedFields = [];

    for each field in changes:
        if (existingTask[field] != changes[field]) {
            changedFields.add({
                field: field,
                oldValue: existingTask[field],
                newValue: changes[field]
            });
            existingTask[field] = changes[field];
        }

    // Handle special field changes
    if ("fkWorkflowTaskStatus" in changedFields) {
        handleStatusChange(existingTask, oldStatus, newStatus);
    }

    if ("fkAccountId" in changedFields) {
        handleAssigneeChange(existingTask, oldAssignee, newAssignee);
    }

    if ("sprintId" in changedFields) {
        handleSprintChange(existingTask, oldSprint, newSprint);
    }

    // Create history entries
    for each change in changedFields:
        createTaskHistory(existingTask, "UPDATE", change);

    // Update last modified
    existingTask.lastUpdatedDateTime = now();
    existingTask.fkAccountIdLastUpdated = currentUser;

    return existingTask;
}
```

### Parent-Child Task Logic

```java
// Creating a child task
createChildTask(parentTaskId, childRequest) {
    parentTask = getTask(parentTaskId);

    // Create child
    childTask = createTask(childRequest);
    childTask.parentTaskId = parentTaskId;
    childTask.taskTypeId = CHILD_TASK;

    // Update parent
    parentTask.taskTypeId = PARENT_TASK;
    parentTask.childTaskIds.add(childTask.taskId);

    // Inherit properties from parent if not specified
    if (childTask.sprintId == null) {
        childTask.sprintId = parentTask.sprintId;
    }
    if (childTask.taskPriority == null) {
        childTask.taskPriority = parentTask.taskPriority;
    }

    // Update parent estimates
    recalculateParentEstimates(parentTask);

    return childTask;
}

// Update child affects parent
onChildTaskUpdate(childTask, changes) {
    parentTask = getTask(childTask.parentTaskId);

    if ("recordedEffort" in changes) {
        // Sum child efforts to parent
        parentTask.recordedEffort = sumOf(
            childTask.recordedEffort for childTask in parentTask.childTaskIds
        );
    }

    if ("fkWorkflowTaskStatus" in changes) {
        // Check if all children complete
        allChildrenComplete = checkAllChildrenComplete(parentTask);
        if (allChildrenComplete) {
            parentTask.fkWorkflowTaskStatus = COMPLETED;
        }
    }
}
```

### Dependency Management

```java
// Dependency types
enum DependencyType {
    BLOCKED_BY,      // This task is blocked by another
    BLOCKS,          // This task blocks another
    RELATED_TO       // Related tasks
}

addDependency(sourceTaskId, targetTaskId, type) {
    sourceTask = getTask(sourceTaskId);
    targetTask = getTask(targetTaskId);

    // Validate no circular dependency
    if (wouldCreateCircularDependency(sourceTaskId, targetTaskId)) {
        throw "Circular dependency detected";
    }

    // Create dependency
    dependency = Dependency {
        sourceTaskId: sourceTaskId,
        targetTaskId: targetTaskId,
        dependencyType: type
    };

    // Update task fields
    if (type == BLOCKED_BY) {
        sourceTask.dependencyIds.add(targetTaskId);
        sourceTask.taskDependency = BLOCKED;
        sourceTask.fkAccountIdBlockedBy = targetTask.fkAccountId;
    }

    return dependency;
}
```

---

## GitHub Integration

### OAuth Flow

```
1. User initiates GitHub link
   └─ Frontend redirects to GitHub OAuth

2. GitHub callback with authorization code
   └─ POST /github/linkAccount { code: authCode }

3. Exchange code for access token
   └─ Call GitHub API: POST /oauth/access_token

4. Get GitHub user info
   └─ Call GitHub API: GET /user

5. Store credentials
   └─ GithubAccount {
        orgId: currentOrg,
        githubUserCode: authCode,
        githubAccessToken: encryptedToken,
        githubUserName: githubUser.login,
        fkUserId: currentUser
      }
```

### Branch Synchronization

```java
syncBranches(githubAccountId) {
    account = getGithubAccount(githubAccountId);

    // Get repos user has access to
    repos = callGitHubAPI("/user/repos", account.githubAccessToken);

    for each repo in repos:
        // Get branches
        branches = callGitHubAPI(
            "/repos/" + repo.owner + "/" + repo.name + "/branches",
            account.githubAccessToken
        );

        // Update local cache
        for each branch in branches:
            updateBranchCache(repo, branch);

    return branches;
}
```

### Task-Branch Linking

```java
linkTaskToBranch(taskId, branchName, repoName, repoOwner) {
    task = getTask(taskId);

    // Create mapping
    mapping = WorkItemGithubBranch {
        taskId: taskId,
        branchName: branchName,
        repositoryName: repoName,
        repositoryOwner: repoOwner,
        githubAccountId: getCurrentGithubAccount()
    };

    // Update task
    task.referenceWorkItemId.add(mapping.id);

    return mapping;
}

// Auto-create branch for task
createBranchForTask(taskId, baseBranch) {
    task = getTask(taskId);
    account = getCurrentGithubAccount();

    // Generate branch name
    branchName = "feature/" + task.taskNumber.toLowerCase();

    // Create branch via GitHub API
    response = callGitHubAPI(
        "/repos/{owner}/{repo}/git/refs",
        {
            ref: "refs/heads/" + branchName,
            sha: getBaseBranchSha(baseBranch)
        },
        account.githubAccessToken
    );

    // Link to task
    linkTaskToBranch(taskId, branchName, repo, owner);

    return branchName;
}
```

---

## Notification Mechanism

### Notification Types

| Category | Types |
|----------|-------|
| Task | TASK_ASSIGNED, TASK_UPDATED, TASK_COMPLETED, TASK_COMMENT |
| Meeting | MEETING_INVITE, MEETING_REMINDER, MEETING_UPDATED, MEETING_CANCELLED |
| Leave | LEAVE_APPLIED, LEAVE_APPROVED, LEAVE_REJECTED, LEAVE_CANCELLED |
| Sprint | SPRINT_STARTED, SPRINT_COMPLETED, SPRINT_ENDING |
| System | ALERT, REMINDER |

### Multi-Channel Delivery

```
┌─────────────────────────────────────────────────────────────────┐
│                    NOTIFICATION FLOW                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Event Occurs (Task Update, Meeting, etc.)                       │
│       │                                                          │
│       ▼                                                          │
│  NotificationService.createNotification()                        │
│       │                                                          │
│       ├──────────────────┬──────────────────┐                    │
│       ▼                  ▼                  ▼                    │
│  ┌─────────┐      ┌─────────────┐    ┌──────────┐               │
│  │ In-App  │      │   Firebase  │    │  Email   │               │
│  │  Store  │      │    (FCM)    │    │  (SMTP)  │               │
│  └────┬────┘      └──────┬──────┘    └────┬─────┘               │
│       │                  │                │                      │
│       ▼                  ▼                ▼                      │
│   Database         Mobile Push       Email Server               │
│   (notification    Notification                                  │
│    table)                                                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### FCM Integration

```java
sendPushNotification(userId, title, body, payload) {
    // Get user's FCM tokens
    tokens = getFirebaseTokens(userId);

    for each token in tokens:
        message = Message.builder()
            .setToken(token.tokenValue)
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .putAllData(payload)
            .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (InvalidTokenException e) {
            // Remove invalid token
            deleteFirebaseToken(token);
        }
}
```

### Notification Cleanup

```java
// Scheduler: Daily midnight (0 0 0 * * *)
deleteOldNotifications() {
    // Default retention: 30 days
    cutoffDate = now().minusDays(30);

    notifications = findNotificationsOlderThan(cutoffDate);

    for each notification in notifications:
        deleteNotification(notification.id);

    log("Deleted " + notifications.size() + " old notifications");
}
```

---

## Stats System (Removed/Refactored)

### Original Purpose
The stats system calculated task progress status automatically based on various factors:
- Task priority
- Expected end dates
- Actual progress
- Office hours
- Buffer times

### StatType Enum (Still Present)
```java
enum StatType {
    DELAYED(0),           // Task is behind schedule
    WATCHLIST(1),         // Task is at risk
    ONTRACK(2),           // Progressing normally
    NOTSTARTED(3),        // Not yet begun
    LATE_COMPLETION(4),   // Completed after deadline
    COMPLETED(5)          // Completed on time
}
```

### Task Fields (Still Present)
```java
// In Task entity
taskProgressSystem: StatType;                    // System-calculated
taskProgressSystemLastUpdated: LocalDateTime;
nextTaskProgressSystemChangeDateTime: LocalDateTime;
taskProgressSetByUser: StatType;                 // User override
taskProgressSetByAccountId: Long;
taskProgressSetByUserLastUpdated: LocalDateTime;
```

### Original Calculation Logic (REMOVED)

The following methods exist as stubs but internal logic was removed:

```java
// StatsService.java - Methods exist but gutted

// Derive estimate from history if not provided
deriveTimeEstimateForTask(Task task) {
    // Logic removed
}

// Calculate expected end based on priority
deriveEndTimeForTask(Task task, String timeZone) {
    // Logic removed
    // Original: Used priority buffers (P0=3hrs, P1=6hrs)
}

// Check if started task is delayed
isStartedTaskDelayed(Task task) {
    // Logic removed
    // Original: Compared actual progress vs expected progress
}

// Check if todo task should be marked delayed
isTodoTaskDelayed(Task task) {
    // Logic removed
    // Original: Checked if expected start date passed
}

// Main computation method
computeAndUpdateStat(Task task, boolean isComputeAllowed) {
    // Logic removed
    // Original: Complex algorithm using:
    //   - Office hours
    //   - Priority buffers
    //   - Remaining time calculations
    //   - Effort tracking
}
```

### Buffer Constants (Still Present)
```java
// In Constants.java
END_TIME_P0_BUFFER = 3 hours;    // Priority 0 buffer
END_TIME_P1_BUFFER = 6 hours;    // Priority 1 buffer

BUFFER_TIME_FOR_ESTIMATE = {
    bufferTimeSmall: 0 min,
    bufferTimeMedium: 30 min,
    bufferTimeLarge: 1 hour,
    bufferTimeLarge_1: 1.5 hours,
    bufferTimeLarge_2: 2 hours,
    bufferTimeLarge_3: 4 hours    // For estimates > 16 hrs
};
```

### Current Usage
The stats system infrastructure remains for:
1. Manual status setting by users (`taskProgressSetByUser`)
2. Filtering tasks by status in UI
3. Sprint statistics in `CompletedSprintStats`
4. Dashboard views via `StatsController` endpoints

### Why Removed?
Comments in code suggest:
- "Incomplete and wrong logic"
- Complex timezone handling issues
- Office hours should come from database, not constants
- Algorithm accuracy concerns

### Deprecation Comments
```java
// @deprecated: this method has incomplete and wrong logic
// to calculate the status of the task
// this method is used by /getStats api
public List<Task> filterTasksForStats(StatType statName, List<Task> allTasks)

// @deprecated: The office start time in stats algo will come
// from the database instead of taking it from the constant file.
```

### Current State
- **Active**: StatsController with 3 endpoints
- **Active**: Task fields for status tracking
- **Active**: Manual status setting by users
- **Removed**: Automatic calculation algorithms
- **Removed**: Time-based status transitions
- **Removed**: Priority buffer calculations

The system now relies on users manually updating task status rather than automatic calculation based on time and progress.
