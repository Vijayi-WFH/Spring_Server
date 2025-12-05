package com.tse.core_application.service.Impl.geo_fencing.attendance;

import com.tse.core_application.constants.DistanceUnitEnum;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.constants.geo_fencing.AttendanceStatus;
import com.tse.core_application.constants.geo_fencing.EventKind;
import com.tse.core_application.custom.model.LeaveTypeAlias;
import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.dto.geo_fence.attendance.AttendanceDataResponse;
import com.tse.core_application.dto.geo_fence.attendance.GeoFenceAttendanceDataRequest;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.exception.geo_fencing.ProblemException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.EntityPreference;
import com.tse.core_application.model.LeaveApplication;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.model.geo_fencing.attendance.AttendanceDay;
import com.tse.core_application.model.geo_fencing.attendance.AttendanceEvent;
import com.tse.core_application.model.geo_fencing.fence.GeoFence;
import com.tse.core_application.model.geo_fencing.policy.AttendancePolicy;
import com.tse.core_application.repository.EntityPreferenceRepository;
import com.tse.core_application.repository.LeaveApplicationRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.repository.UserFeatureAccessRepository;
import com.tse.core_application.repository.geo_fencing.attendance.AttendanceDayRepository;
import com.tse.core_application.repository.geo_fencing.attendance.AttendanceEventRepository;
import com.tse.core_application.repository.geo_fencing.fence.GeoFenceRepository;
import com.tse.core_application.repository.geo_fencing.policy.AttendancePolicyRepository;
import com.tse.core_application.service.Impl.UserFeatureAccessService;
import com.tse.core_application.service.Impl.geo_fencing.fence.GeoFenceService;
import com.tse.core_application.service.Impl.geo_fencing.policy.GeoFencingPolicyService;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.geo_fencing.DistanceConversionUtil;
import com.tse.core_application.utils.geo_fencing.GeoMath;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.Opt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeoFenceAttendanceDataService {
    private static final Logger logger = LoggerFactory.getLogger(GeoFenceAttendanceDataService.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private GeoFencingPolicyService geoFencingPolicyService;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;
    @Autowired
    private UserFeatureAccessService userFeatureAccessService;

    private final AttendanceEventRepository eventRepository;
    private final AttendanceDayRepository dayRepository;
    private final AttendancePolicyRepository policyRepository;
    private final GeoFenceRepository fenceRepository;
    private final EntityPreferenceRepository entityPreferenceRepository;

    public GeoFenceAttendanceDataService(
            AttendanceEventRepository eventRepository,
            AttendanceDayRepository dayRepository,
            AttendancePolicyRepository policyRepository,
            GeoFenceRepository fenceRepository,
            EntityPreferenceRepository entityPreferenceRepository) {
        this.eventRepository = eventRepository;
        this.dayRepository = dayRepository;
        this.policyRepository = policyRepository;
        this.fenceRepository = fenceRepository;
        this.entityPreferenceRepository = entityPreferenceRepository;
    }

    /**
     * Get comprehensive attendance data for given org, date range, and account IDs.
     */
    @Transactional(readOnly = true)
    public AttendanceDataResponse getAttendanceData(GeoFenceAttendanceDataRequest request, String accountId, String userTimeZone) {
        // 1. Validate request
        geoFencingPolicyService.validateOrg (request.getOrgId());
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAttendence(accountIdLong,request.getOrgId());

        if (!geoFencingPolicyService.validateUserRoleAccess (Constants.EntityTypes.ORG, request.getOrgId(), accountIdLong, List.of(RoleEnum.ORG_ADMIN.getRoleId())) && !checkHrAccess) {
            throw new ValidationFailedException("You do not have permission to get attendance of geo fence");
        }

        if (request.getAccountIds() == null || request.getAccountIds().isEmpty()) {
            List<Long> accountIdListOfOrg = userAccountRepository.findAllAccountIdByOrgIdAndIsActive(request.getOrgId(), true);
            if (accountIdListOfOrg == null) {
                return new AttendanceDataResponse();
            }
            request.setAccountIds(accountIdListOfOrg);
        }

        // 2. Parse dates
        LocalDate fromDate = parseDate(request.getFromDate());
        LocalDate toDate = parseDate(request.getToDate());

        if (toDate.isBefore(fromDate)) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE",
                    "Invalid date range",
                    "toDate must be after or equal to fromDate"
            );
        }

        // 3. Load org policy
        AttendancePolicy policy = policyRepository.findByOrgId(request.getOrgId())
                .orElseThrow(() -> new ProblemException(
                        HttpStatus.NOT_FOUND,
                        "POLICY_NOT_FOUND",
                        "Attendance policy not found",
                        "No attendance policy found for org: " + request.getOrgId()
                ));

        // 4. Load user names in bulk (optimization)
        List<EmailFirstLastAccountIdIsActive> emailFirstLastAccountIdIsActiveList = userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountIdIn(request.getAccountIds());
        Map<Long, String> userNamesMap = getUserNamesMap(emailFirstLastAccountIdIsActiveList);
        Map<Long, String> userEmailsMap = getUserEmailsMap(emailFirstLastAccountIdIsActiveList);

        Optional<EntityPreference> optionalEntityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, request.getOrgId());

        // 5. Load all events for the date range and account IDs
        Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap = loadEvents(
                request.getOrgId(), request.getAccountIds(), fromDate, toDate, userTimeZone);

        // 6. Load all attendance days for the date range and account IDs
        Map<Long, Map<LocalDate, AttendanceDay>> daysMap = loadAttendanceDays(
                request.getOrgId(), request.getAccountIds(), fromDate, toDate);

        // 7. Load fences for location labels
        Map<Long, GeoFence> fenceMap = loadFences(request.getOrgId());

        // 8. Build response
        AttendanceDataResponse response = new AttendanceDataResponse();

        // A) Build summary section
        response.setSummary(buildSummarySection(request, fromDate, toDate, eventsMap, daysMap, policy, optionalEntityPreference, userTimeZone));

        // B) Build unified attendance data (sorted by date, then by accountId)
        response.setAttendanceData(buildAttendanceData(request, fromDate, toDate, eventsMap, daysMap, fenceMap, policy, userNamesMap, userEmailsMap, optionalEntityPreference, userTimeZone));

        return response;
    }

    /**
     * Get attendance data for a single user and single date.
     * Used by /today API endpoint. Reuses all the logic from /data API.
     *
     * @param orgId Organization ID
     * @param accountId Account ID
     * @param date Date in yyyy-MM-dd format
     * @param userTimeZone User's timezone
     * @return Single user's attendance data for the specified date
     */
    @Transactional(readOnly = true)
    public AttendanceDataResponse.UserAttendanceData getSingleUserAttendanceData(
            Long orgId, Long accountId, String date, String userTimeZone, Optional<EntityPreference> optionalEntityPreference) {

        // Parse date
        LocalDate targetDate = parseDate(date);

        // Load policy
        AttendancePolicy policy = policyRepository.findByOrgId(orgId)
                .orElseThrow(() -> new ProblemException(
                        HttpStatus.NOT_FOUND,
                        "POLICY_NOT_FOUND",
                        "Attendance policy not found",
                        "No attendance policy found for org: " + orgId
                ));

        // Load events for this user and date (timezone-aware)
        Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap = loadEvents(
                orgId, Collections.singletonList(accountId), targetDate, targetDate, userTimeZone);

        // Load attendance day for this user and date
        Map<Long, Map<LocalDate, AttendanceDay>> daysMap = loadAttendanceDays(
                orgId, Collections.singletonList(accountId), targetDate, targetDate);

        // Load fences for location labels
        Map<Long, GeoFence> fenceMap = loadFences(orgId);

        // Get user name
        List<EmailFirstLastAccountIdIsActive> emailFirstLastAccountIdIsActiveList = userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountIdIn(List.of(accountId));
        Map<Long, String> userNamesMap = getUserNamesMap(emailFirstLastAccountIdIsActiveList);
        Map<Long, String> userEmailsMap = getUserEmailsMap(emailFirstLastAccountIdIsActiveList);

        // Build user attendance data using existing logic
        return buildUserAttendanceData(
                orgId, accountId, targetDate, eventsMap, daysMap, fenceMap, policy, userNamesMap, userEmailsMap, optionalEntityPreference, userTimeZone);
    }

    /**
     * Bulk user name resolver to avoid N queries.
     * Returns map of accountId -> displayName.
     */
    private Map<Long, String> getUserNamesMap(List<EmailFirstLastAccountIdIsActive> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }

        return list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        EmailFirstLastAccountIdIsActive::getAccountId,
                        dto -> {
                            String first = dto.getFirstName() != null ? dto.getFirstName().trim() : "";
                            String last  = dto.getLastName() != null ? dto.getLastName().trim() : "";
                            return (first + " " + last).trim();
                        },
                        (a, b) -> a, // in case of duplicates, keep first
                        LinkedHashMap::new
                ));
    }

    private Map<Long, String> getUserEmailsMap(List<EmailFirstLastAccountIdIsActive> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }

        return list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        EmailFirstLastAccountIdIsActive::getAccountId,
                        EmailFirstLastAccountIdIsActive::getEmail,
                        (a, b) -> a, // in case of duplicates, keep first
                        LinkedHashMap::new
                ));
    }


