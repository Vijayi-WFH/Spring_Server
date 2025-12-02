# AI-Ready Coding Guidelines

These guidelines ensure consistency across the codebase and help AI assistants generate code that matches existing patterns.

## Table of Contents
1. [Naming Conventions](#naming-conventions)
2. [DTO Design Style](#dto-design-style)
3. [Entity Design](#entity-design)
4. [Repository Patterns](#repository-patterns)
5. [Service Layer Patterns](#service-layer-patterns)
6. [Controller Patterns](#controller-patterns)
7. [Exception Handling](#exception-handling)
8. [SQL & Query Writing](#sql--query-writing)
9. [Sorting & Filtering Logic](#sorting--filtering-logic)
10. [Lombok Usage](#lombok-usage)
11. [Code Formatting](#code-formatting)
12. [Security Patterns](#security-patterns)
13. [Testing Patterns](#testing-patterns)

---

## Naming Conventions

### Package Naming
```
com.tse.core_application           # TSe_Server root
com.tse.core                       # TSEHR root
com.example.chat_app               # Chat-App root

# Sub-packages
.controller                        # REST controllers
.service                          # Service interfaces
.service.Impl                     # Service implementations
.repository                       # JPA repositories
.model                            # JPA entities
.dto                              # Data Transfer Objects
.exception                        # Custom exceptions
.config                           # Configuration classes
.utils                            # Utility classes
.constants                        # Constants
.filters                          # Request filters
.handlers                         # Exception handlers
.validators                       # Custom validators
```

### Class Naming
```java
// Entities - singular nouns
Task, User, Meeting, Sprint, LeaveApplication

// Repositories - Entity + Repository
TaskRepository, UserRepository, MeetingRepository

// Services - Interface + Impl
public interface TaskService {}
public class TaskServiceImpl implements TaskService {}

// Controllers - Entity + Controller
TasksController, UserController, MeetingController

// DTOs - Purpose + Request/Response
TaskCreateRequest, TaskUpdateRequest, TaskResponse
LeaveApplicationRequest, LeaveApplicationResponse

// Exceptions - Description + Exception
ValidationFailedException, UnauthorizedException, TokenExpiredException
```

### Method Naming
```java
// CRUD operations
createTask(), updateTask(), deleteTask()
saveEntity(), findById(), existsById()

// Query methods
findByTeamId(), findByAccountIdAndStatus()
getTasksBySprintId(), getAllActiveUsers()

// Business logic
calculateLeaveDays(), validateLeaveApplication()
computeSprintCapacity(), processNotification()

// Boolean checks
isTaskCompleted(), hasPermission(), canModifyEstimate()

// Conversion
convertToDTO(), mapToEntity(), transformResponse()
```

### Variable Naming
```java
// Entity IDs
Long taskId, Long userId, Long accountId

// Collections
List<Task> tasks, Set<Long> accountIds
Map<String, Object> payload

// Boolean flags
boolean isActive, boolean isDeleted, boolean hasPermission

// Date/Time
LocalDate fromDate, LocalDate toDate
LocalDateTime createdDateTime, LocalDateTime lastUpdatedDateTime

// Request/Response
TaskRequest request, TaskResponse response
```

### Database Column Naming
```sql
-- Primary keys
task_id, user_id, account_id

-- Foreign keys
fk_team_id, fk_account_id, fk_org_id

-- Boolean columns
is_active, is_deleted, is_verified

-- Timestamps
created_date_time, last_updated_date_time

-- Encrypted columns
-- Same naming, marked with @Convert annotation
```

---

## DTO Design Style

### Request DTOs
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {
    // Required fields with validation
    @NotNull(message = "Task title is required")
    @Size(min = 3, max = 70, message = "Title must be 3-70 characters")
    private String taskTitle;

    @NotNull(message = "Task description is required")
    @Size(min = 3, max = 5000)
    private String taskDesc;

    @NotNull
    private Long teamId;

    // Optional fields without validation
    private Long assigneeAccountId;
    private String taskPriority;
    private Integer taskEstimate;
    private Long sprintId;
    private List<String> labels;

    // Nested object for complex references
    private EntityReference fkTeamId;
}
```

### Response DTOs
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Long taskId;
    private String taskNumber;
    private String taskTitle;
    private String taskDesc;

    // Nested DTOs for related entities
    private UserDTO assignee;
    private TeamDTO team;
    private List<LabelDTO> labels;

    // Timestamps as strings (timezone-converted)
    private String createdDateTime;
    private String lastUpdatedDateTime;

    // Builder method for conversion
    public static TaskResponse fromEntity(Task task, String timeZone) {
        return TaskResponse.builder()
            .taskId(task.getTaskId())
            .taskNumber(task.getTaskNumber())
            .taskTitle(task.getTaskTitle())
            .assignee(UserDTO.fromEntity(task.getFkAccountId()))
            .createdDateTime(DateTimeUtils.convertToUserTimezone(
                task.getCreatedDateTime(), timeZone))
            .build();
    }
}
```

### Entity Reference DTO
```java
// For referencing entities in requests
@Data
public class EntityReference {
    private Long id;

    // Convenience constructor
    public EntityReference(Long id) {
        this.id = id;
    }
}

// Usage in request
{
    "fkTeamId": { "teamId": 1 },
    "fkAccountId": { "accountId": 2 }
}
```

---

## Entity Design

### Standard Entity Pattern
```java
@Entity
@Table(name = "task", schema = "tse")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    // Required fields
    @Column(name = "task_number", nullable = false, length = 40)
    private String taskNumber;

    // Encrypted fields
    @Column(name = "task_title", nullable = false, length = 70)
    @Convert(converter = DataEncryptionConverter.class)
    private String taskTitle;

    // Validation
    @Size(min = 3, max = 70, message = "Title must be 3-70 characters")
    private String taskTitle;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_team_id", nullable = false)
    private Team fkTeamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_account_id", nullable = false)
    private UserAccount fkAccountId;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<Comment> comments;

    @ManyToMany
    @JoinTable(
        name = "task_label",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private List<Label> labels;

    // JSON columns
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private List<Long> childTaskIds;

    // Audit timestamps
    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time")
    private LocalDateTime lastUpdatedDateTime;

    // Optimistic locking
    @Version
    private Integer version;
}
```

### Enum Usage
```java
// Define as enum
public enum StatType {
    DELAYED(0),
    WATCHLIST(1),
    ONTRACK(2),
    NOTSTARTED(3),
    LATE_COMPLETION(4),
    COMPLETED(5);

    private final int value;

    StatType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

// Use in entity
@Column(name = "task_progress_system")
@Enumerated(EnumType.STRING)
private StatType taskProgressSystem;
```

---

## Repository Patterns

### Standard Repository
```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>,
                                        JpaSpecificationExecutor<Task> {

    // Simple finders
    Task findByTaskId(Long taskId);
    List<Task> findByFkTeamIdTeamId(Long teamId);
    boolean existsByTaskNumber(String taskNumber);

    // Multiple conditions
    List<Task> findByFkAccountIdAccountIdAndFkWorkflowTaskStatusWorkFlowTaskStatusIdIn(
        Long accountId, List<Integer> statusIds);

    // Custom JPQL query
    @Query("SELECT t FROM Task t WHERE t.sprintId = :sprintId " +
           "AND t.fkWorkflowTaskStatus.workFlowTaskStatusId IN :statusIds")
    List<Task> findBySprintAndStatuses(
        @Param("sprintId") Long sprintId,
        @Param("statusIds") List<Integer> statusIds);

    // Native SQL query
    @Query(value = "SELECT * FROM tse.task t " +
                   "WHERE t.fk_team_id = :teamId " +
                   "AND t.task_exp_end_date < NOW() " +
                   "ORDER BY t.task_exp_end_date ASC",
           nativeQuery = true)
    List<Task> findOverdueTasks(@Param("teamId") Long teamId);

    // Modifying query
    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.sprintId = :newSprintId " +
           "WHERE t.sprintId = :oldSprintId")
    int moveTasksToSprint(
        @Param("oldSprintId") Long oldSprintId,
        @Param("newSprintId") Long newSprintId);

    // Projection query
    @Query("SELECT t.taskId, t.taskNumber, t.taskTitle FROM Task t " +
           "WHERE t.fkTeamId.teamId = :teamId")
    List<Object[]> findTaskSummaries(@Param("teamId") Long teamId);
}
```

### Custom Repository Implementation
```java
// Interface
public interface TaskCustomInterface {
    Page<Task> findTasksWithFilters(TaskFilterRequest filter, Pageable pageable);
}

// Implementation
@Repository
public class TaskCustomInterfaceImpl implements TaskCustomInterface {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Task> findTasksWithFilters(TaskFilterRequest filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Task> query = cb.createQuery(Task.class);
        Root<Task> root = query.from(Task.class);

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getTeamIds() != null && !filter.getTeamIds().isEmpty()) {
            predicates.add(root.get("fkTeamId").get("teamId").in(filter.getTeamIds()));
        }

        if (filter.getAssigneeId() != null) {
            predicates.add(cb.equal(root.get("fkAccountId").get("accountId"),
                                    filter.getAssigneeId()));
        }

        query.where(predicates.toArray(new Predicate[0]));

        // Execute query with pagination
        TypedQuery<Task> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Task> results = typedQuery.getResultList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        countQuery.select(cb.count(countQuery.from(Task.class)));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long count = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, count);
    }
}
```

---

## Service Layer Patterns

### Service Interface
```java
public interface TaskService {
    Task createTask(TaskCreateRequest request, Long creatorAccountId, String timeZone);
    Task updateTask(Long taskId, TaskUpdateRequest request, Long updaterAccountId, String timeZone);
    void deleteTask(Long taskId, Long deleterAccountId, String reason);
    Task getTaskById(Long taskId);
    Page<TaskResponse> getTasks(TaskFilterRequest filter, Pageable pageable, String timeZone);
}
```

### Service Implementation
```java
@Service
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    // Constructor injection (preferred over @Autowired)
    public TaskServiceImpl(TaskRepository taskRepository,
                           TeamRepository teamRepository,
                           UserAccountRepository userAccountRepository,
                           NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.teamRepository = teamRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Task createTask(TaskCreateRequest request, Long creatorAccountId, String timeZone) {
        log.info("Creating task for team {} by account {}",
                 request.getTeamId(), creatorAccountId);

        // 1. Validate
        validateTaskRequest(request);
        Team team = getTeamOrThrow(request.getTeamId());
        UserAccount assignee = getAccountOrThrow(request.getAssigneeAccountId());

        // 2. Build entity
        Task task = new Task();
        task.setTaskNumber(generateTaskNumber(team));
        task.setTaskTitle(request.getTaskTitle());
        task.setTaskDesc(request.getTaskDesc());
        task.setFkTeamId(team);
        task.setFkAccountId(assignee);
        task.setFkAccountIdCreator(getAccountOrThrow(creatorAccountId));
        task.setTaskWorkflowId(getDefaultWorkflowId(team));
        task.setFkWorkflowTaskStatus(getInitialStatus());
        task.setCurrentActivityIndicator(0);

        // 3. Save
        Task saved = taskRepository.save(task);
        log.info("Task {} created successfully", saved.getTaskNumber());

        // 4. Post-processing
        createTaskHistory(saved, "CREATE", creatorAccountId);
        notifyAssignee(saved, assignee);

        return saved;
    }

    // Private helper methods
    private void validateTaskRequest(TaskCreateRequest request) {
        if (request.getTaskTitle() == null || request.getTaskTitle().trim().isEmpty()) {
            throw new ValidationFailedException("Task title is required");
        }
        if (request.getTaskTitle().length() < 3 || request.getTaskTitle().length() > 70) {
            throw new ValidationFailedException("Title must be 3-70 characters");
        }
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team not found: " + teamId));
    }

    private UserAccount getAccountOrThrow(Long accountId) {
        return userAccountRepository.findByAccountIdAndIsActive(accountId, true)
            .orElseThrow(() -> new NotFoundException("Account not found or inactive: " + accountId));
    }
}
```

---

## Controller Patterns

### Standard Controller
```java
@RestController
@RequestMapping("/task")
@Slf4j
public class TasksController {

    private final TaskService taskService;

    public TasksController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/addNewTask")
    public ResponseEntity<?> createTask(
            @RequestHeader("accountIds") String accountIds,
            @RequestHeader("timeZone") String timeZone,
            @RequestHeader("screenName") String screenName,
            @Valid @RequestBody TaskCreateRequest request) {

        try {
            Long creatorAccountId = parseFirstAccountId(accountIds);
            Task task = taskService.createTask(request, creatorAccountId, timeZone);
            TaskResponse response = TaskResponse.fromEntity(task, timeZone);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ValidationFailedException e) {
            log.warn("Validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(new ErrorResponse(406, e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal server error"));
        }
    }

    @GetMapping("/getTaskByTaskId/{taskId}")
    public ResponseEntity<?> getTask(
            @RequestHeader("accountIds") String accountIds,
            @RequestHeader("timeZone") String timeZone,
            @PathVariable Long taskId) {

        try {
            Task task = taskService.getTaskById(taskId);
            return ResponseEntity.ok(TaskResponse.fromEntity(task, timeZone));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/getTasks")
    public ResponseEntity<?> getTasks(
            @RequestHeader("accountIds") String accountIds,
            @RequestHeader("timeZone") String timeZone,
            @RequestBody TaskFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDateTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        Page<TaskResponse> tasks = taskService.getTasks(filter, pageable, timeZone);
        return ResponseEntity.ok(tasks);
    }

    private Long parseFirstAccountId(String accountIds) {
        return Long.parseLong(accountIds.split(",")[0].trim());
    }
}
```

---

## Exception Handling

### Custom Exceptions
```java
// Base exception
public class TseException extends RuntimeException {
    private final int statusCode;

    public TseException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

// Specific exceptions
public class ValidationFailedException extends TseException {
    public ValidationFailedException(String message) {
        super(message, 406);
    }
}

public class NotFoundException extends TseException {
    public NotFoundException(String message) {
        super(message, 404);
    }
}

public class UnauthorizedException extends TseException {
    public UnauthorizedException(String message) {
        super(message, 401);
    }
}
```

### Global Exception Handler
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationFailedException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
            .body(new ErrorResponse(406, ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "Internal server error"));
    }
}

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private String timestamp = Instant.now().toString();

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
```

---

## SQL & Query Writing

### JPQL Guidelines
```java
// Use named parameters
@Query("SELECT t FROM Task t WHERE t.taskId = :taskId")
Task findByTaskId(@Param("taskId") Long taskId);

// Join fetch for eager loading
@Query("SELECT t FROM Task t LEFT JOIN FETCH t.fkAccountId " +
       "LEFT JOIN FETCH t.fkTeamId WHERE t.taskId = :taskId")
Task findByIdWithAssociations(@Param("taskId") Long taskId);

// IN clause
@Query("SELECT t FROM Task t WHERE t.fkTeamId.teamId IN :teamIds")
List<Task> findByTeamIds(@Param("teamIds") List<Long> teamIds);
```

### Native SQL Guidelines
```java
// Use explicit schema
@Query(value = "SELECT * FROM tse.task WHERE fk_team_id = :teamId",
       nativeQuery = true)
List<Task> findByTeam(@Param("teamId") Long teamId);

// Complex joins
@Query(value = """
    SELECT t.*, u.first_name, u.last_name
    FROM tse.task t
    JOIN tse.user_account ua ON t.fk_account_id = ua.account_id
    JOIN tse.tse_users u ON ua.fk_user_id = u.user_id
    WHERE t.sprint_id = :sprintId
    ORDER BY t.task_exp_end_date ASC
    """, nativeQuery = true)
List<Object[]> findSprintTasksWithUsers(@Param("sprintId") Long sprintId);

// Pagination in native query
@Query(value = "SELECT * FROM tse.task WHERE fk_team_id = :teamId",
       countQuery = "SELECT COUNT(*) FROM tse.task WHERE fk_team_id = :teamId",
       nativeQuery = true)
Page<Task> findByTeamPaginated(@Param("teamId") Long teamId, Pageable pageable);
```

---

## Sorting & Filtering Logic

### Dynamic Sorting
```java
// In service
public Page<Task> getTasks(TaskFilterRequest filter, Pageable pageable) {
    // Validate sort field
    List<String> allowedSortFields = Arrays.asList(
        "createdDateTime", "lastUpdatedDateTime",
        "taskExpEndDate", "taskPriority", "taskTitle"
    );

    String sortBy = pageable.getSort().iterator().next().getProperty();
    if (!allowedSortFields.contains(sortBy)) {
        throw new ValidationFailedException("Invalid sort field: " + sortBy);
    }

    return taskRepository.findAll(buildSpecification(filter), pageable);
}

// Specification for dynamic filtering
private Specification<Task> buildSpecification(TaskFilterRequest filter) {
    return (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.getTeamIds() != null && !filter.getTeamIds().isEmpty()) {
            predicates.add(root.get("fkTeamId").get("teamId").in(filter.getTeamIds()));
        }

        if (filter.getSearchQuery() != null && !filter.getSearchQuery().isEmpty()) {
            String search = "%" + filter.getSearchQuery().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("taskTitle")), search),
                cb.like(cb.lower(root.get("taskNumber")), search)
            ));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    };
}
```

---

## Lombok Usage

### Standard Annotations
```java
@Entity
@Table(name = "task")
@Data                    // Getters, setters, equals, hashCode, toString
@NoArgsConstructor       // Required for JPA
@AllArgsConstructor      // Full constructor
public class Task {
    // fields
}

@Data
@Builder                 // Builder pattern for DTOs
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    // fields
}

@Service
@Slf4j                   // Logger injection
public class TaskServiceImpl {
    // log.info(), log.error(), etc.
}
```

### Avoid These Patterns
```java
// Don't use @Data on entities with relationships (causes infinite recursion)
// Instead, manually implement equals/hashCode using ID only

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Task {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return taskId != null && taskId.equals(task.taskId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

---

## Code Formatting

### Import Order
```java
// 1. java.* packages
import java.time.LocalDateTime;
import java.util.List;

// 2. javax.* packages
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

// 3. Spring packages
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// 4. Other libraries
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

// 5. Project packages
import com.tse.core_application.model.Task;
import com.tse.core_application.repository.TaskRepository;
```

### Method Organization
```java
public class TaskServiceImpl {
    // 1. Fields (private)
    private final TaskRepository taskRepository;

    // 2. Constructor
    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    // 3. Public methods (interface implementations)
    @Override
    public Task createTask(...) { }

    @Override
    public Task updateTask(...) { }

    // 4. Private helper methods
    private void validateRequest(...) { }

    private Task buildEntity(...) { }
}
```

---

## Security Patterns

### Request Validation
```java
// Always validate headers
public void validateRequest(String accountIds, String timeZone, String screenName) {
    if (accountIds == null || accountIds.isEmpty()) {
        throw new ValidationFailedException("accountIds header is required");
    }

    if (timeZone == null || !TimeZone.getAvailableIDs().contains(timeZone)) {
        throw new ValidationFailedException("Valid timeZone header is required");
    }

    if (screenName == null || !screenName.matches("^[a-zA-Z0-9]+$")) {
        throw new ValidationFailedException("Valid screenName header is required");
    }
}
```

### Permission Checks
```java
// Check permissions before operations
public void checkPermission(Long accountId, Long teamId, String action) {
    UserRole role = userRoleRepository.findByAccountIdAndTeamId(accountId, teamId);

    if (role == null) {
        throw new UnauthorizedException("User is not a member of this team");
    }

    if (!role.hasPermission(action)) {
        throw new UnauthorizedException("User lacks permission: " + action);
    }
}
```

---

## Testing Patterns

### Unit Test Structure
```java
@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void createTask_ValidRequest_Success() {
        // Given
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTaskTitle("Test Task");
        request.setTeamId(1L);

        Team team = new Team();
        team.setTeamId(1L);
        team.setTeamCode("DEV");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Task result = taskService.createTask(request, 1L, "Asia/Kolkata");

        // Then
        assertNotNull(result);
        assertEquals("Test Task", result.getTaskTitle());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_InvalidTitle_ThrowsException() {
        // Given
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTaskTitle("AB"); // Too short

        // When/Then
        assertThrows(ValidationFailedException.class,
            () -> taskService.createTask(request, 1L, "Asia/Kolkata"));
    }
}
```

### Integration Test Structure
```java
@SpringBootTest
@AutoConfigureMockMvc
class TasksControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void createTask_ValidRequest_Returns201() throws Exception {
        String requestBody = """
            {
                "taskTitle": "Test Task",
                "taskDesc": "Test Description",
                "teamId": 1
            }
            """;

        mockMvc.perform(post("/task/addNewTask")
                .header("Authorization", "Bearer " + getTestToken())
                .header("accountIds", "1")
                .header("timeZone", "Asia/Kolkata")
                .header("screenName", "test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.taskNumber").exists());
    }
}
```
