package com.tse.core_application.dto.geo_fence.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TodaySummaryResponse {

    @JsonProperty("accountId")
    private Long accountId;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("date")
    private String date; // yyyy-MM-dd

    @JsonProperty("isWeekend")
    private Boolean isWeekend;

    @JsonProperty("isHoliday")
    private Boolean isHoliday;

    @JsonProperty("isOnLeave")
    private Boolean isOnLeave;

    @JsonProperty("leaveName")
    private String leaveName;

    @JsonProperty("status")
    private String status; // PRESENT, LATE, PARTIAL, ABSENT, PENDING, NOT_MARKED, HOLIDAY, LEAVE

    @JsonProperty("checkInTime")
    private String checkInTime; // HH:mm:ss in user timezone

    @JsonProperty("checkOutTime")
    private String checkOutTime; // HH:mm:ss in user timezone

    @JsonProperty("totalHoursMinutes")
    private Integer totalHoursMinutes;

    @JsonProperty("totalEffortMinutes")
    private Integer totalEffortMinutes;

    @JsonProperty("totalBreakMinutes")
    private Integer totalBreakMinutes;

    @JsonProperty("breaks")
    private List<BreakInterval> breaks;

    @JsonProperty("primaryFenceName")
    private String primaryFenceName;

    @JsonProperty("flags")
    private List<String> flags; // e.g., "Late check-in", "Weekend work", "Outside fence"

    @JsonProperty("timeline")
    private List<PunchEvent> timeline; // All punch events including missing events

    /**
     * Break interval.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BreakInterval {
        @JsonProperty("startTime")
        private String startTime; // HH:mm:ss

        @JsonProperty("endTime")
        private String endTime;   // HH:mm:ss, null if break not ended

        @JsonProperty("durationMinutes")
        private Integer durationMinutes;
    }

    /**
     * Punch event in timeline.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PunchEvent {
        @JsonProperty("eventId")
        private Long eventId;

        @JsonProperty("type")
        private String type; // CHECK_IN, CHECK_OUT, BREAK_START, BREAK_END, PUNCHED, MISSING_CHECK_IN, MISSING_CHECK_OUT

        @JsonProperty("dateTime")
        private String dateTime; // yyyy-MM-dd HH:mm:ss in user timezone

        @JsonProperty("attemptStatus")
        private String attemptStatus; // SUCCESSFUL, UNSUCCESSFUL, IGNORED, MISSING

        @JsonProperty("locationLabel")
        private String locationLabel; // fence/site or "X km away"

        @JsonProperty("lat")
        private Double lat;

        @JsonProperty("lon")
        private Double lon;

        @JsonProperty("withinFence")
        private Boolean withinFence;

        @JsonProperty("integrityVerdict")
        private String integrityVerdict; // PASS, WARN, FAIL

        @JsonProperty("failReason")
        private String failReason;

        @JsonProperty("requestorAccountInfo")
        private EmailFirstLastAccountIdIsActive requestorAccountInfo;
    }
}