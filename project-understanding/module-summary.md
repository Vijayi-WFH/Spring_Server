# Module-by-Module Deep Analysis

## Table of Contents
1. [TSe_Server (Core Application)](#tse_server-core-application)
2. [TSEHR (Timesheet/HR Module)](#tsehr-timesheethr-module)
3. [Notification_Reminders (Scheduler Module)](#notification_reminders-scheduler-module)
4. [Vijayi_WFH_Conversation (Chat Module)](#vijayi_wfh_conversation-chat-module)

---

## TSe_Server (Core Application)

### Overview
| Property | Value |
|----------|-------|
| **Port** | 8080 |
| **Context Path** | `/api` |
| **Spring Boot Version** | 2.4.5 |
| **Java Version** | 11 |
| **File Count** | 1,116 Java files |
| **Artifact ID** | `core_application` |
| **Group ID** | `com.tse` |

### Responsibilities
- User authentication and authorization (JWT/OAuth2)
- Organization, BU, Project, Team hierarchy management
- Task, Epic, Sprint lifecycle management
- Meeting scheduling and management
- Leave application and approval workflow
- Notification generation and delivery
- Geo-fencing and attendance tracking
- GitHub integration for source control
- Performance notes and ratings
- Personal task management
- File attachment handling
- Email notifications via SMTP

### Internal Components

#### Controllers (50+ controllers)
| Controller | Base Path | Key Responsibilities |
|------------|-----------|---------------------|
| `TasksController` | `/task` | Task CRUD, comments, attachments, history |
| `MeetingController` | `/meeting` | Meeting CRUD, attendees, action items |
| `LeaveController` | `/leave` | Leave applications, approvals, balance |
| `SprintController` | `/sprint` | Sprint lifecycle, capacity planning |
| `EpicController` | `/epic` | Epic management, task linking |
| `UserController` | `/user` | User profile management |
| `OrganizationController` | `/organization` | Org settings, quotas |
| `TeamController` | `/team` | Team CRUD, member management |
| `ProjectController` | `/project` | Project CRUD within BUs |
| `NotificationController` | `/notification` | Notification retrieval, marking read |
| `AuthController` | `/auth` | Login, registration, token management |
| `GithubController` | `/github` | GitHub OAuth, branch linking |
| `PerfNoteController` | `/perfNote` | Performance notes CRUD |
| `AttendanceController` | `/attendance` | Geo-fencing attendance |
| `StatsController` | `/stats` | Task progress statistics |

#### Services (90+ implementations)
| Service | Lines | Key Methods |
|---------|-------|-------------|
| `TaskServiceImpl` | 9,516 | `addTaskInTaskTable()`, `updateFieldsInTaskTable()`, `deleteTaskByTaskId()` |
| `MeetingService` | ~2,000 | `createMeeting()`, `addAttendees()`, `generateMeetingMinutes()` |
| `LeaveService` | ~1,500 | `applyLeave()`, `approveLeave()`, `calculateLeaveDays()` |
| `SprintService` | ~1,200 | `createSprint()`, `startSprint()`, `completeSprint()` |
| `NotificationService` | ~800 | `createNotification()`, `sendNotification()` |
| `UserService` | ~600 | `createUser()`, `updateUser()`, `deactivateUser()` |
| `GithubService` | ~500 | `linkGithubAccount()`, `syncBranches()` |
| `FCMService` | ~400 | Firebase push notification delivery |

#### Entities (100+ models)
**Core Entities:**
- `Task` (28,071 lines) - Work items with 100+ fields
- `TaskHistory` (14,235 lines) - Task audit trail
- `User` - User profiles with encrypted fields
- `UserAccount` - Account-organization mapping
- `Organization` - Organization details
- `Meeting` - Meeting with attendees and action items
- `Sprint` - Sprint with capacity tracking
- `Epic` - Epic containers for tasks
- `LeaveApplication` - Leave requests

**Supporting Entities:**
- `Comment`, `Note`, `NoteHistory`
- `Attendee`, `ActionItem`, `MeetingNote`
- `Label`, `Priority`, `Severity`, `Resolution`
- `WorkFlowTaskStatus`, `WorkFlowEpicStatus`
- `GeoFence`, `AttendanceDay`, `AttendanceEvent`
- `GithubAccount`, `WorkItemGithubBranch`

#### Repositories (100+ interfaces)
All repositories extend `JpaRepository` with custom query methods:
- Native SQL queries for complex joins
- `@Query` annotations for JPQL
- Specification executors for dynamic filtering
- Projections for optimized data fetching

### Key Business Flows

#### Task Lifecycle
```
Created → Not Started → In Progress → Review → Done
              ↓              ↓           ↓
           Blocked       Blocked     Blocked
```

#### Sprint Lifecycle
```
Created → Started → Completed
    ↓
 Extended (if needed)
```

#### Leave Workflow
```
Applied → Waiting Approval → Approved/Rejected
                   ↓
             Waiting Cancel → Cancelled
```

### External Dependencies
| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-data-jpa` | Database ORM |
| `spring-boot-starter-security` | Authentication |
| `spring-boot-starter-websocket` | WebSocket support |
| `spring-boot-starter-mail` | Email notifications |
| `spring-boot-starter-data-redis` | Token caching |
| `firebase-admin` | Push notifications |
| `google-api-client` | OAuth2 validation |
| `jjwt` | JWT token handling |
| `jasypt-spring-boot-starter` | Property encryption |
| `springdoc-openapi-ui` | API documentation |
| `postgresql` | Database driver |
| `lombok` | Boilerplate reduction |
| `hibernate-types-52` | JSON column support |
| `apache-poi` | Excel generation |
| `quartz-scheduler` | Job scheduling |

---

## TSEHR (Timesheet/HR Module)

### Overview
| Property | Value |
|----------|-------|
| **Port** | 8081 |
| **Spring Boot Version** | 2.7.6 |
| **Java Version** | 11 |
| **File Count** | 114 Java files |
| **Artifact ID** | `Timesheet_Demo` |
| **Group ID** | `com.tse.core` |

### Responsibilities
- Timesheet generation and reporting
- Leave policy management (org-level)
- Leave balance calculations
- Pro-rata leave allocations for mid-year joiners
- Leave carry-forward logic
- Negative leave balance handling
- Leave reports (individual and organizational)

### Internal Components

#### Controllers (2 controllers)
| Controller | Base Path | Endpoints |
|------------|-----------|-----------|
| `LeaveController` | `/leave` | 20+ endpoints for policy and applications |
| `TimeSheetController` | `/getts` | Timesheet generation with filters |

**LeaveController Key Endpoints:**
- `POST /leave/defaultLeavePolicyAssignment` - Assign default policy to new user
- `POST /leave/addLeavePolicy` - Create new policy
- `POST /leave/getOrgLeavePolicy` - Get organization policies
- `POST /leave/leaveApplication` - Submit leave application
- `POST /leave/changeLeaveApplicationStatus` - Approve/reject leave
- `POST /leave/getLeaveRemaining` - Get leave balance
- `POST /leave/getAllLeavesByFilter` - Reporting

**TimeSheetController Key Endpoint:**
- `POST /getts` - Get timesheet with hierarchical filtering (org/BU/project/team)

#### Services (3 services)
| Service | Lines | Purpose |
|---------|-------|---------|
| `LeaveService` | 2,151 | Leave policy and application management |
| `TimeSheetService` | ~900 | Timesheet generation and calculations |
| `EntityPreferenceService` | ~100 | Entity settings (off-days, office hours) |

**LeaveService Key Methods:**
```java
// Policy Management
defaultLeavePolicyAssignment(accountId, orgId, isNewOrg)
createLeavePolicyForLeaveType(orgId)
addLeavePolicy(LeavePolicyRequest)
updateLeavePolicy(request, policyId)
assignLeavePolicyToUsers(request)

// Application Management
validateLeaveApplication(request)
saveLeaveApplication(request, timeZone)
changeLeaveApplicationStatus(request, timeZone)
getLeaveApplications(filterRequest, pageable, timeZone)

// Balance Calculations
calculateLeaveDays(fromDate, toDate, options)
getLeaveRemaining(accountId, timeZone)
```

#### Entities (23 models)
**Leave Entities:**
- `LeaveApplication` - Leave requests with doctor certificate support
- `LeavePolicy` - Organization policies with carry-forward settings
- `LeaveRemaining` - Balance tracking per user per policy
- `LeaveType` - Leave type definitions (Time Off, Sick Leave)
- `LeaveApplicationStatus` - Status enum values

**Supplement Entities (shared from TSe_Server):**
- `User`, `UserAccount`, `Organization`
- `BU`, `Project`, `Team`, `Task`
- `Role`, `HolidayOffDay`, `EntityPreference`
- `CalendarDays` - Date dimension table

**Timesheet Entity:**
- `TimeSheet` - Effort entries with entity references

#### Repositories (20+ interfaces)
| Repository | Key Queries |
|------------|-------------|
| `LeaveApplicationRepository` | Date range queries, status filtering, expanded reports |
| `LeavePolicyRepository` | Policy lookup by org/BU/project/team |
| `LeaveRemainingRepository` | Balance by account and type |
| `TimeSheetRepository` | Hierarchical effort queries |
| `EntityPreferenceRepository` | Settings by entity type and ID |

### Key Business Flows

#### Leave Policy Assignment (New User)
```java
1. Check if new organization
2. If new org: Create default policies for all leave types
3. Calculate pro-rata leaves based on join date
4. Create LeaveRemaining records with calculated balance
5. Mark as currentlyActive
```

#### Leave Balance Calculation
```
leaveRemaining = initialLeaves
                 - leaveTaken
                 - plannedLeaves (approved but not consumed)

If isNegativeLeaveAllowed:
    Min Balance = -maxNegativeLeaves
Else:
    Min Balance = 0
```

#### Pro-Rata Calculation (Mid-Year Joiner)
```
Join Date: July 15, 2025
Annual Allocation: 12 days
Months Remaining: 6 (July-December)

If !isMonthlyLeaveUpdateOnProRata:
    Pro-rata = 12 * (6/12) = 6 days
Else:
    Pro-rata = 0 (monthly accrual will add)
```

### External Dependencies
| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-data-jpa` | Database ORM |
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-validation` | Input validation |
| `postgresql` | Database driver |
| `lombok` | Boilerplate reduction |
| `hibernate-types-52` | JSON columns |
| `log4j2` | Logging |

---

## Notification_Reminders (Scheduler Module)

### Overview
| Property | Value |
|----------|-------|
| **Port** | 8081 |
| **Spring Boot Version** | 2.7.9 |
| **Java Version** | 11 |
| **File Count** | 3 Java files |
| **Artifact ID** | `tse_scheduler` |
| **Group ID** | `com.example` |

### Responsibilities
- Meeting reminder notifications
- Timesheet submission reminders
- Leave balance resets (annual)
- Leave status updates (consumed, expired)
- Old notification cleanup
- Blocked task reminders
- Dependency alerts
- Geo-fencing auto-checkout
- Sprint task email distribution
- AI registration retries

### Internal Components

#### Main Application
```java
@SpringBootApplication
@EnableScheduling
public class WfhSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WfhSchedulerApplication.class, args);
    }
}
```

#### Scheduler Class (22 scheduled methods)
| Method | Schedule | Endpoint Called |
|--------|----------|-----------------|
| `meetingReminderScheduler` | Every 60s | `/meeting/reminder` |
| `timesheetPreReminderScheduler` | Every 60s | `/timesheet/timesheetPreReminder` |
| `timesheetPostReminderScheduler` | Every 60s | `/timesheet/timesheetPostReminder` |
| `timesheetBeforeOfficeReminderScheduler` | Every 60s | `/timesheet/timesheetBeforeOfficeReminder` |
| `leaveRemainingReset` | Jan 1st midnight | `/leave/leaveRemainingReset` |
| `startMeetingConfirmation` | Every 60s | `/meeting/startMeetingConfirmation` |
| `endMeetingConfirmation` | Every 60s | `/meeting/endMeetingConfirmation` |
| `deleteOldNotificationsScheduler` | Daily midnight | `/notification/deleteOldNotifications` |
| `blockedTaskReminderScheduler` | Daily 11 AM | `/blockedTask/blockedTaskReminder` |
| `holidaysTimesheetScheduler` | Sunday 12 PM | `/timesheet/fillHolidaysTimesheet` |
| `leaveRemainingMonthlyUpdate` | Last day 23:58:59 | `/leave/leaveRemainingMonthlyUpdate` |
| `dependencyAlertSchedular` | Daily 11 AM | `/alert/dependencyAlert` |
| `userReminderScheduler` | Every 60s | `/reminder/userReminder` |
| `expireLeaveApplicationsSchedular` | Daily 23:58:59 | `/leave/expireLeaveApplications` |
| `leaveApprovalReminderSchedular` | Every 60s | `/leave/sendLeaveApprovalReminder` |
| `changeLeaveStatusToConsumed` | Daily 3 AM | `/leave/changeLeaveStatusToConsumed` |
| `sendSprintTasksMail` | Every 60 min | `/sprint/sendSprintTasksMail` |
| `notifyBeforeShiftStartScheduler` | Every minute | `/geo-fencing/notifyBeforeShiftStart` |
| `autoCheckoutScheduler` | Every minute | `/geo-fencing/autoCheckout` |
| `missedPunchScheduler` | Every minute | `/geo-fencing/missedPunch` |
| `retryFailedAiRegistration` | Every 30 min | `/ai/retryFailedUserRegistration` |
| `deleteAlerts` | Daily 23:50:59 | `/alert/deleteAlerts` |

#### Constants
```java
// Meeting
public static final String meetingRoot = "/meeting";
public static final String reminder = "/reminder";

// Leave
public static final String leaveRoot = "/leave";
public static final String getLeaveRemainingReset = "/leaveRemainingReset";
public static final String getLeaveRemainingMonthlyUpdate = "/leaveRemainingMonthlyUpdate";

// Timesheet
public static final String timesheetRoot = "/timesheet";
public static final String timesheetPreReminder = "/timesheetPreReminder";

// Geo-fencing
public static final String geoFenceRoute = "/geo-fencing";
public static final String notifyBeforeShiftStart = "/notifyBeforeShiftStart";
public static final String autoCheckout = "/autoCheckout";

// ... 31 total constants
```

### Communication Pattern
```
┌───────────────────────┐       HTTP POST        ┌───────────────────────┐
│                       │ ────────────────────>  │                       │
│  Notification_        │                        │     TSe_Server        │
│  Reminders            │                        │     (Port 8080)       │
│  (Port 8081)          │ <────────────────────  │                       │
│                       │    String Response     │                       │
└───────────────────────┘                        └───────────────────────┘

Base URL: http://localhost:8080/api/schedule
```

### Implementation Pattern
```java
@Scheduled(fixedRate = 60000)
public void meetingReminderScheduler() {
    try {
        logger.info("Meeting reminder scheduler started at " + LocalDateTime.now());
        RestTemplate restTemplate = new RestTemplate();
        String uri = rootPath + Constants.meetingRoot + Constants.reminder;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(uri,
            HttpMethod.POST, requestEntity,
            new ParameterizedTypeReference<String>() {});
        logger.info("Scheduler completed at " + LocalDateTime.now()
            + " with response " + response);
    } catch (Exception e) {
        logger.error(LocalDateTime.now() + ". Caught error: " + e);
    }
}
```

### External Dependencies
| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-web` | HTTP client |
| `log4j2` | Logging |
| `awaitility` | Async testing |

---

## Vijayi_WFH_Conversation (Chat Module)

### Overview
| Property | Value |
|----------|-------|
| **Port** | 8082 |
| **Spring Boot Version** | 2.4.5 |
| **Java Version** | 11 |
| **File Count** | 108 Java files |
| **Artifact ID** | `chat-app` |
| **Group ID** | `com.example` |
| **Database** | chatapp (PostgreSQL) |

### Responsibilities
- Real-time WebSocket messaging
- Direct messages (DM) and group chats
- Group creation and management
- File attachment handling
- Read receipts and delivery status (tick system)
- Typing indicators
- User online/offline status
- Pinned and favourite chats
- Message reactions (emoji)
- Message editing and deletion
- User join/leave tracking in groups

### Internal Components

#### Controllers (6 controllers)
| Controller | Base Path | Protocol |
|------------|-----------|----------|
| `WebSocketController` | `/chat` | WebSocket |
| `ChatController` | `/api/chats` | REST |
| `MessageController` | `/api/message` | REST |
| `GroupController` | `/api/groups` | REST |
| `UserController` | `/api/users` | REST |
| `MessageAttachmentController` | `/api/attachments` | REST |

**WebSocket Actions Handled:**
- `MESSAGE_SENT` - Send new message
- `DELIVERY_ACK` - Mark as delivered
- `READ_ACK` - Mark as read
- `TYPING_INDICATOR` - Show typing status
- `ACTIVITY_STATUS` - Online/offline status
- `REACTION` - Add emoji reaction
- `EDIT_MESSAGE` - Edit existing message
- `DELETE_MESSAGE` - Soft delete message

#### Services (6 services)
| Service | Purpose |
|---------|---------|
| `ChatService` | Chat list retrieval, pin/favourite |
| `MessageService` | Message CRUD, pagination |
| `GroupService` | Group CRUD, member management |
| `UserService` | User CRUD, activation |
| `MessageAttachmentService` | File upload/download |
| `FCMClient` | Push notifications to TSe_Server |

#### Entities (15 models)
**Core Entities:**
- `User` - Chat user profiles
- `Message` - Messages with DM/group support
- `Group` - Chat groups with icon/color
- `GroupUser` - Group membership (admin flag)
- `MessageUser` - Read receipts per user
- `MessageAttachment` - File storage as LOB
- `MessageStats` - Aggregated tick counts
- `UserGroupEvent` - Join/leave audit trail

**Composite Keys:**
- `GroupUserId` - (groupId, accountId)
- `MessageUserId` - (messageId, accountId)

**Supporting Entities:**
- `PinnedChats`, `FavouriteChats`
- `Tag`, `History`, `HistoryTag`

#### Repositories (15 interfaces)
| Repository | Key Features |
|------------|--------------|
| `UserRepository` | User lookup, chat response queries |
| `MessageRepository` | Cursor-based pagination, interval queries |
| `GroupRepository` | Group with users fetch |
| `GroupUserRepository` | Bulk admin/delete operations |
| `MessageUserRepository` | Bulk delivery/read updates |
| `MessageAttachmentRepository` | File metadata queries |
| `MessageStatsRepository` | Increment and fetch counts |
| `UserGroupEventRepository` | Join/leave history |

### Key Business Flows

#### Message Sending Flow
```
User ──WebSocket──> WebSocketController
         │
         ▼
    Parse MESSAGE_SENT action
         │
         ▼
    Create Message entity
         │
         ▼
    Create MessageUser for each recipient
         │
         ▼
    Create/Update MessageStats
         │
         ▼
    FCMClient ──HTTP──> TSe_Server ──> Firebase
         │
         ▼
    Broadcast to active sessions
```

#### Read Receipt Flow (Tick System)
```
Single Tick (✓):     Message sent
Double Tick (✓✓):    All recipients received
Blue Double Tick (✓✓): All recipients read

Implementation:
- MessageStats tracks deliveredCount, readCount, groupSize
- DeliveryAckScheduler batches updates every 2 seconds
- Tick status calculated: deliveredCount == groupSize ? DOUBLE : SINGLE
```

#### User Join/Leave Tracking
```
When user joins group:
  1. Create GroupUser with isDeleted=false
  2. Create UserGroupEvent with eventType=JOIN

When user leaves/removed:
  1. Update GroupUser.isDeleted=true
  2. Create UserGroupEvent with eventType=LEAVE

When fetching messages:
  1. Find user's JOIN/LEAVE events
  2. Filter messages visible in join intervals
  3. Use window function for complex interval queries
```

### WebSocket Configuration
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketController, "/chat")
                .setAllowedOrigins("*")
                .addInterceptors(new WebSocketHandshakeInterceptor(jwtUtil, userRepository));
    }
}
```

### JWT Validation (via TSe_Server)
```java
// JWTUtil.validateTokenAndAccountIds()
1. Extract Bearer token
2. Call TSe_Server: GET /api/auth/validateTokenAccount
3. Pass headers: Authorization, screenName, accountIds, token
4. Return true if 200 OK, throw exception otherwise
```

### External Dependencies
| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-data-jpa` | Database ORM |
| `spring-boot-starter-websocket` | WebSocket support |
| `spring-boot-starter-security` | Security filters |
| `postgresql` | Database driver |
| `lombok` | Boilerplate reduction |
| `jackson-databind` | JSON serialization |
| `jsoup` | HTML parsing |
| `springfox-swagger-ui` | API documentation |
| `log4j2` | Logging |

### Configuration Properties
```properties
server.port=8082
tseserver.application.root.path=http://localhost:8080
updateMessageTime=15
pinnedChatsLimit=5
scanfile.endpoint=http://85.25.119.59:8080/upload
chat.file.extensions=.avi,.csv,.doc,.jpg,.png,.pdf,...
spring.servlet.multipart.max-file-size=55MB
```

---

## Module Comparison Summary

| Feature | TSe_Server | TSEHR | Notification_Reminders | Chat-App |
|---------|------------|-------|------------------------|----------|
| **Port** | 8080 | 8081 | 8081 | 8082 |
| **File Count** | 1,116 | 114 | 3 | 108 |
| **Database** | db_tse | db_tse | None | chatapp |
| **Spring Boot** | 2.4.5 | 2.7.6 | 2.7.9 | 2.4.5 |
| **Protocol** | REST | REST | HTTP Client | REST + WebSocket |
| **Auth** | JWT Provider | None (internal) | None | JWT Consumer |
| **Caching** | Redis | None | None | None |
| **Push Notif** | Firebase | None | None | Via TSe_Server |
| **Scheduling** | Quartz | None | Spring Scheduler | Batch (2s) |