//    private void validateRequest(GeoFenceAttendanceDataRequest request) {
//        if (request.getOrgId() == null || request.getOrgId() <= 0) {
//            throw new ProblemException(
//                    HttpStatus.BAD_REQUEST,
//                    "INVALID_ORG_ID",
//                    "Invalid orgId",
//                    "orgId is required and must be positive"
//            );
//        }
//
//        if (request.getAccountIds() == null || request.getAccountIds().isEmpty()) {
//            throw new ProblemException(
//                    HttpStatus.BAD_REQUEST,
//                    "INVALID_ACCOUNT_IDS",
//                    "Invalid accountIds",
//                    "At least one accountId is required"
//            );
//        }
//    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_FORMAT",
                    "Invalid date format",
                    "Date must be in format yyyy-MM-dd: " + dateStr
            );
        }
    }

    private Map<Long, Map<LocalDate, List<AttendanceEvent>>> loadEvents(
            Long orgId, List<Long> accountIds, LocalDate fromDate, LocalDate toDate, String userTimeZone) {
        Map<Long, Map<LocalDate, List<AttendanceEvent>>> result = new HashMap<>();

        // Convert user's date range to server timezone
        // User wants events from 00:00:00 to 23:59:59 in THEIR timezone
        LocalDateTime userStartOfDay = fromDate.atStartOfDay();
        LocalDateTime serverStart = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(
                userStartOfDay, userTimeZone);

        LocalDateTime userEndOfDay = toDate.plusDays(1).atStartOfDay();
        LocalDateTime serverEnd = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(
                userEndOfDay, userTimeZone);

        for (Long accountId : accountIds) {
            List<AttendanceEvent> events = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                    orgId, accountId, serverStart, serverEnd);

            Map<LocalDate, List<AttendanceEvent>> dateMap = events.stream()
                    .collect(Collectors.groupingBy(e -> {
                        // Convert server timestamp to user's timezone to get correct date
                        LocalDateTime userDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                                e.getTsUtc(), userTimeZone);
                        return userDateTime.toLocalDate();
                    }));

            result.put(accountId, dateMap);
        }

        return result;
    }

    private Map<Long, Map<LocalDate, AttendanceDay>> loadAttendanceDays(
            Long orgId, List<Long> accountIds, LocalDate fromDate, LocalDate toDate) {
        Map<Long, Map<LocalDate, AttendanceDay>> result = new HashMap<>();

        for (Long accountId : accountIds) {
            List<AttendanceDay> days = dayRepository.findByOrgIdAndAccountIdAndDateKeyBetween(
                    orgId, accountId, fromDate, toDate);

            Map<LocalDate, AttendanceDay> dateMap = days.stream()
                    .collect(Collectors.toMap(AttendanceDay::getDateKey, d -> d));

            result.put(accountId, dateMap);
        }

        return result;
    }

    private Map<Long, GeoFence> loadFences(Long orgId) {
        List<GeoFence> fences = fenceRepository.findByOrgId(orgId);
        return fences.stream()
                .collect(Collectors.toMap(GeoFence::getId, f -> f));
    }

    private AttendanceDataResponse.SummarySection buildSummarySection(
            GeoFenceAttendanceDataRequest request, LocalDate fromDate, LocalDate toDate,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            AttendancePolicy policy, Optional<EntityPreference> optionalEntityPreference,
            String userTimeZone) {

        Map<String, AttendanceDataResponse.DateSummary> perDateSummary = new HashMap<>();
        AttendanceDataResponse.DateSummary overallSummary = new AttendanceDataResponse.DateSummary();

        int overallPresent = 0, overallAbsent = 0, overallOnLeave = 0;
        int overallOnHoliday = 0, overallPartial = 0, overallLate = 0, overallAlerts = 0;

        LocalDate currentDate = fromDate;
        while (!currentDate.isAfter(toDate)) {
            AttendanceDataResponse.DateSummary dateSummary = computeDateSummary(
                    request, currentDate, eventsMap, daysMap, policy, optionalEntityPreference, userTimeZone);
            perDateSummary.put(currentDate.format(DATE_FORMATTER), dateSummary);

            overallPresent += dateSummary.getPresent();
            overallAbsent += dateSummary.getAbsent();
            overallOnLeave += dateSummary.getOnLeave();
            overallOnHoliday += dateSummary.getOnHoliday();
            overallPartial += dateSummary.getPartiallyPresent();
            overallLate += dateSummary.getLatePresent();
            overallAlerts += dateSummary.getAlertsCount();

            currentDate = currentDate.plusDays(1);
        }

        overallSummary.setTotalEmployees(request.getAccountIds().size());
        overallSummary.setPresent(overallPresent);
        overallSummary.setAbsent(overallAbsent);
        overallSummary.setOnLeave(overallOnLeave);
        overallSummary.setOnHoliday(overallOnHoliday);
        overallSummary.setPartiallyPresent(overallPartial);
        overallSummary.setLatePresent(overallLate);
        overallSummary.setAlertsCount(overallAlerts);

        AttendanceDataResponse.SummarySection summary = new AttendanceDataResponse.SummarySection();
        summary.setPerDateSummary(perDateSummary);
        summary.setOverallSummary(overallSummary);

        return summary;
    }

    private AttendanceDataResponse.DateSummary computeDateSummary(
            GeoFenceAttendanceDataRequest request, LocalDate date,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            AttendancePolicy policy,
            Optional<EntityPreference> optionalEntityPreference,
            String userTimeZone) {

        int present = 0, absent = 0, onLeave = 0, onHoliday = 0, partial = 0, late = 0, alerts = 0;

        for (Long accountId : request.getAccountIds()) {
            HolidayInfo holidayInfo = getHolidayInfo(request.getOrgId(), date, accountId, optionalEntityPreference);

            if (holidayInfo.isWeekend()) {
                // Weekend - skip from counts
                continue;
            }

            if (holidayInfo.isPublicHoliday()) {
                onHoliday++;
                continue;
            }

            if (holidayInfo.isOnLeave()) {
                onLeave++;
                continue;
            }

            List<AttendanceEvent> events = eventsMap.getOrDefault(accountId, Collections.emptyMap())
                    .getOrDefault(date, Collections.emptyList());
            AttendanceDay day = daysMap.getOrDefault(accountId, Collections.emptyMap()).get(date);

            String status = determineStatus(date, events, day, policy, optionalEntityPreference, userTimeZone, request.getOrgId());

            switch (status) {
                case "PRESENT":
                    present++;
                    break;
                case "LATE":
                    late++;
                    break;
                case "PARTIAL":
                    partial++;
                    break;
                case "ABSENT":
                    absent++;
                    break;
                case "PENDING":
                    // Today before office hours - don't count in any category yet
                    break;
                case "NOT_MARKED":
                    // Future date - don't count in any category
                    break;
            }

            // Count alerts
            long eventAlerts = events.stream()
                    .filter(e -> !e.getSuccess() || "WARN".equals(e.getVerdict().name()) || "FAIL".equals(e.getVerdict().name()))
                    .count();
            alerts += eventAlerts;
        }

        AttendanceDataResponse.DateSummary summary = new AttendanceDataResponse.DateSummary();
        summary.setTotalEmployees(request.getAccountIds().size());
        summary.setPresent(present);
        summary.setAbsent(absent);
        summary.setOnLeave(onLeave);
        summary.setOnHoliday(onHoliday);
        summary.setPartiallyPresent(partial);
        summary.setLatePresent(late);
        summary.setAlertsCount(alerts);

        return summary;
    }

    /**
     * Build unified attendance data organized by date (ascending), then by user (ascending by accountId).
     */
    private List<AttendanceDataResponse.DailyAttendanceData> buildAttendanceData(
            GeoFenceAttendanceDataRequest request, LocalDate fromDate, LocalDate toDate,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy,
            Map<Long, String> userNamesMap,
            Map<Long, String> userEmailsMap,
            Optional<EntityPreference> optionalEntityPreference,
            String userTimeZone) {

        List<AttendanceDataResponse.DailyAttendanceData> attendanceData = new ArrayList<>();

        // Iterate through dates in ascending order
        LocalDate currentDate = fromDate;
        while (!currentDate.isAfter(toDate)) {
            AttendanceDataResponse.DailyAttendanceData dailyData = new AttendanceDataResponse.DailyAttendanceData();
            dailyData.setDate(currentDate.format(DATE_FORMATTER));

            // Check if this date is a weekend for the org
            // We can check using any accountId since weekend is org-level
            HolidayInfo dateHolidayInfo = getHolidayInfo(request.getOrgId(), currentDate,
                    request.getAccountIds().isEmpty() ? 0L : request.getAccountIds().get(0), optionalEntityPreference);
            dailyData.setIsWeekend(dateHolidayInfo.isWeekend());

            // Compute date summary
            AttendanceDataResponse.DateSummary dateSummary = computeDateSummary(
                    request, currentDate, eventsMap, daysMap, policy, optionalEntityPreference, userTimeZone);
            dailyData.setDateSummary(dateSummary);

            // Build user attendance list (sorted by accountId)
            List<AttendanceDataResponse.UserAttendanceData> userAttendanceList = new ArrayList<>();

            // Sort accountIds in ascending order
            List<Long> sortedAccountIds = new ArrayList<>(request.getAccountIds());
            Collections.sort(sortedAccountIds);

            for (Long accountId : sortedAccountIds) {
                AttendanceDataResponse.UserAttendanceData userAttendance = buildUserAttendanceData(
                        request.getOrgId(), accountId, currentDate,
                        eventsMap, daysMap, fenceMap, policy, userNamesMap, userEmailsMap, optionalEntityPreference, userTimeZone);

                // Only add non-weekend entries
                if (userAttendance != null) {
                    userAttendanceList.add(userAttendance);
                }
            }

            if (!userAttendanceList.isEmpty()) {
                userAttendanceList.sort(
                        Comparator
                                .comparing(
                                        AttendanceDataResponse.UserAttendanceData::getDisplayName,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                                )
                                .thenComparing(
                                        AttendanceDataResponse.UserAttendanceData::getEmail,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                                )
                );
            }
            dailyData.setUserAttendance(userAttendanceList);
            attendanceData.add(dailyData);

            currentDate = currentDate.plusDays(1);
        }

        return attendanceData;
    }

    /**
     * Build complete attendance data for a single user on a specific date.
     */
    private AttendanceDataResponse.UserAttendanceData buildUserAttendanceData(
            Long orgId, Long accountId, LocalDate date,
            Map<Long, Map<LocalDate, List<AttendanceEvent>>> eventsMap,
            Map<Long, Map<LocalDate, AttendanceDay>> daysMap,
            Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy,
            Map<Long, String> userNamesMap,
            Map<Long, String> userEmailsMap,
            Optional<EntityPreference> optionalEntityPreference,
            String userTimeZone) {

        List<AttendanceEvent> events = eventsMap.getOrDefault(accountId, Collections.emptyMap())
                .getOrDefault(date, Collections.emptyList());
        AttendanceDay day = daysMap.getOrDefault(accountId, Collections.emptyMap()).get(date);


        // STEP 2: Get special day information
        HolidayInfo holidayInfo = getHolidayInfo(orgId, date, accountId, optionalEntityPreference);
        boolean hasEvents = !events.isEmpty();

        // STEP 3: If NO events on special day → return with special status
        if (!hasEvents) {
            AttendanceDataResponse.UserAttendanceData userData = new AttendanceDataResponse.UserAttendanceData();
            userData.setAccountId(accountId);
            userData.setDisplayName(userNamesMap.getOrDefault(accountId, "User " + accountId));
            userData.setEmail(userEmailsMap.getOrDefault(accountId, "No email"));
            // Skip weekends with no activity (don't show in list)
            if (holidayInfo.isWeekend()) {
                return null;
            }

            // Public holiday with no work
            if (holidayInfo.isPublicHoliday()) {
                userData.setStatus("HOLIDAY");
                userData.setCheckInTime(null);
                userData.setCheckOutTime(null);
                userData.setTotalHoursMinutes(0);
                userData.setTotalEffortMinutes(0);
                userData.setTotalBreakMinutes(0);
                userData.setBreaks(Collections.emptyList());
                userData.setPrimaryFenceName(null);
                userData.setFlags(Collections.emptyList());
                userData.setTimeline(Collections.emptyList());
                return userData;
            }

            // On leave with no work
            if (holidayInfo.isOnLeave()) {
                userData.setStatus("LEAVE");
                userData.setCheckInTime(null);
                userData.setCheckOutTime(null);
                userData.setTotalHoursMinutes(0);
                userData.setTotalEffortMinutes(0);
                userData.setTotalBreakMinutes(0);
                userData.setBreaks(Collections.emptyList());
                userData.setPrimaryFenceName(null);
                String leaveLabel = "On Leave: " + (holidayInfo.getLeaveName() != null ? holidayInfo.getLeaveName() : "Approved");
                userData.setFlags(Collections.singletonList(leaveLabel));
                userData.setTimeline(Collections.emptyList());
                return userData;
            }
        }

        // STEP 4: If HAS events → Process normally (regardless of special day)
        // This handles cases where user worked on weekend/holiday/leave
        AttendanceDataResponse.UserAttendanceData userData = new AttendanceDataResponse.UserAttendanceData();
        userData.setAccountId(accountId);
        userData.setDisplayName(userNamesMap.getOrDefault(accountId, "User " + accountId));
        userData.setEmail(userEmailsMap.getOrDefault(accountId, "No email"));

            // Find check-in and check-out events
        AttendanceEvent checkInEvent = findSuccessfulEvent(events, EventKind.CHECK_IN);
        AttendanceEvent checkOutEvent = findSuccessfulEvent(events, EventKind.CHECK_OUT);

        // Convert check-in and check-out times to user timezone
        if (checkInEvent != null) {
            LocalDateTime userCheckInDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                    checkInEvent.getTsUtc(), userTimeZone);
            userData.setCheckInTime(userCheckInDateTime.toLocalTime().format(TIME_FORMATTER));
        } else {
            userData.setCheckInTime(null);
        }

        if (checkOutEvent != null) {
            LocalDateTime userCheckOutDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                    checkOutEvent.getTsUtc(), userTimeZone);
            userData.setCheckOutTime(userCheckOutDateTime.toLocalTime().format(TIME_FORMATTER));
        } else {
            userData.setCheckOutTime(null);
        }

        // Breaks
        List<AttendanceDataResponse.BreakInterval> breaks = extractBreaks(events, optionalEntityPreference, userTimeZone, orgId, date);
        int totalBreakMinutes = breaks.stream()
                .mapToInt(AttendanceDataResponse.BreakInterval::getDurationMinutes)
                .sum();
        userData.setTotalBreakMinutes(totalBreakMinutes);
        userData.setBreaks(breaks);

        // Totals
        if (day != null) {
            userData.setTotalHoursMinutes(day.getWorkedSeconds() / 60);
            userData.setTotalEffortMinutes((day.getWorkedSeconds() - day.getBreakSeconds()) / 60);
        } else {
            userData.setTotalHoursMinutes(0);
            userData.setTotalEffortMinutes(0);
        }

        // Location
        if (checkInEvent != null && checkInEvent.getFenceId() != null) {
            GeoFence fence = fenceMap.get(checkInEvent.getFenceId());
            userData.setPrimaryFenceName(fence != null ? fence.getName() : "Unknown");
        } else {
            userData.setPrimaryFenceName(null);
        }

        // Status
        String status = determineStatus(date, events, day, policy, optionalEntityPreference, userTimeZone, orgId);
        userData.setStatus(status);

        // Flags
        List<String> flags = extractFlags(events, checkInEvent, checkOutEvent, policy, holidayInfo, optionalEntityPreference, userTimeZone);
        userData.setFlags(flags);

        // Timeline (all punches with date+time, sorted chronologically)
        List<AttendanceDataResponse.PunchEvent> timeline = buildTimeline(
                events, fenceMap, policy, date, checkInEvent, checkOutEvent, optionalEntityPreference, userTimeZone, orgId);
        userData.setTimeline(timeline);

        return userData;
    }

    private List<AttendanceDataResponse.PunchEvent> buildTimeline(
            List<AttendanceEvent> events, Map<Long, GeoFence> fenceMap,
            AttendancePolicy policy, LocalDate date,
            AttendanceEvent checkInEvent, AttendanceEvent checkOutEvent,
            Optional<EntityPreference> optionalEntityPreference,
            String userTimeZone, Long orgId) {

        List<AttendanceDataResponse.PunchEvent> timeline = new ArrayList<>();

        // Determine if this date is today, past, or future in user's timezone
        LocalDate todayInUserTZ = LocalDate.now(ZoneId.of(userTimeZone));
        boolean isToday = date.equals(todayInUserTZ);
        boolean isFuture = date.isAfter(todayInUserTZ);

        // Get office hours for today's logic
        LocalTime officeStartTime = optionalEntityPreference.isPresent() ? optionalEntityPreference.get().getOfficeHrsStartTime() : Constants.OFFICE_START_TIME.toLocalTime();
        LocalTime officeEndTime = optionalEntityPreference.isPresent() ? optionalEntityPreference.get().getOfficeHrsEndTime() : Constants.OFFICE_END_TIME.toLocalTime();
        LocalTime currentTimeInUserTZ = isToday ? LocalTime.now(ZoneId.of(userTimeZone)) : null;

        // Add actual events (sorted by timestamp)
        for (AttendanceEvent event : events) {
            AttendanceDataResponse.PunchEvent punchEvent = new AttendanceDataResponse.PunchEvent();
            punchEvent.setEventId(event.getId());
            punchEvent.setType(event.getEventKind().name());
            // Convert server timestamp to user timezone
            LocalDateTime userDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                    event.getTsUtc(), userTimeZone);
            punchEvent.setDateTime(userDateTime.format(DATETIME_FORMATTER));
            punchEvent.setAttemptStatus(event.getSuccess() ? "SUCCESSFUL" : "UNSUCCESSFUL");

            // Location label
            if (event.getFenceId() != null) {
                GeoFence fence = fenceMap.get(event.getFenceId());
                if (fence != null && event.getLat() != null && event.getLon() != null) {
                    double distance = GeoMath.distanceMeters(
                            event.getLat(), event.getLon(),
                            fence.getCenterLat(), fence.getCenterLng()
                    );
                    if (Boolean.TRUE.equals(event.getUnderRange())) {
                        punchEvent.setLocationLabel(fence.getName());
                    } else {
                        // Fetch org's distance unit preference
                        DistanceUnitEnum distanceUnit = getDistanceUnitForOrg(orgId);
                        String formattedDistance = DistanceConversionUtil.formatDistanceLabel(distance, distanceUnit, fence.getName());
                        punchEvent.setLocationLabel(formattedDistance);
                    }
                } else {
                    punchEvent.setLocationLabel("Unknown");
                }
            } else {
                punchEvent.setLocationLabel("No fence assigned");
            }

            punchEvent.setLat(event.getLat());
            punchEvent.setLon(event.getLon());
            punchEvent.setWithinFence(event.getUnderRange());
            punchEvent.setIntegrityVerdict(event.getVerdict() != null ? event.getVerdict().name() : "UNKNOWN");
            punchEvent.setFailReason(event.getFailReason());

            // Populate requestor account info if this punch was requested by someone
            if (event.getRequesterAccountId() != null) {
                try {
                    EmailFirstLastAccountIdIsActive requestorInfo =
                            userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(event.getRequesterAccountId());
                    punchEvent.setRequestorAccountInfo(requestorInfo);
                } catch (Exception e) {
//                    logger.warn("Failed to fetch requestor account info for accountId: " + event.getRequesterAccountId(), e);
                    // Continue without requestor info if fetch fails
                }
            }

            timeline.add(punchEvent);
        }

        // Add missing check-in event if needed
        // Logic:
        // - Future dates: Never show missing check-in
        // - Today: Only show if current time has passed office start time
        // - Past dates: Always show if missing
        boolean shouldShowMissingCheckIn = checkInEvent == null &&
                !isFuture && // Don't show for future dates
                (!isToday || (currentTimeInUserTZ != null && currentTimeInUserTZ.isAfter(officeStartTime)));

        if (shouldShowMissingCheckIn && !events.isEmpty()) {
            AttendanceDataResponse.PunchEvent missingCheckIn = new AttendanceDataResponse.PunchEvent();
            missingCheckIn.setEventId(null);
            missingCheckIn.setType("MISSING_CHECK_IN");
//            missingCheckIn.setDateTime(date.format(DATE_FORMATTER) + " --:--:--");
            missingCheckIn.setDateTime(date.format(DATE_FORMATTER) + " " + officeStartTime.format(TIME_FORMATTER));
            missingCheckIn.setAttemptStatus("MISSING");
            missingCheckIn.setLocationLabel("No check-in recorded");
            missingCheckIn.setLat(null);
            missingCheckIn.setLon(null);
            missingCheckIn.setWithinFence(null);
            missingCheckIn.setIntegrityVerdict(null);
            missingCheckIn.setFailReason("User did not check in");
            timeline.add(0, missingCheckIn);
        } else if (shouldShowMissingCheckIn && events.isEmpty()) {
            // No events at all - add missing check-in
            AttendanceDataResponse.PunchEvent missingCheckIn = new AttendanceDataResponse.PunchEvent();
            missingCheckIn.setEventId(null);
            missingCheckIn.setType("MISSING_CHECK_IN");
//            missingCheckIn.setDateTime(date.format(DATE_FORMATTER) + " --:--:--");
            missingCheckIn.setDateTime(date.format(DATE_FORMATTER) + " " + officeStartTime.format(TIME_FORMATTER));
            missingCheckIn.setAttemptStatus("MISSING");
            missingCheckIn.setLocationLabel("No check-in recorded");
            missingCheckIn.setLat(null);
            missingCheckIn.setLon(null);
            missingCheckIn.setWithinFence(null);
            missingCheckIn.setIntegrityVerdict(null);
            missingCheckIn.setFailReason("User did not check in");
            timeline.add(missingCheckIn);
        }

        // Add missing check-out event if needed
        // Logic:
        // - Future dates: Never show missing check-out
        // - Today: Only show if current time has passed office end time AND check-in exists
        // - Past dates: Always show if missing (and check-in exists)
        boolean shouldShowMissingCheckOut = checkInEvent != null && checkOutEvent == null &&
                !isFuture && // Don't show for future dates
                (!isToday || (currentTimeInUserTZ != null && currentTimeInUserTZ.isAfter(officeEndTime)));

        if (shouldShowMissingCheckOut) {
            // Check-in exists but no check-out
            AttendanceDataResponse.PunchEvent missingCheckOut = new AttendanceDataResponse.PunchEvent();
            missingCheckOut.setEventId(null);
            missingCheckOut.setType("MISSING_CHECK_OUT");
//            missingCheckOut.setDateTime(date.format(DATE_FORMATTER) + " --:--:--");
            missingCheckOut.setDateTime(date.format(DATE_FORMATTER) + " " + officeEndTime.format(TIME_FORMATTER));
            missingCheckOut.setAttemptStatus("MISSING");
            missingCheckOut.setLocationLabel("No check-out recorded");
            missingCheckOut.setLat(null);
            missingCheckOut.setLon(null);
            missingCheckOut.setWithinFence(null);
            missingCheckOut.setIntegrityVerdict(null);
            missingCheckOut.setFailReason("User did not check out (auto check-out may have failed)");
            timeline.add(missingCheckOut);
        }

        return timeline;
    }

    private AttendanceEvent findSuccessfulEvent(List<AttendanceEvent> events, EventKind kind) {
        return events.stream()
                .filter(e -> e.getEventKind() == kind && e.getSuccess())
                .findFirst()
                .orElse(null);
    }

    private List<AttendanceDataResponse.BreakInterval> extractBreaks(List<AttendanceEvent> events, Optional<EntityPreference> optionalEntityPreference, String userTimeZone,
                                                                        Long orgId, LocalDate date) {
        List<AttendanceDataResponse.BreakInterval> breaks = new ArrayList<>();

        AttendanceEvent breakStart = null;
        for (AttendanceEvent event : events) {
            if (event.getEventKind() == EventKind.BREAK_START && event.getSuccess()) {
                breakStart = event;
            } else if (event.getEventKind() == EventKind.BREAK_END && event.getSuccess() && breakStart != null) {
                AttendanceDataResponse.BreakInterval breakInterval = new AttendanceDataResponse.BreakInterval();

                // Convert break times to user timezone
                LocalDateTime userBreakStartDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                        breakStart.getTsUtc(), userTimeZone);
                breakInterval.setStartTime(userBreakStartDateTime.toLocalTime().format(TIME_FORMATTER));

                LocalDateTime userBreakEndDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                        event.getTsUtc(), userTimeZone);
                breakInterval.setEndTime(userBreakEndDateTime.toLocalTime().format(TIME_FORMATTER));

                long durationMinutes = ChronoUnit.MINUTES.between(breakStart.getTsUtc(), event.getTsUtc());
                breakInterval.setDurationMinutes((int) durationMinutes);

                breaks.add(breakInterval);
                breakStart = null;
            }
        }

        // If break start exists but no end, add incomplete break

        if (breakStart != null) {
            AttendanceDataResponse.BreakInterval breakInterval = new AttendanceDataResponse.BreakInterval();
            LocalDateTime userBreakStartDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                    breakStart.getTsUtc(), userTimeZone);
            breakInterval.setStartTime(userBreakStartDateTime.toLocalTime().format(TIME_FORMATTER));

            // Assume break ended at office end time
            LocalTime officeEndTime = optionalEntityPreference.isPresent() ? optionalEntityPreference.get().getOfficeHrsEndTime() : Constants.OFFICE_END_TIME.toLocalTime();

            // Calculate estimated duration from break start to office end time
            LocalDateTime officeEndDateTime = date.atTime(officeEndTime);
            LocalDateTime serverOfficeEndDateTime = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(
                    officeEndDateTime, userTimeZone);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentDateTimeInUserTimeZone = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds (now, userTimeZone);
            LocalDateTime breakEndDateTime = null;
            if (currentDateTimeInUserTimeZone.isBefore(officeEndDateTime)) {
                breakInterval.setEndTime(currentDateTimeInUserTimeZone.toLocalTime().format(TIME_FORMATTER));
                breakEndDateTime = now;
            }
            else {
                breakInterval.setEndTime(officeEndTime.format(TIME_FORMATTER));
                breakEndDateTime = serverOfficeEndDateTime;
            }
            long estimatedDurationMinutes = ChronoUnit.MINUTES.between(breakStart.getTsUtc(), breakEndDateTime);
            breakInterval.setDurationMinutes((int) Math.max(0, estimatedDurationMinutes));

            breaks.add(breakInterval);
        }

        return breaks;
    }

    private String determineStatus(LocalDate date, List<AttendanceEvent> events, AttendanceDay day, AttendancePolicy policy, Optional<EntityPreference> optionalEntityPreference, String userTimeZone, Long orgId) {
        AttendanceEvent checkInEvent = findSuccessfulEvent(events, EventKind.CHECK_IN);
        AttendanceEvent checkOutEvent = findSuccessfulEvent(events, EventKind.CHECK_OUT);

        // Determine if this date is today or future in user's timezone
        LocalDate todayInUserTZ = LocalDate.now(ZoneId.of(userTimeZone));
        boolean isToday = date.equals(todayInUserTZ);
        boolean isFuture = date.isAfter(todayInUserTZ);

        // Absent if no check-in
        if (checkInEvent == null) {
            // For future dates: Don't mark as absent (attendance can't be marked yet)
            if (isFuture) {
                return "NOT_MARKED"; // Future date - no attendance expected
            }

            // For today: Only mark as absent if office start time has passed
            if (isToday) {
                LocalTime currentTimeInUserTZ = LocalTime.now(ZoneId.of(userTimeZone));
                LocalTime officeStartTime = optionalEntityPreference.isPresent() ? optionalEntityPreference.get().getOfficeHrsStartTime() : Constants.OFFICE_START_TIME.toLocalTime();

                // If current time is before office start, don't mark as absent yet
                if (currentTimeInUserTZ.isBefore(officeStartTime)) {
                    return "PENDING"; // Before office hours - status pending
                }
            }

            // For past dates or today after office start: Mark as absent
            return "ABSENT";
        }

        // Late if check-in is late
        if (isLateCheckIn(checkInEvent, policy, optionalEntityPreference, userTimeZone)) {
            return "LATE";
        }

        // Partial if no check-out or insufficient effort
        if (checkOutEvent == null) {
            // For today: Only mark as partial if office end time has passed
            if (isToday) {
                LocalTime currentTimeInUserTZ = LocalTime.now(ZoneId.of(userTimeZone));
                LocalTime officeEndTime = optionalEntityPreference.isPresent() ? optionalEntityPreference.get().getOfficeHrsEndTime() : Constants.OFFICE_END_TIME.toLocalTime();

                // If current time is before office end, status is still in progress
                if (currentTimeInUserTZ.isBefore(officeEndTime)) {
                    return "PRESENT"; // During work hours - consider as present
                }
            }

            return "PARTIAL";
        }

        if (day != null) {
            int effortMinutes = (day.getWorkedSeconds() - day.getBreakSeconds()) / 60;
            int expectedMinutes = policy.getMaxWorkingHoursPerDay() * 60;
            if (effortMinutes < expectedMinutes * 0.8) { // Less than 80% of expected
                return "PARTIAL";
            }
        }

        return "PRESENT";
    }

    private boolean isLateCheckIn(AttendanceEvent checkInEvent, AttendancePolicy policy, Optional<EntityPreference> optionalEntityPreference, String userTimeZone) {
        // TODO: Implement proper late check-in logic based on office start time
        // For now, assume office starts at 9:00 AM
        Boolean isLateCheckIn = null;
        if (optionalEntityPreference.isPresent()) {
            LocalTime officeStartTime = optionalEntityPreference.get().getOfficeHrsStartTime();
            LocalDateTime userCheckInDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                    checkInEvent.getTsUtc(), userTimeZone);
            LocalTime checkInTime = userCheckInDateTime.toLocalTime();
            LocalTime lateThreshold = officeStartTime.plusMinutes(policy.getLateCheckinAfterStartMin());

            isLateCheckIn = checkInTime.isAfter(lateThreshold);
        }
        else {
            LocalTime officeStartTime = Constants.OFFICE_START_TIME.toLocalTime();
            LocalDateTime userCheckInDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(
                    checkInEvent.getTsUtc(), userTimeZone);
            LocalTime checkInTime = userCheckInDateTime.toLocalTime();
            LocalTime lateThreshold = officeStartTime.plusMinutes(policy.getLateCheckinAfterStartMin());

            isLateCheckIn = checkInTime.isAfter(lateThreshold);
        }
        return isLateCheckIn;
    }

    private List<String> extractFlags(List<AttendanceEvent> events, AttendanceEvent checkInEvent, AttendanceEvent checkOutEvent, AttendancePolicy policy, HolidayInfo holidayInfo,
                                        Optional<EntityPreference> optionalEntityPreference, String userTimeZone) {
        List<String> flags = new ArrayList<>();

        // CONTEXTUAL FLAGS FIRST (informational about special circumstances)
        if (!events.isEmpty()) {
            // Weekend work
            if (holidayInfo.isWeekend()) {
                flags.add("Weekend work");
            }

            // Public holiday work
            if (holidayInfo.isPublicHoliday()) {
                String holidayName = holidayInfo.getLeaveName();
                if (holidayName != null && !holidayName.isEmpty()) {
                    flags.add("Worked on Holiday: " + holidayName);
                } else {
                    flags.add("Worked on Holiday");
                }
            }

            // On leave but attended
            if (holidayInfo.isOnLeave()) {
                String leaveName = holidayInfo.getLeaveName();
                if (leaveName != null && !leaveName.isEmpty()) {
                    flags.add("On Leave: " + leaveName);
                } else {
                    flags.add("On Leave: Approved");
                }
            }
        }

        if (checkInEvent != null) {
            if (isLateCheckIn(checkInEvent, policy, optionalEntityPreference, userTimeZone)) {
                flags.add("Late check-in");
            }
            if (!Boolean.TRUE.equals(checkInEvent.getUnderRange())) {
                flags.add("Outside fence at check-in");
            }
            if (checkInEvent.getVerdict() != null &&
                    ("WARN".equals(checkInEvent.getVerdict().name()) || "FAIL".equals(checkInEvent.getVerdict().name()))) {
                flags.add("Integrity warning at check-in");
            }
        }

        if (checkOutEvent != null) {
            if (!Boolean.TRUE.equals(checkOutEvent.getUnderRange())) {
                flags.add("Outside fence at check-out");
            }
            if (checkOutEvent.getVerdict() != null &&
                    ("WARN".equals(checkOutEvent.getVerdict().name()) || "FAIL".equals(checkOutEvent.getVerdict().name()))) {
                flags.add("Integrity warning at check-out");
            }
        }

        // Check for unsuccessful attempts
        long unsuccessfulAttempts = events.stream()
                .filter(e -> !e.getSuccess())
                .count();
        if (unsuccessfulAttempts > 0) {
            flags.add(unsuccessfulAttempts + " unsuccessful attempts");
        }

        return flags;
    }

    public HolidayInfo getHolidayInfo(Long orgId, LocalDate date, Long accountId, Optional<EntityPreference> optionalEntityPreference) {
        HolidayInfo info = new HolidayInfo();

        if (optionalEntityPreference.isPresent()) {
            EntityPreference entityPreference = optionalEntityPreference.get();
            // Weekend/off-days: store is typically 1..7 (Mon..Sun). Use ints directly.
            List<Integer> offDays = entityPreference.getOffDays();
            if (offDays != null && offDays.contains(date.getDayOfWeek().getValue())) {
                info.setWeekend(true);
                return info;
            }

            // Public holidays on exact date and active
            if (entityPreference.getHolidayOffDays() != null) {
                boolean publicHoliday = entityPreference.getHolidayOffDays().stream()
                        .anyMatch(h -> h.isActive() && date.equals(h.getDate()));
                if (publicHoliday) {
                    info.setPublicHoliday(true);
                    info.setLeaveName(AttendanceStatus.HOLIDAY.name());
                    return info;
                }
            }
        }

        // Personal leave
        if (accountId != null && optionalEntityPreference.isPresent()) {

            List<LeaveApplication> leaveApplication = leaveApplicationRepository.findByAccountIdAndDate(accountId, date, List.of(
                    Constants.LeaveApplicationStatusIds.APPROVED_LEAVE_APPLICATION_STATUS_ID,
                    Constants.LeaveApplicationStatusIds.CONSUMED_LEAVE_APPLICATION_STATUS_ID
            ));
            if (leaveApplication != null && !leaveApplication.isEmpty()) {
                if (Objects.equals(Constants.TIME_OFF_LEAVE_TYPE_ID, leaveApplication.get(0).getLeaveTypeId())) {
                    info.setLeaveName(optionalEntityPreference.get().getTimeOffAlias());
                    info.setOnLeave(true);
                    return info;
                }
                info.setLeaveName(optionalEntityPreference.get().getSickLeaveAlias());
                info.setOnLeave(true);
                return info;
            }
        }

        return info;
    }

    /**
     * Get the distance unit preference for an organization.
     * Returns KM as default if not set.
     *
     * @param orgId the organization ID
     * @return the DistanceUnitEnum (KM or MILES), defaults to KM
     */
    private DistanceUnitEnum getDistanceUnitForOrg(Long orgId) {
        Optional<EntityPreference> orgPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (orgPreference.isPresent() && orgPreference.get().getDistanceUnitId() != null) {
            return DistanceUnitEnum.fromIdOrDefault(orgPreference.get().getDistanceUnitId());
        }
        return DistanceUnitEnum.KM;
    }

    /**
     * Helper class to hold holiday information.
     */
    @Setter
    @Getter
    static class HolidayInfo {
        private boolean isWeekend = false;
        private boolean isPublicHoliday = false;
        private boolean isOnLeave = false;
        private String leaveName = null;

    }
}