# Known Edge Cases & Tricky Scenarios

## Table of Contents
1. [Organization & Team Edge Cases](#organization--team-edge-cases)
2. [Attendance & Geo-fencing Edge Cases](#attendance--geo-fencing-edge-cases)
3. [Leave Management Edge Cases](#leave-management-edge-cases)
4. [Task & Workflow Edge Cases](#task--workflow-edge-cases)
5. [Meeting Edge Cases](#meeting-edge-cases)
6. [GitHub Integration Edge Cases](#github-integration-edge-cases)
7. [Data Integrity Edge Cases](#data-integrity-edge-cases)
8. [Timezone Edge Cases](#timezone-edge-cases)

---

## Organization & Team Edge Cases

### Cross-Org Team Code Collisions
**Scenario:** Two organizations create teams with the same team code.

**Problem:** Team codes are used in task numbers (e.g., "DEV-123"), but codes are only unique within an organization.

**Current Behavior:**
- Team codes are unique per organization only
- Task numbers include team code, not org prefix
- Cross-org searches might return ambiguous results

**Mitigation:**
```java
// In TeamService - validate uniqueness within org only
if (teamRepository.existsByTeamCodeAndOrgId(teamCode, orgId)) {
    throw new ValidationException("Team code already exists in this organization");
}
```

**Edge Case Handling:**
- Always filter by orgId when searching by team code
- Include orgId in task search queries
- Display organization context in UI when showing tasks

---

### Multi-Association Users
**Scenario:** User belongs to multiple organizations via different accounts.

**Problem:** User has multiple accountIds, needs to switch between org contexts.

**Current Behavior:**
```java
// User entity
Boolean multiAssociation;  // true if user has accounts in multiple orgs

// JWT token contains all accountIds
claims.put("accountIds", [1, 2, 3]);  // accounts across orgs

// Request header specifies current context
accountIds: "1"  // or "1,2" for specific accounts
```

**Edge Cases:**
1. User applies for leave - which account/org?
2. User creates task - which team context?
3. Notification routing - which account receives it?

**Resolution:**
- Request headers specify active account(s)
- Default account used when ambiguous
- JWT filter validates requested accountIds against token

---

### Duplicate Child-Parent Validations
**Scenario:** Preventing circular task hierarchies.

**Problem:**
- Task A is parent of Task B
- User tries to make Task B parent of Task A

**Validation Logic:**
```java
// In DependencyService
boolean wouldCreateCircularDependency(Long sourceTaskId, Long targetTaskId) {
    Set<Long> visited = new HashSet<>();
    return hasPath(targetTaskId, sourceTaskId, visited);
}

private boolean hasPath(Long current, Long target, Set<Long> visited) {
    if (current.equals(target)) return true;
    if (visited.contains(current)) return false;
    visited.add(current);

    Task task = taskRepository.findById(current);
    if (task.getChildTaskIds() != null) {
        for (Long childId : task.getChildTaskIds()) {
            if (hasPath(childId, target, visited)) return true;
        }
    }
    return false;
}
```

---

### Soft Delete vs Hard Delete
**Scenario:** Different entities have different deletion behaviors.

| Entity | Deletion Type | Behavior |
|--------|---------------|----------|
| User | Soft | isActive=false, accounts deactivated |
| Team | Soft | isDeleted=true, tasks remain |
| Task | Soft | statusAtTimeOfDeletion recorded |
| Message (Chat) | Soft | isDeleted=true, content hidden |
| Notification | Hard | Deleted after retention period |
| Leave Application | Never | Always retained for audit |

**Edge Cases:**
1. Restoring deleted team - what about reassigned tasks?
2. Deleting user with pending approvals - reassign approver
3. Orphaned tasks after team deletion

---

## Attendance & Geo-fencing Edge Cases

### Night Shift Attendance
**Scenario:** User works from 10 PM to 6 AM (crosses midnight).

**Problem:**
- Punch-in on Day 1, punch-out on Day 2
- Which date gets the attendance?

**Current Handling:**
```java
// AttendanceDay tracks by dateKey
// Night shift creates:
// - Day 1: punch-in at 22:00, no punch-out
// - Day 2: punch-out at 06:00, no punch-in

// Auto-checkout job (runs every minute)
// Checks for users past their shift end
// Night shift special case:
if (shiftEndsAfterMidnight && currentTime < shiftEndTime) {
    // Don't auto-checkout yet, shift continues next day
}
```

**Edge Cases:**
1. User forgets to punch out - auto-checkout at shift end
2. User punches in twice (Day 1 evening, Day 2 morning) - separate days
3. Leave on Day 2 - Day 1 attendance affected?

---

### Geo-fence Conflict Assignments
**Scenario:** User assigned to multiple overlapping fences.

**Problem:**
- Fence A: Office (500m radius)
- Fence B: Campus (2000m radius)
- User inside both - which fence is "active"?

**Resolution Priority:**
1. Most restrictive (smallest radius) first
2. Most recently assigned
3. locationKind priority: OFFICE > REMOTE

```java
// In GeoFenceService
GeoFence findMatchingFence(Long accountId, double lat, double lng) {
    List<FenceAssignment> assignments = getActiveAssignments(accountId);

    // Sort by radius ascending (smallest first)
    assignments.sort(comparing(a -> a.getFence().getRadiusM()));

    for (FenceAssignment assignment : assignments) {
        GeoFence fence = assignment.getFence();
        double distance = calculateDistance(lat, lng,
            fence.getCenterLat(), fence.getCenterLng());

        if (distance <= fence.getRadiusM()) {
            return fence;  // Return first matching (smallest)
        }
    }
    return null;  // Outside all fences
}
```

---

### Boundary Condition Punching
**Scenario:** User exactly on fence boundary.

**Problem:** User at exactly radiusM distance from center.

**Handling:**
```java
// Use <= not < for boundary inclusion
if (distance <= fence.getRadiusM()) {
    return INSIDE_FENCE;
}
```

**GPS Accuracy Consideration:**
```java
// Account for GPS accuracy in validation
double effectiveDistance = distance - gpsAccuracyMeters;
if (effectiveDistance <= fence.getRadiusM()) {
    return INSIDE_FENCE;
}
```

---

## Leave Management Edge Cases

### Leave Retention Rules
**Scenario:** Annual leave reset with carry-forward.

**Edge Cases:**

1. **Carry-forward exceeds cap:**
```java
// User has 10 days remaining, max carry-forward is 5
carryForward = Math.min(leaveRemaining, maxLeaveCarryForward);
// carryForward = 5 days (capped)
newBalance = initialLeaves + carryForward;
// 5 extra days are forfeited
```

2. **Negative balance at year end:**
```java
// User has -3 days (borrowed)
if (leaveRemaining < 0) {
    // Option A: Carry negative forward
    newBalance = initialLeaves + leaveRemaining;  // 12 + (-3) = 9

    // Option B: Reset to zero (forgive debt)
    newBalance = initialLeaves;  // 12
}
```

3. **Mid-year policy change:**
```java
// Policy increases from 12 to 15 days mid-year
oldBalance = 6 days remaining
adjustment = (15 - 12) * (monthsRemaining / 12)
newBalance = oldBalance + adjustment
```

---

### Leave Overlapping Dates
**Scenario:** User applies for leave that overlaps existing application.

**Validation:**
```java
// In LeaveService.validateLeaveApplication()
List<LeaveApplication> existing = repository.findByAccountIdAndOverlappingDateRange(
    accountId,
    request.getFromDate(),
    request.getToDate(),
    Arrays.asList(WAITING_APPROVAL, APPROVED)
);

if (!existing.isEmpty()) {
    throw new ValidationException("Leave dates overlap with existing application");
}
```

**Edge Cases:**
1. Apply for Jan 10-12, already have Jan 12-15 approved
2. Apply for half-day when full-day approved
3. Apply spanning year boundary (Dec 28 - Jan 2)

---

### Half-Day Leave Combinations
**Scenario:** Mixing half-day and full-day leaves.

**Edge Cases:**

1. **Same day half-days:**
```java
// Can't apply for first-half AND second-half same day
// Unless combining into full day
```

2. **Half-day with meeting:**
```java
// Meeting from 9 AM - 10 AM
// User applies for first-half leave (9 AM - 1 PM)
// Should meeting be auto-cancelled?
```

3. **Half-day calculation:**
```java
if (isHalfDay) {
    numberOfLeaveDays = 0.5f;
} else {
    numberOfLeaveDays = calculateBusinessDays(fromDate, toDate);
}
```

---

### Leave Approver Change
**Scenario:** Manager who approved leave is deactivated.

**Problem:**
- Pending leave applications need new approver
- Approved leaves reference old approver

**Handling:**
```java
// When manager is deactivated
void reassignPendingLeaves(Long oldApproverId, Long newApproverId) {
    leaveRepository.bulkUpdateApprover(
        accountIds,           // affected employees
        oldApproverId,
        Arrays.asList(WAITING_APPROVAL, WAITING_CANCEL),
        newApproverId
    );
}
```

---

## Task & Workflow Edge Cases

### Sprint Changed Mid-Sprint
**Scenario:** Task moved from active sprint to another.

**Problems:**
1. Effort logged in old sprint
2. Sprint capacity affected
3. Sprint metrics skewed

**Handling:**
```java
// Track sprint changes
task.isSprintChanged = true;
task.prevSprints.add(oldSprintId);

// Effort stays with original sprint for reporting
// Capacity adjusted in both sprints
oldSprint.usedCapacity -= task.taskEstimate;
newSprint.usedCapacity += task.taskEstimate;
```

---

### Task Estimate After Completion
**Scenario:** Task completed, then estimate changed.

**Validation:**
```java
// In TaskService.validateTaskEstimateByWorkflowTaskStatus()
if (task.isCompleted() && changes.contains("taskEstimate")) {
    throw new ValidationException("Cannot change estimate of completed task");
}
```

**Exception:**
- Allow with `canModifyEstimates` flag
- Allow for correcting data entry errors

---

### Deleted Parent Task
**Scenario:** Parent task deleted, children remain.

**Handling:**
```java
void deleteParentTask(Task parentTask) {
    // Option A: Cascade delete children
    for (Long childId : parentTask.getChildTaskIds()) {
        deleteTask(childId);
    }

    // Option B: Orphan children (promote to regular tasks)
    for (Long childId : parentTask.getChildTaskIds()) {
        Task child = getTask(childId);
        child.parentTaskId = null;
        child.taskTypeId = TASK;  // No longer child
    }
}
```

---

## Meeting Edge Cases

### Meeting-Task References
**Scenario:** Meeting references tasks for discussion.

**Edge Cases:**
1. Referenced task deleted
2. Multiple meetings reference same task
3. Task completed before meeting

**Handling:**
```java
// In Meeting entity
referenceEntityNumber;    // Task number for display
referenceEntityTypeId;    // 6 for Task
referencedMeetingReasonId; // Why referenced

// On task deletion
// Don't remove reference - keep for history
// Mark as "referenced task deleted" in UI
```

---

### Recurring Meeting Conflicts
**Scenario:** Recurring meeting instance conflicts with new meeting.

**Validation:**
```java
// Check for conflicts when generating recurring instances
List<Meeting> existing = meetingRepository.findByAccountIdAndDateTimeRange(
    organizerAccountId,
    instanceStartTime,
    instanceEndTime
);

if (!existing.isEmpty()) {
    // Option A: Skip this instance
    // Option B: Notify organizer of conflict
    // Option C: Create anyway with warning
}
```

---

### Meeting Timezone Handling
**Scenario:** Attendees in different timezones.

**Problem:**
- Meeting set for 10 AM IST
- Attendee in PST sees what time?

**Storage:**
```java
// Store in UTC
meeting.startDateTime = convertToUTC(localTime, organizerTimeZone);

// Display conversion for each attendee
for (Attendee attendee : meeting.getAttendees()) {
    String tz = attendee.getUser().getTimeZone();
    LocalDateTime localTime = convertToLocal(meeting.startDateTime, tz);
    // Display localTime to this attendee
}
```

---

## GitHub Integration Edge Cases

### Branch Priority Sorting
**Scenario:** Multiple branches for same task.

**Problem:** Which branch is "primary" for the task?

**Sorting Logic:**
```java
// Priority order
1. Branches matching task number exactly (feature/DEV-123)
2. Branches containing task number (bugfix/DEV-123-urgent)
3. Most recently created
4. Alphabetically

List<WorkItemGithubBranch> sorted = branches.stream()
    .sorted(Comparator
        .comparing((b) -> !b.getBranchName().contains(taskNumber))
        .thenComparing(WorkItemGithubBranch::getCreatedDateTime, reverseOrder())
        .thenComparing(WorkItemGithubBranch::getBranchName))
    .collect(toList());
```

---

### Token Expiration
**Scenario:** GitHub OAuth token expires.

**Detection:**
```java
try {
    callGitHubAPI(endpoint, token);
} catch (UnauthorizedException e) {
    // Token expired
    githubAccount.isLinked = false;
    // Notify user to re-authenticate
}
```

**Refresh Flow:**
- OAuth tokens don't auto-refresh in current implementation
- User must re-link account

---

### Repository Access Changes
**Scenario:** User loses access to linked repository.

**Problem:**
- Branches linked to tasks
- User can no longer access repo

**Handling:**
```java
// On sync attempt
try {
    branches = callGitHubAPI("/repos/" + owner + "/" + repo + "/branches", token);
} catch (ForbiddenException e) {
    // User lost access
    // Remove mappings for this repo
    branchRepository.deleteByRepositoryOwnerAndRepositoryName(owner, repo);
    // Notify user
}
```

---

## Data Integrity Edge Cases

### Concurrent Updates
**Scenario:** Two users update same task simultaneously.

**Problem:** Lost update if not handled.

**Solution - Optimistic Locking:**
```java
// In Task entity
@Version
private Integer version;

// When saving
try {
    taskRepository.save(task);
} catch (OptimisticLockException e) {
    // Conflict detected
    Task latest = taskRepository.findById(task.getTaskId());
    // Merge changes or notify user
}
```

---

### Orphaned Records
**Scenario:** Parent entity deleted, children remain.

**Examples:**
1. Team deleted → Tasks orphaned
2. Sprint deleted → Tasks without sprint
3. User deactivated → Tasks assigned to inactive user

**Prevention:**
```java
// Before deleting team
long taskCount = taskRepository.countByTeamId(teamId);
if (taskCount > 0) {
    throw new ValidationException("Cannot delete team with " + taskCount + " tasks. Reassign or delete tasks first.");
}
```

---

## Timezone Edge Cases

### Cross-Timezone Scheduling
**Scenario:** User in IST schedules task for user in PST.

**Problem:** "Due by end of day" - whose end of day?

**Convention:**
```java
// Store in UTC
// Display in viewer's timezone
// Treat deadlines in assignee's timezone

// When creating task
if (assigneeTimeZone != creatorTimeZone) {
    // Convert expected end time to assignee's timezone
    // Store the logical end time in assignee's context
}
```

---

### DST (Daylight Saving Time) Transitions
**Scenario:** Meeting scheduled during DST transition.

**Problem:**
- Meeting at 2:30 AM on DST transition day
- Time doesn't exist or exists twice

**Handling:**
```java
// Use ZonedDateTime for scheduling
ZonedDateTime meetingTime = ZonedDateTime.of(
    localDateTime,
    ZoneId.of(timeZone)
);

// Java handles DST automatically
// 2:30 AM might become 3:30 AM or 1:30 AM
```

---

### Year Boundary Operations
**Scenario:** Operations spanning year boundary.

**Edge Cases:**
1. **Leave Dec 28 - Jan 2:**
   - Deducted from which year's balance?
   - Split across years?

2. **Sprint Dec 20 - Jan 10:**
   - Holiday handling across years
   - Annual leave reset mid-sprint

3. **Leave balance reset:**
   ```java
   // If user has approved leave for Jan 1-3
   // And balance resets at midnight Jan 1
   // New balance should account for these approved leaves
   ```

---

## Summary of Critical Validations

| Scenario | Validation Required |
|----------|---------------------|
| Task creation | Team membership, valid assignee |
| Leave application | Balance check, date validation, approver validation |
| Sprint start | No overlapping active sprint |
| Team deletion | No active tasks, no active sprint |
| User deactivation | Reassign pending approvals |
| Geo-fence punch | Location within radius |
| Meeting creation | No conflicts for organizer |
| GitHub link | Valid OAuth token |
| Parent-child task | No circular references |
| Sprint task move | Capacity adjustment both sprints |
