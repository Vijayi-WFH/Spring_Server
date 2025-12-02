# Refactoring Opportunities & Code Quality

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [Naming Inconsistencies](#naming-inconsistencies)
3. [Large Classes Needing Refactoring](#large-classes-needing-refactoring)
4. [Performance Issues](#performance-issues)
5. [Duplicate Code](#duplicate-code)
6. [Missing Best Practices](#missing-best-practices)
7. [Recommendations](#recommendations)
8. [Priority Matrix](#priority-matrix)

---

## Executive Summary

| Priority | Issue | Count | Impact |
|----------|-------|-------|--------|
| **CRITICAL** | TaskServiceImpl size | 9,516 lines | Unmaintainable |
| **CRITICAL** | Missing FETCH JOIN | 5+ queries | N+1 problem |
| **CRITICAL** | User entity duplication | 3 versions | Data inconsistency |
| **HIGH** | No pagination on large queries | 28+ methods | Memory overflow |
| **HIGH** | Loop-based DB operations | 20+ methods | Performance |
| **HIGH** | Excessive autowiring | 50+ deps/service | Testing difficulty |
| **MEDIUM** | Snake_case fields | 3+ classes | Convention violation |
| **MEDIUM** | Duplicate custom models | 70+ classes | Maintenance burden |
| **MEDIUM** | Missing caching | Entity lookups | CPU overhead |
| **LOW** | Commented code | 30+ lines | Code clarity |

---

## Naming Inconsistencies

### 1. Inconsistent Entity Naming (User/Account/UserAccount)

**Problem:** Three separate `User` entity implementations with duplicate fields.

**Files:**
- `TSe_Server/src/main/java/com/tse/core_application/model/User.java` (~230 lines)
- `TSEHR/src/main/java/com/tse/core/model/supplements/User.java` (~130 lines)
- `Vijayi_WFH_Conversation/chat-app/src/main/java/com/example/chat_app/model/User.java` (~75 lines)

**Duplicate Fields Across All Three:**
```java
// Present in all three User classes:
primaryEmail, alternateEmail, personalEmail, currentOrgEmail
firstName, lastName, middleName
isActive, multiAssociation
```

**Impact:**
- Data inconsistency risk
- Maintenance nightmare (changes need replication)
- Confusing for developers

**Recommendation:**
```java
// Option 1: Create shared library module
shared-models/
  └── src/main/java/com/tse/shared/model/User.java

// Option 2: Use composition
public class UserAccount {
    @Embedded
    private UserBasicInfo basicInfo;  // Shared fields
    // Account-specific fields
}
```

### 2. Inconsistent Field Naming (Snake_case vs CamelCase)

**Non-standard Java fields found:**

| File | Field | Should Be |
|------|-------|-----------|
| `TaskHistoryColumnsMapping.java:24` | `task_history_columns_mapping_id` | `taskHistoryColumnsMappingId` |
| `GithubTokenResponse.java:9-10` | `access_token`, `token_type` | Use `@JsonProperty` |
| `TimeSheetResponse.java:23` | `account_Id` | `accountId` |

**Example Fix for GitHub Response:**
```java
// Current (incorrect)
public class GithubTokenResponse {
    private String access_token;  // Snake case in Java
    private String token_type;
}

// Correct
public class GithubTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;
}
```

### 3. Inconsistent DTO Naming Patterns

**Mixed patterns found:**
- `UserDto.java` vs `UserRequest.java`
- `GroupAndUsersDTO.java` vs `GroupAndUserDTO.java` (singular/plural inconsistency)
- `*Response`, `*Dto`, `*DTO`, `*Request` - no standard

**Recommendation:** Adopt consistent naming:
```
Requests:  *Request.java    (e.g., CreateTaskRequest)
Responses: *Response.java   (e.g., TaskDetailResponse)
DTOs:      *Dto.java        (e.g., UserDto)
```

### 4. Conflicting User Imports

**Found in 28+ files:**

```java
// UserService.java - confusing imports
import com.tse.core_application.dto.User;     // Line 9 - DTO
import com.tse.core_application.model.*;       // Line 15 - Entity
import com.tse.core_application.model.User;    // Line 25 - Entity (conflicts!)
```

**Recommendation:** Rename DTO:
```java
// Rename: dto/User.java -> dto/UserDto.java
import com.tse.core_application.dto.UserDto;
import com.tse.core_application.model.User;
```

---

## Large Classes Needing Refactoring

### 1. TaskServiceImpl.java - 9,516 Lines (CRITICAL)

**Path:** `TSe_Server/src/main/java/com/tse/core_application/service/Impl/TaskServiceImpl.java`

**Issues:**
- 73+ public methods
- 50+ injected dependencies
- Violates Single Responsibility Principle
- Extremely difficult to test
- Long compilation times

**Recommended Split:**

| New Service | Responsibility | Methods |
|-------------|----------------|---------|
| `TaskValidationService` | Input validation, business rules | validateTask*, checkPermissions* |
| `TaskQueryService` | Read operations, search | getTask*, findTask*, searchTask* |
| `TaskMutationService` | Create, update, delete | createTask, updateTask, deleteTask |
| `TaskHistoryService` | Audit trail, history | logHistory*, getTaskHistory* |
| `TaskNotificationService` | Notifications | sendTaskNotification*, notifyAssignee* |
| `TaskDependencyService` | Dependencies, relationships | addDependency*, checkCircular* |

**Refactoring Pattern:**
```java
// Before (monolithic)
@Service
public class TaskServiceImpl {
    // 9,516 lines of everything
}

// After (modular)
@Service
public class TaskFacade {
    private final TaskValidationService validationService;
    private final TaskQueryService queryService;
    private final TaskMutationService mutationService;
    private final TaskHistoryService historyService;

    public TaskResponse createTask(CreateTaskRequest request) {
        validationService.validateCreateRequest(request);
        Task task = mutationService.create(request);
        historyService.logCreation(task);
        return TaskMapper.toResponse(task);
    }
}
```

### 2. SprintService.java - 4,008 Lines

**Path:** `TSe_Server/src/main/java/com/tse/core_application/service/Impl/SprintService.java`

**Recommended Split:**
- `SprintPlanningService` - Sprint creation, capacity planning
- `SprintExecutionService` - Task assignment, backlog management
- `SprintReportingService` - Burndown, velocity, metrics
- `SprintClosureService` - Sprint completion, retrospective

### 3. MeetingService.java - 3,871 Lines

**Path:** `TSe_Server/src/main/java/com/tse/core_application/service/Impl/MeetingService.java`

**Recommended Split:**
- `MeetingSchedulingService` - Create, update, cancel
- `MeetingAttendanceService` - RSVP, attendee management
- `MeetingJitsiService` - Video conference integration
- `MeetingNotificationService` - Reminders, invites

### 4. NotificationService.java - 3,518 Lines

**Path:** `TSe_Server/src/main/java/com/tse/core_application/service/Impl/NotificationService.java`

**Recommended Split:**
- `PushNotificationService` - FCM push notifications
- `InAppNotificationService` - In-app notifications
- `EmailNotificationService` - Email sending
- `NotificationPreferenceService` - User preferences

---

## Performance Issues

### 1. N+1 Query Problems (CRITICAL)

**File:** `Vijayi_WFH_Conversation/chat-app/src/main/java/com/example/chat_app/repository/GroupRepository.java`

**Problem Queries:**

```java
// Line 17 - MISSING FETCH JOIN
@Query("SELECT g FROM Group g JOIN g.groupUsers gu JOIN gu.user u WHERE u IN :users")
List<Group> findByUsersIn(@Param("users") List<User> users);
// Issue: Loads groups, then N separate queries for groupUsers and Users

// Lines 25-48 - Multiple similar queries with N+1 potential
```

**Fix:**
```java
// Correct - with FETCH JOIN (like line 21)
@Query("SELECT g FROM Group g " +
       "LEFT JOIN FETCH g.groupUsers gu " +
       "LEFT JOIN FETCH gu.user u " +
       "WHERE u IN :users")
List<Group> findByUsersInWithUsers(@Param("users") List<User> users);
```

### 2. Missing Pagination (HIGH)

**28+ methods** return `List<>` without pagination:

```java
// Problem - loads ALL records
List<Task> findByTeamId(Long teamId);
List<User> findAll();
List<Message> findByGroupId(Long groupId);

// Fix - add Pageable
Page<Task> findByTeamId(Long teamId, Pageable pageable);
```

**Files Affected:**
- `UserAccountService.java` - findAll() loads all users
- `TaskRepository.java` - multiple findBy* methods
- `MessageRepository.java` - message queries without limit

### 3. Loop-Based Database Operations (HIGH)

**Example:** `FenceAssignmentService.java:104-122`

```java
// Problem - N database calls in loop
for (EntityActionItem item : request.getAdd()) {
    EntityResult entityResult = processAdd(item);  // DB call
    results.add(entityResult);
}
for (EntityActionItem item : request.getRemove()) {
    EntityResult entityResult = processRemove(item);  // DB call
    results.add(entityResult);
}
```

**Fix - Batch Operations:**
```java
// Collect all entities first
List<FenceAssignment> toAdd = request.getAdd().stream()
    .map(this::mapToEntity)
    .collect(toList());

List<Long> toRemoveIds = request.getRemove().stream()
    .map(EntityActionItem::getId)
    .collect(toList());

// Batch save/delete
fenceAssignmentRepository.saveAll(toAdd);
fenceAssignmentRepository.deleteAllByIdIn(toRemoveIds);
```

### 4. Missing Caching (MEDIUM)

**0 `@Cacheable` annotations** despite repeated lookups:

**High-cache candidates:**
- User by AccountId (frequently queried)
- Role lookups (static/rarely changing)
- Organization configuration (constant per request)
- Team by TeamId (repeated access)

**Recommendation:**
```java
@Service
public class UserAccountService {

    @Cacheable(value = "users", key = "#accountId")
    public User getUserByAccountId(Long accountId) {
        return userRepository.findByAccountId(accountId);
    }

    @CacheEvict(value = "users", key = "#accountId")
    public void updateUser(Long accountId, UserDto dto) {
        // ...
    }
}
```

**Cache Configuration:**
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "users", "roles", "orgConfig", "teams"
        );
    }
}
```

### 5. Missing Database Indexes

**181 relationship annotations** without explicit index hints:

```java
// Example: User.java line 119-121
@Column(name = "fk_country_id")
private Long fkCountryId;  // Likely queried but no index

// Recommendation: Add index
@Column(name = "fk_country_id")
@Index(name = "idx_user_country")
private Long fkCountryId;
```

**Priority indexes needed:**
- `task.fk_assignee_account_id`
- `task.fk_team_id`
- `task.fk_sprint_id`
- `leave_application.fk_account_id`
- `message.fk_group_id`

---

## Duplicate Code

### 1. Duplicate Custom Models (70+ classes)

**Same classes in multiple modules:**

| Class | TSe_Server | TSEHR | Chat-App |
|-------|-----------|-------|----------|
| `AccountId.java` | Yes | Yes | - |
| `RestResponseWithoutData.java` | Yes | Yes | Yes |
| `User.java` | Yes | Yes | Yes |
| `DateTimeUtils.java` | Yes | Yes | Yes |
| `CommonUtils.java` | Yes | Yes | - |

**Recommendation:** Create shared library module:
```
shared-library/
├── pom.xml
└── src/main/java/com/tse/shared/
    ├── model/
    │   └── RestResponseWithoutData.java
    ├── utils/
    │   ├── DateTimeUtils.java
    │   └── CommonUtils.java
    └── dto/
        └── AccountId.java
```

### 2. Repeated Validation Logic

**Found in multiple services:**
```java
// Pattern repeated 50+ times
if (request.getTitle() == null || request.getTitle().isEmpty()) {
    throw new ValidationFailedException("Title is required");
}
if (request.getTitle().length() > 70) {
    throw new ValidationFailedException("Title too long");
}
```

**Recommendation:** Create validation utilities:
```java
public class ValidationUtils {
    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationFailedException(fieldName + " is required");
        }
    }

    public static void requireLength(String value, int min, int max, String fieldName) {
        if (value.length() < min || value.length() > max) {
            throw new ValidationFailedException(
                fieldName + " must be between " + min + " and " + max + " characters"
            );
        }
    }
}
```

### 3. Commented Code

**30+ lines of commented code in User.java:**

```java
// Lines 134-162 - Commented toString() method
// @Override
// public String toString() {
//     return "User{" +
//         "userId=" + userId +
//         ...
// }
```

**Action:** Remove commented code - use version control for history.

---

## Missing Best Practices

### 1. Excessive Dependency Injection

**TaskServiceImpl has 50+ @Autowired fields:**

```java
@Service
public class TaskServiceImpl {
    @Autowired private TaskRepository taskRepository;
    @Autowired private UserAccountService userAccountService;
    @Autowired private TeamService teamService;
    @Autowired private SprintService sprintService;
    @Autowired private NotificationService notificationService;
    // ... 45+ more
}
```

**Issues:**
- Violates Single Responsibility Principle
- Makes unit testing extremely difficult
- Hidden dependencies
- Circular dependency risk

**Recommendation:** Use constructor injection + Facade pattern:
```java
@Service
@RequiredArgsConstructor
public class TaskServiceImpl {
    private final TaskRepository taskRepository;
    private final TaskDependencies dependencies;  // Facade for related services
}

@Component
@RequiredArgsConstructor
public class TaskDependencies {
    private final UserAccountService userAccountService;
    private final TeamService teamService;
    private final SprintService sprintService;
    // Group related dependencies
}
```

### 2. Missing Transaction Management

**Large methods without proper transaction boundaries:**

```java
// Problem - multiple DB operations without transaction
public void complexOperation() {
    taskRepository.save(task1);    // Commit 1
    sprintRepository.save(sprint); // Commit 2
    // If this fails, task1 is already committed!
    notificationService.send(...); // External call
}

// Fix - proper transaction management
@Transactional
public void complexOperation() {
    taskRepository.save(task1);
    sprintRepository.save(sprint);
    // External calls should be outside transaction or use events
}
```

### 3. Missing Input Validation Annotations

**DTOs lack validation annotations:**

```java
// Current - manual validation in service
public class CreateTaskRequest {
    private String title;
    private String description;
}

// Recommended - use Bean Validation
public class CreateTaskRequest {
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 70, message = "Title must be 3-70 characters")
    private String title;

    @Size(max = 5000, message = "Description must be under 5000 characters")
    private String description;
}
```

---

## Recommendations

### Immediate Actions (Week 1-2)

1. **Fix N+1 queries** in GroupRepository
2. **Add pagination** to high-volume queries
3. **Remove commented code** across codebase
4. **Fix snake_case fields** with @JsonProperty

### Short-Term (Month 1)

1. **Create shared library module** for common code
2. **Split TaskServiceImpl** into focused services
3. **Add caching** for frequently accessed entities
4. **Implement batch operations** replacing loops

### Medium-Term (Quarter 1)

1. **Refactor all large services** (Sprint, Meeting, Notification)
2. **Add database indexes** for foreign keys
3. **Implement proper transaction management**
4. **Add Bean Validation** to all DTOs

### Long-Term (Quarter 2-3)

1. **Consolidate User entity** across modules
2. **Standardize naming conventions** project-wide
3. **Implement CQRS pattern** for complex queries
4. **Add comprehensive caching strategy**

---

## Priority Matrix

```
                    IMPACT
                    High    │    Low
               ┌────────────┼────────────┐
        High   │ CRITICAL   │   MEDIUM   │
               │            │            │
    EFFORT     │ • Split    │ • Naming   │
               │   Services │   fixes    │
               │ • N+1 fix  │ • Comments │
               ├────────────┼────────────┤
        Low    │   HIGH     │    LOW     │
               │            │            │
               │ • Caching  │ • Style    │
               │ • Pagination│  updates  │
               │ • Batch ops│            │
               └────────────┴────────────┘
```

### Action Priority

| Priority | Issue | Effort | Impact |
|----------|-------|--------|--------|
| 1 | Fix N+1 queries | Low | Critical |
| 2 | Add pagination | Low | High |
| 3 | Batch DB operations | Low | High |
| 4 | Add caching | Medium | High |
| 5 | Split TaskServiceImpl | High | Critical |
| 6 | Fix naming inconsistencies | Low | Medium |
| 7 | Create shared library | Medium | Medium |
| 8 | Add Bean Validation | Medium | Medium |
| 9 | Add database indexes | Low | Medium |
| 10 | Consolidate User entity | High | Medium |
