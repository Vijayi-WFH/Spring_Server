# Shared Utility Libraries

## Table of Contents
1. [Overview](#overview)
2. [Date/Time Utilities](#datetime-utilities)
3. [Common Utilities](#common-utilities)
4. [Security & Encryption](#security--encryption)
5. [JWT Utilities](#jwt-utilities)
6. [File Handling](#file-handling)
7. [Data Converters](#data-converters)
8. [Notification Utilities](#notification-utilities)
9. [Geo-Fencing Utilities](#geo-fencing-utilities)
10. [Other Utilities](#other-utilities)
11. [Constants Classes](#constants-classes)

---

## Overview

The project contains **27 utility classes** across 3 modules:
- **TSe_Server**: 20 utility classes
- **TSEHR**: 2 utility classes
- **Chat-App**: 5 utility classes

---

## Date/Time Utilities

### TSe_Server DateTimeUtils

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/DateTimeUtils.java`

**Purpose:** Comprehensive timezone and date/time conversion utilities

**Key Methods:**

```java
// Convert LocalDateTime to milliseconds
public static Long getDateAndTimeInMillis(LocalDateTime localDateTime)
public static Long getDateAndTimeInMillis(LocalDate localDate, LocalTime localTime)

// User timezone to server timezone conversion
public static LocalDateTime convertUserDateToServerTimezone(
    LocalDateTime localDateTime, String userTimeZone)

// Server timezone to user timezone conversion
public static LocalDateTime convertServerDateToUserTimezone(
    LocalDateTime serverDateTime, String userTimeZone)

// Time-only conversions
public static LocalTime convertUserTimeToServerTimeZone(
    LocalTime time, String userTimeZone)
public static LocalTime convertServerTimeToUserTimeZone(
    LocalTime time, String userTimeZone)

// Dynamic date parsing (11+ supported formats)
public static LocalDateTime parseDynamicDate(String dateString)
// Supported formats:
// - yyyy-MM-dd'T'HH:mm:ss.SSSSSS
// - yyyy-MM-dd'T'HH:mm:ss.SSS
// - yyyy-MM-dd'T'HH:mm:ss
// - yyyy-MM-dd HH:mm:ss
// - yyyy-MM-dd
// - dd-MM-yyyy
// - MM/dd/yyyy
// - With offset: +05:30, Z, etc.

// Parse ISO format
public static LocalDateTime parseDateTime(String dateTimeString)
```

**Usage Example:**
```java
// Convert user's local time to server timezone for storage
LocalDateTime userDateTime = LocalDateTime.of(2024, 12, 2, 10, 30);
LocalDateTime serverDateTime = DateTimeUtils.convertUserDateToServerTimezone(
    userDateTime, "Asia/Kolkata");

// Convert server time back to user's timezone for display
LocalDateTime displayTime = DateTimeUtils.convertServerDateToUserTimezone(
    serverDateTime, "America/New_York");
```

### TSEHR DateTimeUtils

**Path:** `TSEHR/src/main/java/com/tse/core/utils/DateTimeUtils.java`

**Key Methods:**
```java
// Calculate days between two dates
public static Long differanceBetweenTwoDates(LocalDate from, LocalDate to)

// Timezone conversions
public static LocalDateTime convertLocalTimeToServerTimeZone(
    LocalDateTime localDateTime, String userTimeZone)
public static LocalDateTime convertServerDateToLocalTimezone(
    LocalDateTime serverDateTime, String userTimeZone)
```

### Chat-App DateTimeUtils

**Path:** `Vijayi_WFH_Conversation/chat-app/src/main/java/com/example/chat_app/utils/DateTimeUtils.java`

**Key Methods:**
```java
// Millisecond precision timezone conversion
public static LocalDateTime convertUserDateToServerTimezone(
    LocalDateTime localDateTime, String userTimeZone)
public static LocalDateTime convertServerDateToUserTimezone(
    LocalDateTime serverDateTime, String userTimeZone)
```

---

## Common Utilities

### TSe_Server CommonUtils

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/CommonUtils.java`

**Purpose:** General-purpose utility functions

**Key Methods:**

```java
// Redis key generation for OTP
public static String getRedisKeyForOtp(String email)
// Returns: "OTP_<email>"

// RSA key management
public static PrivateKey getPrivateKey() throws Exception
public static PublicKey getPublicKey() throws Exception
// Reads from: keys/private_key.der, keys/public_key.der

// Team identifier generation
public static String createJIDForTeamName(String teamName)
// Returns: normalized team name for XMPP/chat

// Bean property copying (non-null only)
public static void copyNonNullProperties(Object source, Object target)
// Uses Spring BeanWrapper to copy only non-null properties

// List intersection check
public static boolean containsAny(List<Long> list1, List<Long> list2)
// Returns true if lists share any common elements

// String case conversion
public static String convertToTitleCase(String str)
// "hello world" -> "Hello World"

// Current date in timezone
public static LocalDate getLocalDateInGivenTimeZone(String timeZone)

// Header validation
public static void validateTimeZoneAndScreenNameInHeader(String timeZone, String screenName)
// Throws InvalidRequestHeaderException if invalid

// HTML text length validation (uses Jsoup)
public static boolean isValidPlainTextLength(String htmlContent, int maxLength)
// Extracts plain text from HTML and checks length

// CSV to List conversion
public static List<Long> convertToLongList(String commaSeperatedIds)
// "1,2,3" -> [1L, 2L, 3L]

// Get non-null field names via reflection
public static List<String> getNonNullFieldNames(Object object)

// String truncation
public static String truncateWithEllipsis(String str, int maxLength)
// "Hello World" with maxLength=8 -> "Hello..."

// Developer account validation
public static boolean validateDeveloperAccount(Long accountId)
// Checks against app.developer.accountIds property

// Task estimate calculation
public static Float calculateTaskEstimateAdjustment(Float currentEstimate, Float adjustment)

// List to CSV conversion
public static String convertListToString(List<Long> list)
// [1L, 2L, 3L] -> "1,2,3"

// ObjectMapper configuration for Hibernate5
public static ObjectMapper configureObjectMapper(ObjectMapper mapper)
// Registers Hibernate5Module for lazy loading support

// Secret file readers
public static String getAppId()           // Reads from files/app-id.txt
public static String getGithubClientId()  // Reads from files/github-client-id.txt
public static String getGithubClientSecret() // Reads from files/github-client-secret.txt
```

**Usage Example:**
```java
// Copy non-null properties from DTO to entity
TaskDto dto = request.getTaskDto();
Task entity = taskRepository.findById(taskId);
CommonUtils.copyNonNullProperties(dto, entity);

// Validate HTML content length
String description = "<p>Hello <b>World</b></p>";
if (!CommonUtils.isValidPlainTextLength(description, 5000)) {
    throw new ValidationFailedException("Description too long");
}
```

### TSEHR CommonUtils

**Path:** `TSEHR/src/main/java/com/tse/core/utils/CommonUtils.java`

**Key Methods:**
```java
public static void copyNonNullProperties(Object source, Object target)
public static boolean containsAny(List<Long> list1, List<Long> list2)
public static List<Long> convertToLongList(String commaSeperatedIds)
```

---

## Security & Encryption

### EncryptionUtils

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/EncryptionUtils.java`

**Purpose:** Symmetric encryption/decryption for data at rest

**Key Methods:**
```java
// Encrypt list of values
public static List<String> getEncryptValues(List<String> values)

// Decrypt list of values
public static List<String> getDecryptedValues(List<String> values)
```

**Integration:** Uses `DataEncryptionConverter` for JPA entity attribute conversion.

**Configuration:**
```properties
encryption.key=your_16_byte_key
```

### PBKDF2Encoder

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/PBKDF2Encoder.java`

**Purpose:** Password hashing using PBKDF2WithHmacSHA512

```java
@Component
public class PBKDF2Encoder implements PasswordEncoder {

    @Value("${springbootwebfluxjjwt.password.encoder.secret}")
    private String secret;

    @Value("${springbootwebfluxjjwt.password.encoder.iteration}")
    private Integer iteration;

    @Value("${springbootwebfluxjjwt.password.encoder.keylength}")
    private Integer keyLength;

    @Override
    public String encode(CharSequence rawPassword) {
        // Uses PBKDF2WithHmacSHA512 with configurable iterations
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
```

**Configuration:**
```properties
springbootwebfluxjjwt.password.encoder.secret=mysecret
springbootwebfluxjjwt.password.encoder.iteration=33
springbootwebfluxjjwt.password.encoder.keylength=256
```

---

## JWT Utilities

### TSe_Server JWTUtil

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/JWTUtil.java`

**Purpose:** JWT token generation and validation with RSA signatures

**Key Methods:**
```java
// Generate JWT token
public String generateToken(User user, List<Long> accountIds, String chatPassword)
// Claims: username, accountIds, chatPassword
// Algorithm: RS512 (RSA with SHA-512)
// Expiration: Configurable via property

// Validate token
public Boolean validateToken(String token)
// Verifies signature using public key

// Extract username (handles expired tokens)
public String getUsernameFromToken(String token)

// Extract all account IDs from token
public List<Long> getAllAccountIdsFromToken(String token)

// Get expiration date
public Date getExpirationDateFromToken(String token)

// Check if token expired
public Boolean isTokenExpired(String token)
```

**Key Files:**
- `keys/private_key.der` - RSA private key for signing
- `keys/public_key.der` - RSA public key for verification

**Configuration:**
```properties
springbootwebfluxjjwt.jjwt.expiration=2592000  # 30 days in seconds
```

### SHAJWTUtil

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/SHAJWTUtil.java`

**Purpose:** JWT token generation for Jitsi meetings

**Key Methods:**
```java
// Generate meeting token
public String doGenerateToken(String roomName, Map<String, Object> context)
// Algorithm: HS256 (HMAC SHA-256)
// Expiration: 1 hour default

// Generate guest token with custom expiration
public String doGenerateTokenForGuests(String roomName, Map<String, Object> context, long expirationMinutes)

// Get signing key
private Key getSigningKey()
// Derives key from jitsi secret
```

### Chat-App JWTUtil

**Path:** `Vijayi_WFH_Conversation/chat-app/src/main/java/com/example/chat_app/jwtUtils/JWTUtil.java`

**Purpose:** JWT validation via remote TSe_Server

**Key Methods:**
```java
// Validate token against TSe_Server
public Boolean validateTokenAndAccountIds(String token, String accountIds)
// Calls: POST {tseserver.application.root.path}/api/auth/validateTokenAccount
```

---

## File Handling

### TSe_Server FileUtils

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/FileUtils.java`

**Purpose:** File upload validation and quota management

**Key Methods:**
```java
// Validate filename length (max 100 chars)
public static boolean isFilenameValidated(String filename)

// Check extension against whitelist
public static boolean isFileExtensionValidated(String filename, String allowedExtensions)

// Sanitize filename (remove unsafe characters)
public static String sanitizeFilename(String filename, String allowedExtensions)
// Removes: / \ : * ? " < > | and validates extension

// Validate file size against org quota
public static boolean validateFileSizeForOrg(
    MultipartFile file,
    EntityPreference entityPreference,
    Long currentUsedMemory)
// Checks:
// - File size <= entity preference limit
// - Total memory quota not exceeded
```

**Configuration:**
```properties
chat.file.extensions=.avi,.csv,.doc,.docx,.gif,.jpg,.jpeg,.pdf,.png,.txt,.xls,.xlsx,...
task.file.extensions=.avi,.csv,.doc,.docx,.gif,.jpg,.jpeg,.pdf,.png,.txt,.xls,.xlsx,...
default.file.size=10485760  # 10MB
```

### Chat-App FileUtils

**Path:** `Vijayi_WFH_Conversation/chat-app/src/main/java/com/example/chat_app/utils/FileUtils.java`

**Similar methods plus:**
```java
// Load attachment metadata from comma-separated IDs
public static List<Attachment> addFileMetaData(String attachmentIds)
```

### ComponentUtils (File Scanning)

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/ComponentUtils.java`

**Purpose:** Malware detection via external scanning service

```java
@Component
public class ComponentUtils {

    @Value("${scanfile.endpoint}")
    private String scanFileEndpoint;

    // Scan file for malware
    public boolean scanFile(MultipartFile file) {
        // Posts file to external virus scanning API
        // Returns true if clean, false if infected
    }
}
```

**Configuration:**
```properties
scanfile.endpoint=http://85.25.119.59:8080/upload
```

---

## Data Converters

### JPA AttributeConverters

All converters implement `AttributeConverter<T, String>` for database serialization.

#### ListConverter (Abstract Base)

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/ListConverter.java`

```java
public abstract class ListConverter<T extends Number>
    implements AttributeConverter<List<T>, String> {

    @Override
    public String convertToDatabaseColumn(List<T> attribute) {
        return attribute != null
            ? attribute.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","))
            : null;
    }

    @Override
    public List<T> convertToEntityAttribute(String dbData) {
        return dbData != null && !dbData.isEmpty()
            ? Arrays.stream(dbData.split(","))
                .map(this::convertStringToNumber)
                .collect(Collectors.toList())
            : new ArrayList<>();
    }

    protected abstract T convertStringToNumber(String s);
}
```

#### IntegerListConverter

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/IntegerListConverter.java`

```java
@Converter
public class IntegerListConverter extends ListConverter<Integer> {
    @Override
    protected Integer convertStringToNumber(String s) {
        return Integer.parseInt(s.trim());
    }
}
```

**Usage in Entity:**
```java
@Convert(converter = IntegerListConverter.class)
private List<Integer> dayNumbers;  // Stored as "1,2,3,4,5"
```

#### LongListConverter

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/LongListConverter.java`

```java
@Converter
public class LongListConverter implements AttributeConverter<List<Long>, String> {
    // Converts List<Long> to comma-separated string
}
```

**Usage:**
```java
@Convert(converter = LongListConverter.class)
private List<Long> childTaskIds;  // Stored as "101,102,103"
```

#### StringListConverter

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/StringListConverter.java`

```java
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
    // Converts List<String> to comma-separated string
}
```

#### LocalDateTimeListConverter

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/LocalDateTimeListConverter.java`

```java
@Converter
public class LocalDateTimeListConverter
    implements AttributeConverter<List<LocalDateTime>, String> {
    // Converts List<LocalDateTime> for database storage
}
```

#### CustomStringArrayUserTypesEntity

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/CustomStringArrayUserTypesEntity.java`

**Purpose:** Hibernate custom type for PostgreSQL string arrays

---

## Notification Utilities

### FCMNotificationUtil

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/FCMNotificationUtil.java`

**Purpose:** Firebase Cloud Messaging notification dispatch

**Key Methods:**
```java
// Send FCM to all user's device tokens
public void sendFcmNotification(Long accountId, PushNotificationRequest request)
// Retrieves all FCM tokens for user and sends to each

// Send with user preference filtering
public void sendPushNotification(Long accountId, PushNotificationRequest request,
    NotificationCategory category)
// Checks user's notification preferences before sending

// Send conversation-specific notifications
public void sendConversationPushNotification(Long accountId,
    PushNotificationRequest request)
// For chat message notifications
```

**Features:**
- Multi-token support per user
- Category-based filtering (TASK, MEETING, LEAVE, CHAT)
- User preference respect
- Device type support (Android, iOS)

### DeliveryAckScheduler (Chat-App)

**Path:** `Vijayi_WFH_Conversation/chat-app/src/main/java/com/example/chat_app/utils/DeliveryAckScheduler.java`

**Purpose:** Batch processing of message delivery/read acknowledgments

```java
@Component
public class DeliveryAckScheduler {

    private final ConcurrentHashMap<String, List<MessageAck>> buffer =
        new ConcurrentHashMap<>();

    // Buffer ACK for batch processing
    public void addMessageAckValue(String groupId, MessageAck ack) {
        buffer.computeIfAbsent(groupId, k -> new ArrayList<>()).add(ack);
    }

    // Process batch every 2 seconds
    @Scheduled(fixedRate = 2000)
    public void processBatch() {
        // Atomic swap for thread-safe processing
        Map<String, List<MessageAck>> toProcess = new HashMap<>(buffer);
        buffer.clear();

        for (Map.Entry<String, List<MessageAck>> entry : toProcess.entrySet()) {
            updateStatsAndNotifySender(entry.getKey(), entry.getValue());
        }
    }

    // Calculate tick status
    public TickStatus calculateTickStatus(Message message, List<GroupUser> groupUsers) {
        // Returns: SINGLE_TICK, DOUBLE_TICK, or DOUBLE_BLUE_TICK
    }
}
```

---

## Geo-Fencing Utilities

### GeoMath

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/geo_fencing/GeoMath.java`

**Purpose:** Geographic calculations using Haversine formula

```java
public class GeoMath {

    private static final double EARTH_RADIUS_METERS = 6_371_000; // 6,371 km

    // Calculate distance between two points
    public static double distanceMeters(
            double lat1, double lng1,
            double lat2, double lng2) {
        // Haversine formula
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    // Check if point is within circular fence
    public static boolean isWithinFence(
            double pointLat, double pointLng,
            double fenceLat, double fenceLng,
            double radiusMeters) {
        double distance = distanceMeters(pointLat, pointLng, fenceLat, fenceLng);
        return distance <= radiusMeters;
    }
}
```

**Usage Example:**
```java
// Check if user is within office geo-fence
GeoFence office = geoFenceRepository.findById(fenceId);
boolean isInOffice = GeoMath.isWithinFence(
    userLat, userLng,
    office.getCenterLat(), office.getCenterLng(),
    office.getRadiusM()
);
```

---

## Other Utilities

### TeamIdentifierGenerator

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/TeamIdentifierGenerator.java`

**Purpose:** Generate unique 2-4 character team codes

```java
public class TeamIdentifierGenerator {

    public static String generateUniqueIdentifier(
            String teamName,
            Set<String> existingCodes) {
        // Algorithm:
        // 1. Extract first letters of words
        // 2. Handle single-word edge cases
        // 3. Ensure first character is alphabetic
        // 4. Add numeric suffix if duplicate

        // "Backend Team" -> "BT"
        // "QA" -> "QA"
        // "Support" -> "SUP"
    }
}
```

### ExceptionUtil

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/ExceptionUtil.java`

**Purpose:** Exception classification and routing

```java
public class ExceptionUtil {

    public void onException(Exception exception) {
        if (exception instanceof RuntimeException) {
            onUncheckedException((RuntimeException) exception);
        } else {
            onCheckedException(exception);
        }
    }

    private void onUncheckedException(RuntimeException e) {
        // Pass through known exceptions
        if (e instanceof FileNameException ||
            e instanceof DuplicateFileException ||
            e instanceof FileNotFoundException) {
            throw e;
        }
        // Wrap unknown exceptions
        throw new InternalServerErrorException(e.getMessage());
    }
}
```

### EmptyStringToNullDeserializer

**Path:** `TSe_Server/src/main/java/com/tse/core_application/utils/EmptyStringToNullDeserializer.java`

**Purpose:** Jackson deserializer for empty string handling

```java
public class EmptyStringToNullDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) {
        String value = p.getValueAsString();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
```

**Usage:**
```java
@JsonDeserialize(using = EmptyStringToNullDeserializer.class)
private String optionalField;  // "" becomes null
```

---

## Constants Classes

### TSe_Server Constants

**Path:** `TSe_Server/src/main/java/com/tse/core_application/constants/Constants.java`

**Key Sections:**

```java
public final class Constants {

    // HTTP Response constants
    public static final class FormattedResponse {
        public static final Integer SUCCESS = 200;
        public static final Integer CREATED = 201;
        public static final Integer BAD_REQUEST = 400;
        public static final Integer UNAUTHORIZED = 401;
        public static final Integer NOT_FOUND = 404;
    }

    // Task search field names
    public static final class SearchCriteriaConstants {
        public static final String TASK_TITLE = "taskTitle";
        public static final String TASK_NUMBER = "taskNumber";
        public static final String ASSIGNEE_NAME = "assigneeName";
        // ...
    }

    // Work status enums
    public static final class WorkStatus {
        public static final Integer REMOTE = 1;
        public static final Integer OFFICE = 2;
        public static final Integer HYBRID = 3;
    }

    // Task history column mappings (200+ entries)
    public static final class TaskHistory_Column_Name {
        public static final String TASK_TITLE = "taskTitle";
        public static final String TASK_DESCRIPTION = "taskDescription";
        // ... maps field names to display names
    }
}
```

### TSe_Server ControllerConstants

**Path:** `TSe_Server/src/main/java/com/tse/core_application/constants/ControllerConstants.java`

**Key Sections:**

```java
public final class ControllerConstants {

    // Pagination defaults
    public static final class CommentsWithPage {
        public static final Integer DEFAULT_PAGE = 0;
        public static final Integer DEFAULT_SIZE = 10;
    }

    // Auto-increment starting values
    public static final class TaskNumber {
        public static final Long START = 1000L;
    }

    // External service routes
    public static final class TseHr {
        public static final String ROOT = "/api/v1";
        public static final String LEAVE = "/leave";
        public static final String TIMESHEET = "/timesheet";
    }

    public static final class Conversations {
        public static final String ROOT = "/api/v1";
        public static final String CHAT = "/chat";
    }

    public static final class JitsiApi {
        public static final String BASE = "/jitsi";
        public static final String CREATE_ROOM = "/createRoom";
    }
}
```

### TSEHR Constants

**Path:** `TSEHR/src/main/java/com/tse/core/model/Constants.java`

```java
public final class Constants {

    // Leave application status
    public static final class Leave {
        public static final Integer WAITING_APPROVAL = 1;
        public static final Integer WAITING_CANCEL = 2;
        public static final Integer APPROVED = 3;
        public static final Integer REJECTED = 4;
        public static final Integer CANCELLED = 5;
        public static final Integer CANCELLED_AFTER_APPROVAL = 6;
        public static final Integer APPLICATION_EXPIRED = 7;
        public static final Integer CONSUMED = 8;
    }

    // Task types
    public static final class TaskTypes {
        public static final Integer TASK = 1;
        public static final Integer PARENT_TASK = 2;
        public static final Integer CHILD_TASK = 3;
        public static final Integer BUG = 4;
        public static final Integer EPIC = 5;
        public static final Integer INITIATIVE = 6;
        public static final Integer RISK = 7;
        public static final Integer PERSONAL_TASK = 8;
    }

    // Half-day leave types
    public static final class HalfDayLeaveType {
        public static final Integer FIRST_HALF = 1;
        public static final Integer SECOND_HALF = 2;
    }
}
```

### Chat-App Constants

**Path:** `Vijayi_WFH_Conversation/chat-app/src/main/java/com/example/chat_app/constants/Constants.java`

```java
public final class Constants {

    // Group types
    public static final class GroupTypes {
        public static final String ORG_DEFAULT = "ORG_DEFAULT";
        public static final String PROJ_DEFAULT = "PROJ_DEFAULT";
        public static final String TEAM_DEFAULT = "TEAM_DEFAULT";
        public static final String CUSTOM = "CUSTOM";
    }

    // Message status types
    public static final class MessageStatusType {
        public static final String NEW = "NEW";
        public static final String EDIT = "EDIT";
        public static final String DELETE = "DELETE";
        public static final String TAG = "TAG";
        public static final String REACT = "REACT";
        public static final String ACK = "ACK";
        public static final String READ = "READ";
    }

    // User presence indicators
    public static final class IndicatorStatus {
        public static final String OFFLINE = "OFFLINE";
        public static final String AVAILABLE = "AVAILABLE";
        public static final String IN_MEETING = "IN_MEETING";
        public static final String BUSY = "BUSY";
        public static final String AWAY = "AWAY";
        public static final String ON_LUNCH_BREAK = "ON_LUNCH_BREAK";
    }

    // Read receipt status
    public static final class ReadReceiptsStatus {
        public static final String SINGLE_TICK = "SINGLE_TICK";
        public static final String DOUBLE_TICK = "DOUBLE_TICK";
        public static final String DOUBLE_BLUE_TICK = "DOUBLE_BLUE_TICK";
    }

    // Group icons (16 types)
    public static enum GroupIconEnum {
        TEAM_TYPE_1, TEAM_TYPE_2, /* ... */
    }

    // Group colors (6 options)
    public static enum GroupColorEnum {
        COLOR_1("#FF5733"),
        COLOR_2("#33FF57"),
        // ...
    }
}
```

### Notification_Reminders Constants

**Path:** `Notification_Reminders/src/main/java/com/tse/scheduling/constants/Constants.java`

```java
public final class Constants {

    // Scheduled task routes
    public static final String MEETING_REMINDER = "/meeting/reminder";
    public static final String MEETING_START_CONFIRMATION = "/meeting/startMeetingConfirmation";
    public static final String LEAVE_REMAINING_RESET = "/leave/leaveRemainingReset";
    public static final String LEAVE_MONTHLY_UPDATE = "/leave/leaveRemainingMonthlyUpdate";
    public static final String TIMESHEET_PRE_REMINDER = "/timesheet/timesheetPreReminder";
    public static final String TIMESHEET_POST_REMINDER = "/timesheet/timesheetPostReminder";
    public static final String DEPENDENCY_ALERT = "/alert/dependencyAlert";
    public static final String DELETE_ALERTS = "/alert/deleteAlerts";
    public static final String SPRINT_TASKS_MAIL = "/sprint/sendSprintTasksMail";
    public static final String GEO_FENCE_SHIFT_START = "/geo-fencing/notifyBeforeShiftStart";
    public static final String AUTO_CHECKOUT = "/geo-fencing/autoCheckout";
    public static final String AI_RETRY_REGISTRATION = "/ai/retryFailedUserRegistration";
}
```

---

## Summary

| Category | Classes | Location |
|----------|---------|----------|
| Date/Time | 3 | All modules |
| Common | 2 | TSe_Server, TSEHR |
| Security | 2 | TSe_Server |
| JWT | 3 | TSe_Server, Chat-App |
| File Handling | 3 | TSe_Server, Chat-App |
| Data Converters | 6 | TSe_Server |
| Notifications | 2 | TSe_Server, Chat-App |
| Geo-Fencing | 1 | TSe_Server |
| Other | 3 | TSe_Server |
| Constants | 6 | All modules |
| **Total** | **27** | |
