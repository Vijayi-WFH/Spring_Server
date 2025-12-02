# Exception Handling Patterns

## Table of Contents
1. [Overview](#overview)
2. [Global Exception Handlers](#global-exception-handlers)
3. [Custom Exception Classes](#custom-exception-classes)
4. [HTTP Status Code Mappings](#http-status-code-mappings)
5. [Error Response DTOs](#error-response-dtos)
6. [Logging Patterns](#logging-patterns)
7. [Exception Throwing Patterns](#exception-throwing-patterns)
8. [Error Constants](#error-constants)
9. [Best Practices](#best-practices)

---

## Overview

The Spring Server uses a layered exception handling approach:
1. **Service Layer**: Throws domain-specific exceptions
2. **Controller Advisor**: Catches and transforms exceptions to HTTP responses
3. **Response DTOs**: Standardized error response format
4. **Logging**: Structured logging with context

---

## Global Exception Handlers

### TSe_Server - ControllerAdvisor

**File:** `TSe_Server/src/main/java/com/tse/core_application/exception/ControllerAdvisor.java`

```java
@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {
    private static final Logger log = LogManager.getLogger(ControllerAdvisor.class);

    public static String getCurrentUTCTimeStamp() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss:SSS Z");
        return now.format(formatter);
    }

    // 50+ exception handlers...
}
```

**Features:**
- Extends `ResponseEntityExceptionHandler` for Spring MVC support
- UTC timestamp on all responses
- Handles both custom and standard exceptions
- Returns `RestResponseWithoutData` or `RestResponseWithData`

### TSEHR - ControllerAdvisor

**File:** `TSEHR/src/main/java/com/tse/core/exception/ControllerAdvisor.java`

```java
@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(...) {
        // HTTP 401 UNAUTHORIZED
    }

    @ExceptionHandler(LeaveApplicationValidationException.class)
    public ResponseEntity<Object> handleLeaveApplicationValidationException(...) {
        // HTTP 406 NOT_ACCEPTABLE
    }
}
```

**Scope:** Minimal - only 2 exception handlers for leave domain

### Chat-App - ControllerAdvisor

**File:** `Vijayi_WFH_Conversation/chat-app/src/main/java/com/example/chat_app/exception/ControllerAdvisor.java`

```java
@RestControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {
    private static final Logger log = LogManager.getLogger(ControllerAdvisor.class);

    @ExceptionHandler(UnauthorizedLoginException.class)
    public ResponseEntity<Object> handleUnauthorizedException(...) {
        log.error("Unauthorized login for accountIds={}, requestURI={}",
                  ThreadContext.get("accountIds"),
                  ThreadContext.get("requestURI"), e);
        ThreadContext.clearMap();
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
    }
}
```

**Features:**
- Uses `@RestControllerAdvice`
- ThreadContext logging for request tracking
- Context cleanup after logging

---

## Custom Exception Classes

### TSe_Server Exceptions (50+ classes)

**Location:** `TSe_Server/src/main/java/com/tse/core_application/exception/`

#### General Purpose Exceptions

| Exception | Purpose |
|-----------|---------|
| `ValidationFailedException` | Input validation failures |
| `NoDataFoundException` | No data returned from query |
| `UnauthorizedException` | Unauthorized access attempt |
| `InternalServerErrorException` | Generic server error |
| `ForbiddenException` | Access denied |

#### User Management Exceptions

| Exception | Purpose |
|-----------|---------|
| `UserDoesNotExistException` | User not found |
| `UserAlreadyExistException` | Duplicate user |
| `UserNotRegisteredException` | User not registered |
| `InvalidUserNameException` | Invalid username format |

#### Task Management Exceptions

| Exception | Purpose |
|-----------|---------|
| `TaskNotFoundException` | Task not found |
| `TaskEstimateException` | Invalid task estimate |
| `TaskViewException` | View permission denied |
| `DeleteTaskException` | Task deletion failed |
| `SubTaskDetailsMissingException` | Missing subtask info |
| `DependencyValidationException` | Invalid task dependency |

#### Meeting Management Exceptions

| Exception | Purpose |
|-----------|---------|
| `MeetingNotFoundException` | Meeting not found |
| `InvalidMeetingTypeException` | Invalid meeting type |
| `ReferenceMeetingException` | Meeting reference error (with data) |

#### File Management Exceptions

| Exception | Purpose |
|-----------|---------|
| `FileNotFoundException` | File not found |
| `FileNameException` | Invalid filename |
| `DuplicateFileException` | Duplicate file |
| `FileStorageException` | File storage error |
| `DuplicateFileNameException` | Duplicate filename |

#### Authentication Exceptions

| Exception | Purpose |
|-----------|---------|
| `AuthenticationFailedException` | Login failed |
| `UnauthorizedLoginException` | Invalid credentials |
| `InvalidTokenException` | Invalid JWT token |
| `TokenValidationFailedException` | Token validation error |
| `InvalidOtpException` | Invalid OTP |

#### Entity Exceptions

| Exception | Purpose |
|-----------|---------|
| `TeamNotFoundException` | Team not found |
| `ProjectNotFoundException` | Project not found |
| `OrganizationDoesNotExistException` | Org not found |
| `FenceNotFoundException` | Geo-fence not found |
| `PolicyNotFoundException` | Policy not found |
| `CommentNotFoundException` | Comment not found |

#### Other Exceptions

| Exception | Purpose |
|-----------|---------|
| `LeaveApplicationValidationException` | Leave validation error |
| `ServerBusyException` | Server overloaded |
| `FirebaseNotificationException` | FCM send error |
| `GeoFencingAccessDeniedException` | Outside geo-fence |
| `WorkflowTaskStatusFailedException` | Workflow status error |
| `InvalidStatsRequestFilterException` | Stats filter error |
| `StickyNoteFailedException` | Sticky note error |

### Special Exception with Data

```java
// File: TSe_Server/src/main/java/com/tse/core_application/exception/ReferenceMeetingException.java
@Getter
@Setter
public class ReferenceMeetingException extends RuntimeException {
    public final Boolean isNotificationOnCooldown;

    public ReferenceMeetingException(String errorMessage, Boolean isNotificationOnCooldown) {
        super(errorMessage);
        this.isNotificationOnCooldown = isNotificationOnCooldown;
    }
}
```

### TSEHR Exceptions (3 classes)

**Location:** `TSEHR/src/main/java/com/tse/core/exception/`

| Exception | Purpose |
|-----------|---------|
| `UnauthorizedException` | Unauthorized access |
| `ValidationFailedException` | Validation error |
| `LeaveApplicationValidationException` | Leave validation |

### Chat-App Exceptions (10 classes)

**Location:** `Vijayi_WFH_Conversation/chat-app/src/main/java/com/example/chat_app/exception/`

| Exception | Purpose |
|-----------|---------|
| `ValidationFailedException` | Validation error |
| `UnauthorizedLoginException` | Invalid login |
| `UnauthorizedActionException` | Action not allowed |
| `FileNotFoundException` | File not found |
| `FileNameException` | Invalid filename |
| `NotFoundException` | Generic not found |
| `InternalServerErrorException` | Server error |

---

## HTTP Status Code Mappings

### TSe_Server Status Codes

| HTTP Status | Code | Exceptions |
|-------------|------|------------|
| **400 BAD_REQUEST** | 400 | `MethodArgumentNotValidException`, `ConstraintViolationException`, `InvalidRequestParamater`, `IllegalArgumentException`, `HttpMessageNotReadableException`, `DependencyValidationException`, `InvalidInviteException`, `FileNameException`, `WorkflowTypeDoesNotExistException`, `NumberFormatException`, `BoardViewErrorException` |
| **401 UNAUTHORIZED** | 401 | `UnauthorizedException`, `UnauthorizedLoginException`, `UserNotRegisteredException`, `InvalidAuthentication`, `AuthenticationFailedException`, `InvalidUserNameException` |
| **403 FORBIDDEN** | 403 | `ForbiddenException`, `TaskViewException`, `ValidationFailedException`, `IllegalAccessException`, `IllegalStateException`, `TimeLimitExceededException`, `InvalidRelationTypeException`, `GeoFencingAccessDeniedException`, `ReferenceMeetingException` |
| **404 NOT_FOUND** | 404 | `EntityNotFoundException`, `UserDoesNotExistException`, `NoDataFoundException`, `ProjectNotFoundException`, `TaskNotFoundException`, `MeetingNotFoundException`, `CommentNotFoundException`, `TeamNotFoundException`, `FileNotFoundException`, `NoTaskMediaFoundException`, `InvalidApiEndpointException`, `OrganizationDoesNotExistException`, `FenceNotFoundException`, `PolicyNotFoundException`, `InvalidOtpException` |
| **406 NOT_ACCEPTABLE** | 406 | `InvalidRequestHeaderException`, `IncorrectRequestHeaderException`, `WorkflowTaskStatusFailedException`, `DateAndTimePairFailedException`, `TaskEstimateException`, `InvalidStatsRequestFilterException`, `InvalidMeetingTypeException`, `StickyNoteFailedException`, `LeaveApplicationValidationException`, `DeleteTaskException`, `IOException`, `AuthenticationException` |
| **409 CONFLICT** | 409 | `DuplicateFileNameException`, `FirebaseNotificationException`, `NoSuchAlgorithmException`, `HttpClientErrorException` |
| **412 PRECONDITION_FAILED** | 412 | `MissingDetailsException` |
| **415 UNSUPPORTED_MEDIA_TYPE** | 415 | `FileStorageException`, `DuplicateFileException` |
| **422 UNPROCESSABLE_ENTITY** | 422 | `UserAlreadyExistException`, `SubTaskDetailsMissingException` |
| **500 INTERNAL_SERVER_ERROR** | 500 | `Exception` (generic catch-all) |
| **202 ACCEPTED** | 202 | `ServerBusyException` |

### Chat-App Status Codes

| HTTP Status | Code | Exceptions |
|-------------|------|------------|
| **401 UNAUTHORIZED** | 401 | `UnauthorizedLoginException`, `UnauthorizedActionException` |
| **404 NOT_FOUND** | 404 | `FileNotFoundException`, `NotFoundException` |
| **405 METHOD_NOT_ALLOWED** | 405 | `IllegalStateException` |
| **406 NOT_ACCEPTABLE** | 406 | `ValidationFailedException` |
| **417 EXPECTATION_FAILED** | 417 | `NullPointerException` |
| **500 INTERNAL_SERVER_ERROR** | 500 | `Exception` (generic) |
| **502 BAD_GATEWAY** | 502 | `RestClientException` |

### TSEHR Status Codes

| HTTP Status | Code | Exceptions |
|-------------|------|------------|
| **401 UNAUTHORIZED** | 401 | `UnauthorizedException` |
| **406 NOT_ACCEPTABLE** | 406 | `LeaveApplicationValidationException` |

---

## Error Response DTOs

### RestResponseWithoutData

**Location:** All modules have identical structure

```java
// TSe_Server/src/main/java/com/tse/core_application/custom/model/RestResponseWithoutData.java
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RestResponseWithoutData {
    private Integer status;     // HTTP status code
    private String message;     // Error message
    private String timestamp;   // UTC timestamp
}
```

**Example Response:**
```json
{
    "status": 404,
    "message": "Task not found",
    "timestamp": "2024-12-02 14:30:45:123 +0000"
}
```

### RestResponseWithData

**Location:** `TSe_Server/src/main/java/com/tse/core_application/custom/model/RestResponseWithData.java`

```java
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RestResponseWithData {
    private Integer status;
    private String message;
    private String timestamp;
    private Object data;        // Additional error data
}
```

**Used by:** `ReferenceMeetingException` and similar exceptions that need to return additional data.

### CustomResponseHandler

**Location:** `TSe_Server/src/main/java/com/tse/core_application/handlers/CustomResponseHandler.java`

```java
public class CustomResponseHandler {

    public static ResponseEntity<Object> generateCustomResponse(
            HttpStatus status, String message, Object responseObj) {
        RestResponseWithData restResponseWithData = new RestResponseWithData();
        restResponseWithData.setStatus(status.value());
        restResponseWithData.setMessage(message);
        restResponseWithData.setTimestamp(getCurrentUTCTimeStamp());
        restResponseWithData.setData(responseObj);
        return new ResponseEntity<>(restResponseWithData, status);
    }

    public static String getCurrentUTCTimeStamp() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss:SSS Z");
        return now.format(formatter);
    }
}
```

---

## Logging Patterns

### TSe_Server Logging Strategy

```java
// ControllerAdvisor.java
@ExceptionHandler(Exception.class)
public ResponseEntity<Object> handleAllExceptions(Exception e, WebRequest request) {
    log.error("e: ", e);  // Full stack trace for unhandled exceptions

    RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
    restResponseWithoutData.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    restResponseWithoutData.setMessage("Internal server error");
    restResponseWithoutData.setTimestamp(getCurrentUTCTimeStamp());

    return new ResponseEntity<>(restResponseWithoutData, HttpStatus.INTERNAL_SERVER_ERROR);
}
```

### Chat-App Logging Strategy with ThreadContext

```java
// ControllerAdvisor.java
@ExceptionHandler(UnauthorizedLoginException.class)
public ResponseEntity<Object> handleUnauthorizedException(
        UnauthorizedLoginException e, WebRequest request) {

    RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
    restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
    restResponseWithoutData.setMessage(e.getMessage());
    restResponseWithoutData.setTimestamp(getCurrentUTCTimeStamp());

    // Log with context information
    log.error("Unauthorized login for accountIds={}, requestURI={}",
              ThreadContext.get("accountIds"),
              ThreadContext.get("requestURI"), e);

    // Clean up thread context to prevent memory leaks
    ThreadContext.clearMap();

    return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
}
```

**ThreadContext Fields Logged:**
- `accountIds` - User's account IDs
- `requestURI` - Request URI being accessed

### ExceptionUtil Pattern

**Location:** `TSe_Server/src/main/java/com/tse/core_application/utils/ExceptionUtil.java`

```java
public class ExceptionUtil {

    private void onUncheckedException(RuntimeException runtimeException) {
        if (runtimeException instanceof FileNameException) {
            throw runtimeException;
        } else if (runtimeException instanceof DuplicateFileException) {
            throw runtimeException;
        } else if (runtimeException instanceof FileNotFoundException) {
            throw runtimeException;
        } else {
            throw new InternalServerErrorException(runtimeException.getMessage());
        }
    }

    private void onCheckedException(Exception exception) {
        if (exception instanceof IOException) {
            throw new InternalServerErrorException(exception.getMessage());
        }
    }

    public void onException(Exception exception) {
        if (exception instanceof RuntimeException) {
            onUncheckedException((RuntimeException) exception);
        } else {
            onCheckedException(exception);
        }
    }
}
```

---

## Exception Throwing Patterns

### Service Layer Exception Throwing

#### Validation Exceptions

```java
// TaskService.java
if (taskTitle == null || taskTitle.isEmpty()) {
    throw new ValidationFailedException("Task title is required");
}

if (taskTitle.length() > 70) {
    throw new ValidationFailedException("Title should be between 3 and 70 characters");
}
```

#### Authorization Exceptions

```java
// PerfNoteService.java
if (userRatingOwnPerformance) {
    throw new IllegalAccessException("User not authorized to rate his/her own performance");
}

if (!isUserAuthorized) {
    throw new UnauthorizedException("User not authorized to perform this action");
}
```

#### Entity Not Found Exceptions

```java
// TaskService.java
Task task = taskRepository.findById(taskId)
    .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + taskId));

// UserService.java
User user = userRepository.findByEmail(email)
    .orElseThrow(() -> new UserDoesNotExistException("User not found"));
```

#### State Exceptions

```java
// ExceptionalRegistrationService.java
if (userAlreadyRegistered) {
    throw new IllegalStateException("User already registered as exceptional user");
}
```

#### Leave Domain Exceptions

```java
// LeaveService.java (TSEHR)
if (leaveRemaining < numberOfLeaveDays && !negativeLeaveAllowed) {
    throw new LeaveApplicationValidationException(
        "'" + leaveTypeAlias + "' leave remaining is lower than you applied for.");
}

if (negativeLeaveEnabled && maxNegativeLeaves == null) {
    throw new IllegalStateException(
        "Negative leave allowance is enabled, but the maximum allowed negative leaves count is not specified");
}
```

#### Chat-App Exceptions

```java
// ChatService.java
if (!userInGroup) {
    throw new UnauthorizedLoginException("User is not part of this group.");
}

if (senderReceiverNotInSameOrg) {
    throw new ValidationFailedException(
        "Sender and Receiver are not a part of the same Org.");
}

if (fileScanFailed) {
    throw new ValidationFailedException(
        "File scan failed for: " + filename + ". The file might be infected or corrupted.");
}
```

---

## Error Constants

### TSe_Server ErrorConstant

**Location:** `TSe_Server/src/main/java/com/tse/core_application/constants/ErrorConstant.java`

```java
public final class ErrorConstant {

    // User validation errors
    public static final String PRIMARY_EMAIL_ERROR = "Primary email is missing";
    public static final String FIRST_NAME_ERROR = "First name is missing";
    public static final String LAST_NAME_ERROR = "Last name is missing";
    public static final String USERNAME_ERROR = "Username is missing";

    // Task errors
    public static final class Task {
        public static final String TASK_TITLE = "task title is missing";
        public static final String TASK_DESC = "task description is missing";
        public static final String TASK_WORKFLOW_ID = "task workflow id is missing";
        public static final String TITLE_LIMIT = "Title should be between 3 and 70 characters";
        public static final String DESC_LIMIT = "Description Length should be between 3 and 5000 characters";
    }

    // Epic errors
    public static final class Epic {
        public static final String EPIC_ID = "Epic id is missing";
        public static final String EPIC_TITLE = "Epic title is missing";
        public static final String EPIC_LIMIT = "Epic should be between 3 and 70 characters";
    }

    // Leave errors
    public static final class Leave {
        public static final String ACCOUNT_ID_ERROR = "Account ids can not be null or empty";
        public static final String ORG_ID_ERROR = "Organization is a mandatory field";
        public static final String INITIAL_LEAVE_ERROR = "Initial leave can not be null or empty";
    }

    // Meeting errors
    public static final class Meeting {
        public static final String MEETING_ID = "Meeting id can't be null";
        public static final String AGENDA = "Agenda of the meeting is missing.";
        public static final String ATTENDEE_LIST = "Attendee List Cannot be null, invite someone.";
        public static final String MOM_LIMIT = "Minutes of Meeting length should be less than 5000 characters";
    }

    // Team errors
    public static final class Team {
        public static final String TEAM_DESC = "Team Description should be between 3 and 1000 characters";
        public static final String TEAM_NAME = "Team Name should be between 3 and 50 characters";
    }

    // Sprint errors
    public static final class Sprint {
        public static final String SPRINT_TITLE = "Sprint title is a mandatory field.";
        public static final String TITLE_LIMIT = "Sprint title should be between 3 to 70 characters";
    }

    // Jira integration errors
    public static final class Jira {
        public static final String SITE_URL_REQUIRED = "Jira site URL is required.";
        public static final String EMAIL_REQUIRED = "Jira email is required.";
        public static final String TOKEN_REQUIRED = "Jira token is required.";
    }
}
```

### TSEHR ErrorConstant

**Location:** `TSEHR/src/main/java/com/tse/core/constants/ErrorConstant.java`

Similar structure with focus on leave management errors.

---

## Best Practices

### 1. Exception Hierarchy

```
RuntimeException (all custom exceptions extend this)
├── ValidationFailedException
├── UnauthorizedException
├── NoDataFoundException
├── FileNotFoundException
├── TaskNotFoundException
├── ... (50+ more specific exceptions)
└── Standard Java Exceptions
```

### 2. Response Wrapper Pattern

```
Exception Thrown
    ↓
@ControllerAdvice catches
    ↓
Creates RestResponseWithoutData/RestResponseWithData
    ↓
Sets HTTP Status Code
    ↓
Adds UTC Timestamp
    ↓
Returns ResponseEntity to client
```

### 3. UTC Timestamp Standard

All error responses include UTC timestamps:
```
Format: "yyyy-MM-dd HH:mm:ss:SSS Z"
Example: "2024-12-02 14:30:45:123 +0000"
```

### 4. Layered Exception Handling

1. **Controller Layer:** Catches and validates input
2. **Service Layer:** Business logic validation and throwing domain exceptions
3. **Utility Layer:** Generic exception translation (ExceptionUtil)
4. **Global Handler:** ControllerAdvisor catches all exceptions

### 5. Context Logging Pattern (Chat-App)

```java
// Set context before operation
ThreadContext.put("accountIds", accountId);
ThreadContext.put("requestURI", requestURI);

// Log with context
log.error("Error message for accountIds={}, requestURI={}",
          ThreadContext.get("accountIds"),
          ThreadContext.get("requestURI"), exception);

// Clean up
ThreadContext.clearMap();
```

### 6. Recommendations

1. **Always use specific exceptions** - Don't throw generic `RuntimeException`
2. **Use error constants** - Centralize error messages
3. **Include UTC timestamps** - For audit trails
4. **Log full stack traces** - For debugging unexpected errors
5. **Clean up ThreadContext** - Prevent memory leaks
6. **Map to appropriate HTTP status** - Follow REST conventions
7. **Return consistent response format** - Always use `RestResponseWithoutData`

---

## Module Comparison

| Aspect | TSe_Server | TSEHR | Chat-App |
|--------|-----------|-------|----------|
| Exception Handlers | 50+ | 2 | 9 |
| Custom Exceptions | 50+ | 3 | 10 |
| Global Handler Type | `@ControllerAdvice` | `@ControllerAdvice` | `@RestControllerAdvice` |
| Response Models | Both DTOs | Without Data | Without Data |
| Logging Framework | Log4j2 | None | Log4j2 + ThreadContext |
| UTC Timestamps | Yes | Yes | Yes |
| Error Constants | Comprehensive | Minimal | None |
