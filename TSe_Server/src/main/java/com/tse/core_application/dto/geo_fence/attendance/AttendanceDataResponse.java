package com.tse.core_application.dto.geo_fence.attendance;

import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDataResponse {
    // Summary section (for header chips)
    private SummarySection summary;

    // Unified attendance data organized by date, then by user
    private List<DailyAttendanceData> attendanceData;

    /**
     * Summary section for the entire date range and per date.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummarySection {
        private Map<String, DateSummary> perDateSummary; // key: date (yyyy-MM-dd)
        private DateSummary overallSummary; // aggregate for the whole range
    }

    /**
     * Summary for a specific date.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateSummary {
        private Integer totalEmployees;
        private Integer present;
        private Integer absent;
        private Integer onLeave;
        private Integer onHoliday;
        private Integer partiallyPresent;
        private Integer latePresent;
        private Integer alertsCount; // WARN/FAIL events
    }

    /**
     * Attendance data for a specific date.
     * Contains summary for the date and list of all users' attendance.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAttendanceData {
        private String date; // yyyy-MM-dd
        private Boolean isWeekend;
        private DateSummary dateSummary;
        private List<UserAttendanceData> userAttendance; // Sorted by accountId ascending
    }

    /**
     * Complete attendance data for a single user on a specific date.
     * Combines detailed info, status, and full punch timeline.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAttendanceData {
        // Identity
        private Long accountId;
        private String displayName; // From bulk user lookup
        private String email;

        // Status
        private String status; // PRESENT, LATE, PARTIAL, ABSENT, LEAVE, HOLIDAY

        // Times (HH:mm:ss format)
        private String checkInTime;  // null if missing
        private String checkOutTime; // null if missing

        // Totals (in minutes)
        private Integer totalHoursMinutes;
        private Integer totalEffortMinutes;
        private Integer totalBreakMinutes;

        // Breaks
        private List<BreakInterval> breaks;

        // Location
        private String primaryFenceName; // fence/site name for check-in/out

        // Flags/Warnings
        private List<String> flags; // e.g., "Late check-in", "Outside fence", "Integrity warn"

        // Full punch timeline (all events with date+time, sorted chronologically)
        private List<PunchEvent> timeline;
    }

    /**
     * Break interval.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakInterval {
        private String startTime; // HH:mm:ss
        private String endTime;   // HH:mm:ss, null if break not ended
        private Integer durationMinutes;
    }

    /**
     * Punch event in timeline.
     * Includes all event types and missing events.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PunchEvent {
        private Long eventId;
        private String type; // CHECK_IN, CHECK_OUT, BREAK_START, BREAK_END, PUNCHED, MISSING_CHECK_IN, MISSING_CHECK_OUT
        private String dateTime; // yyyy-MM-dd HH:mm:ss (full date and time)
        private String attemptStatus; // SUCCESSFUL, UNSUCCESSFUL, IGNORED, MISSING
        private String locationLabel; // fence/site or "X km away"
        private Double lat;
        private Double lon;
        private Boolean withinFence;
        private String integrityVerdict; // PASS, WARN, FAIL
        private String failReason;
        private EmailFirstLastAccountIdIsActive requestorAccountInfo;
    }
}