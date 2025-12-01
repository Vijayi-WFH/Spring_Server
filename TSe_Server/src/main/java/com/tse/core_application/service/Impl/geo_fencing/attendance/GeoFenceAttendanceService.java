package com.tse.core_application.service.Impl.geo_fencing.attendance;

import com.tse.core_application.constants.geo_fencing.EventAction;
import com.tse.core_application.constants.geo_fencing.EventKind;
import com.tse.core_application.constants.geo_fencing.EventSource;
import com.tse.core_application.constants.geo_fencing.IntegrityVerdict;
import com.tse.core_application.dto.geo_fence.attendance.*;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.exception.geo_fencing.ProblemException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.EntityPreference;
import com.tse.core_application.model.geo_fencing.assignment.FenceAssignment;
import com.tse.core_application.model.geo_fencing.attendance.AttendanceDay;
import com.tse.core_application.model.geo_fencing.attendance.AttendanceEvent;
import com.tse.core_application.model.geo_fencing.fence.GeoFence;
import com.tse.core_application.model.geo_fencing.policy.AttendancePolicy;
import com.tse.core_application.model.geo_fencing.punch.PunchRequest;
import com.tse.core_application.repository.EntityPreferenceRepository;
import com.tse.core_application.repository.geo_fencing.assignment.FenceAssignmentRepository;
import com.tse.core_application.repository.geo_fencing.attendance.AttendanceDayRepository;
import com.tse.core_application.repository.geo_fencing.attendance.AttendanceEventRepository;
import com.tse.core_application.repository.geo_fencing.fence.GeoFenceRepository;
import com.tse.core_application.repository.geo_fencing.policy.AttendancePolicyRepository;
import com.tse.core_application.repository.geo_fencing.punch.PunchRequestRepository;
import com.tse.core_application.service.Impl.UserFeatureAccessService;
import com.tse.core_application.service.Impl.geo_fencing.membership.MembershipProvider;
import com.tse.core_application.service.Impl.geo_fencing.policy.GeoFencingPolicyService;
import com.tse.core_application.service.Impl.geo_fencing.policy.PolicyGate;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.geo_fencing.GeoMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 6b: Orchestration service for attendance operations.
 */
@Service
public class GeoFenceAttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(GeoFenceAttendanceService.class);

    @Autowired
    private GeoFencingPolicyService geoFencingPolicyService;
    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Autowired
    private UserFeatureAccessService userFeatureAccessService;
    private final AttendanceEventRepository eventRepository;
    private final AttendanceDayRepository dayRepository;
    private final AttendancePolicyRepository policyRepository;
    private final FenceAssignmentRepository assignmentRepository;
    private final GeoFenceRepository fenceRepository;
    private final PunchRequestRepository punchRequestRepository;
    private final MembershipProvider membershipProvider;
    private final PolicyGate policyGate;
    private final AcceptanceRules acceptanceRules;
    private final DayRollupService dayRollupService;
    private final OfficePolicyProvider officePolicyProvider;
    private final GeoFenceAttendanceDataService geoFenceAttendanceDataService;

    public GeoFenceAttendanceService(
            AttendanceEventRepository eventRepository,
            AttendanceDayRepository dayRepository,
            AttendancePolicyRepository policyRepository,
            FenceAssignmentRepository assignmentRepository,
            GeoFenceRepository fenceRepository,
            PunchRequestRepository punchRequestRepository,
            MembershipProvider membershipProvider,
            PolicyGate policyGate,
            AcceptanceRules acceptanceRules,
            DayRollupService dayRollupService,
            OfficePolicyProvider officePolicyProvider,
            GeoFenceAttendanceDataService geoFenceAttendanceDataService) {
        this.eventRepository = eventRepository;
        this.dayRepository = dayRepository;
        this.policyRepository = policyRepository;
        this.assignmentRepository = assignmentRepository;
        this.fenceRepository = fenceRepository;
        this.punchRequestRepository = punchRequestRepository;
        this.membershipProvider = membershipProvider;
        this.policyGate = policyGate;
        this.acceptanceRules = acceptanceRules;
        this.dayRollupService = dayRollupService;
        this.officePolicyProvider = officePolicyProvider;
        this.geoFenceAttendanceDataService = geoFenceAttendanceDataService;
    }

    /**
     * Process a punch event (CHECK_IN or CHECK_OUT).
     */
    @Transactional
    public PunchResponse processPunch(long orgId, PunchCreateRequest request, String accountId, String timeZone) {

        geoFencingPolicyService.validateOrg(orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }

        request.setAccountId(accountIdLong);

        // 1. Validate policy is active
        policyGate.assertPolicyActive(orgId);

        // 2. Validate request
        validatePunchRequest(request);

        // 3. Get attendance policy
        AttendancePolicy policy = policyRepository.findByOrgId(orgId)
                .orElseThrow(() -> new ProblemException(
                        HttpStatus.NOT_FOUND,
                        "POLICY_NOT_FOUND",
                        "Attendance policy not found",
                        "No attendance policy found for org: " + orgId
                ));


        // 4. Parse event kind
        EventKind eventKind;
        try {
            eventKind = EventKind.valueOf(request.getEventKind());
        } catch (IllegalArgumentException e) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_EVENT_KIND",
                    "Invalid event kind",
                    "Event kind must be CHECK_IN or CHECK_OUT or BREAK_START or BREAK_END"
            );
        }

        // 5. Get nearest fence for user based on current location
        GeoFence fence = getNearestFenceForUser(orgId, request.getAccountId(), request.getLat(), request.getLon());

        if (fence == null) {
            throw new ValidationFailedException("There is no fence assigned to you");
        }

        // 6. Get today's events for validation
        LocalDate dateKey = dayRollupService.getDateKey(orgId, LocalDateTime.now(), timeZone);
        LocalDateTime dayStartInUserTimeZone = dateKey.atStartOfDay();
        LocalDateTime dayEndInUserTimeZone = dateKey.plusDays(1).atStartOfDay();

        LocalDateTime dayStartInServerTimeZone = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(dayStartInUserTimeZone, timeZone);
        LocalDateTime dayEndInServerTimeZone = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(dayEndInUserTimeZone, timeZone);

        List<AttendanceEvent> todayEvents = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                orgId, request.getAccountId(), dayStartInServerTimeZone, dayEndInServerTimeZone
        );

        // 7. Check idempotency
        if (request.getIdempotencyKey() != null) {
            Optional<AttendanceEvent> existing = todayEvents.stream()
                    .filter(e -> request.getIdempotencyKey().equals(e.getIdempotencyKey()))
                    .findFirst();
            if (existing.isPresent()) {
                return mapToResponse(existing.get(), timeZone);
            }
        }

        // 8. Validate using AcceptanceRules
        AcceptanceRules.ValidationResult validation = acceptanceRules.validate(
                orgId,
                request.getAccountId(),
                eventKind,
                request.getLat(),
                request.getLon(),
                request.getAccuracyM(),
                policy,
                fence,
                todayEvents,
                dateKey,
                timeZone
        );

        // 9. Create AttendanceEvent
        AttendanceEvent event = new AttendanceEvent();
        event.setOrgId(orgId);
        event.setAccountId(request.getAccountId());
        event.setEventKind(eventKind);
        event.setEventSource(EventSource.GEOFENCE);
        event.setEventAction(EventAction.MANUAL);
        event.setTsUtc(LocalDateTime.now());

        if (request.getClientLocalTs() != null) {
            // Convert clientLocalTs from user timezone to server timezone
            LocalDateTime clientLocalTsUser = LocalDateTime.parse(request.getClientLocalTs());
            LocalDateTime clientLocalTsServer = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(clientLocalTsUser, timeZone);
            event.setClientLocalTs(clientLocalTsServer);
        }
        else {
            event.setClientLocalTs(LocalDateTime.now());
        }
        event.setClientTz(timeZone);

        if (fence != null) {
            event.setFenceId(fence.getId());
        }
        event.setLat(request.getLat());
        event.setLon(request.getLon());
        event.setAccuracyM(request.getAccuracyM());

        // Determine under_range
        boolean underRange = false;
        if (fence != null && request.getLat() != null && request.getLon() != null) {
            double distance = GeoMath.distanceMeters(
                    request.getLat(), request.getLon(),
                    fence.getCenterLat(), fence.getCenterLng()
            );
            underRange = distance <= policy.getFenceRadiusM();
        }
        event.setUnderRange(underRange);

        event.setSuccess(validation.isSuccess());
        event.setVerdict(IntegrityVerdict.valueOf(validation.getVerdict()));
        event.setFailReason(validation.getFailReason());
        event.setFlags(validation.getFlags());
        event.setIdempotencyKey(request.getIdempotencyKey());

        // 10. Save event
        AttendanceEvent savedEvent = eventRepository.save(event);

        // 11. Update day rollup
        List<AttendanceEvent> updatedEvents = new ArrayList<>(todayEvents);
        updatedEvents.add(savedEvent);
        dayRollupService.updateDayRollup(orgId, request.getAccountId(), dateKey, updatedEvents);

        // 12. Return response
        return mapToResponse(savedEvent, timeZone);
    }

    /**
     * Process a PUNCHED event (supervisor/manager-triggered punch).
     */
    @Transactional
    public PunchResponse processPunchedEvent(long orgId, long accountId, long punchRequestId,Double lat, Double lon, Double accuracyM, String accountIds, String timeZone) {

        geoFencingPolicyService.validateOrg(orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountIds);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }

        if (!Objects.equals(accountId, accountIdLong)) {
            throw new ValidationFailedException("You are not authorised to punch for another user");
        }

        // 1. Validate policy is active
        policyGate.assertPolicyActive(orgId);

        // 2. Fetch the punch request
        PunchRequest punchRequest = punchRequestRepository.findById(punchRequestId)
                .orElseThrow(() -> new ProblemException(
                        HttpStatus.NOT_FOUND,
                        "PUNCH_REQUEST_NOT_FOUND",
                        "Punch request not found",
                        "No punch request found with id: " + punchRequestId
                ));

        // Validate it belongs to the correct org
        if (!punchRequest.getOrgId().equals(orgId)) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "ORG_MISMATCH",
                    "Organization mismatch",
                    "Punch request does not belong to org: " + orgId
            );
        }

        // 3. Get attendance policy
        AttendancePolicy policy = policyRepository.findByOrgId(orgId)
                .orElseThrow(() -> new ProblemException(
                        HttpStatus.NOT_FOUND,
                        "POLICY_NOT_FOUND",
                        "Attendance policy not found",
                        "No attendance policy found for org: " + orgId
                ));

        // 4. Get nearest fence for user based on location
        GeoFence fence = getNearestFenceForUser(orgId, accountId, lat, lon);

        // 5. Get today's events for validation
        LocalDate dateKey = dayRollupService.getDateKey(orgId, LocalDateTime.now(), timeZone);
        LocalDateTime dayStart = dateKey.atStartOfDay();
        LocalDateTime dayEnd = dateKey.plusDays(1).atStartOfDay();

        List<AttendanceEvent> todayEvents = eventRepository.findByOrgIdAndAccountIdAndTsUtcBetweenOrderByTsUtcAsc(
                orgId, accountId, dayStart, dayEnd
        );

        // 6. Validate using AcceptanceRules with GPS location
        AcceptanceRules.ValidationResult validation = acceptanceRules.validatePunched(
                punchRequest,
                lat,
                lon,
                accuracyM,
                policy,
                fence,
                todayEvents
        );

        // 7. Create AttendanceEvent for PUNCHED
        AttendanceEvent event = new AttendanceEvent();
        event.setOrgId(orgId);
        event.setAccountId(accountId);
        event.setEventKind(EventKind.PUNCHED);
        event.setEventSource(EventSource.SUPERVISOR);
        event.setEventAction(EventAction.MANUAL);
        event.setTsUtc(LocalDateTime.now());
        event.setPunchRequestId(punchRequestId);
        event.setRequesterAccountId(punchRequest.getRequesterAccountId());

        // Set GPS location data
        if (fence != null) {
            event.setFenceId(fence.getId());
        }
        event.setLat(lat);
        event.setLon(lon);
        event.setAccuracyM(accuracyM);

        // Determine under_range
        boolean underRange = false;
        if (fence != null && lat != null && lon != null) {
            double distance = GeoMath.distanceMeters(
                    lat, lon,
                    fence.getCenterLat(), fence.getCenterLng()
            );
            underRange = distance <= policy.getFenceRadiusM();
        }
        event.setUnderRange(underRange);

        event.setSuccess(validation.isSuccess());
        event.setVerdict(IntegrityVerdict.valueOf(validation.getVerdict()));
        event.setFailReason(validation.getFailReason());
        event.setFlags(validation.getFlags());

        // 8. If validation succeeded, mark punch request as FULFILLED
        if (validation.isSuccess()) {
            punchRequest.setState(PunchRequest.State.FULFILLED);
            punchRequestRepository.save(punchRequest);
        }

        // 9. Save event
        AttendanceEvent savedEvent = eventRepository.save(event);

        // 10. Update day rollup if successful
        if (validation.isSuccess()) {
            List<AttendanceEvent> updatedEvents = new ArrayList<>(todayEvents);
            updatedEvents.add(savedEvent);
            dayRollupService.updateDayRollup(orgId, accountId, dateKey, updatedEvents);
        }

        // 11. Return response
        return mapToResponse(savedEvent, timeZone);
    }

    /**
     * Get attendance summary for a specific user and date.
     * Reuses all logic from /data API for consistency.
     *
     * @param orgId Organization ID
     * @param request Request with accountId and date
     * @param timeZone User's timezone
     * @return Attendance summary with all events, missing events, and proper status
     */
    @Transactional(readOnly = true)
    public TodaySummaryResponse getTodaySummary(long orgId, TodayAttendanceRequest request, String accountIds, String timeZone) {
        geoFencingPolicyService.validateOrg(orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountIds);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAttendence(accountIdLong,orgId);
        if (!Objects.equals(request.getAccountId(), accountIdLong) && !checkHrAccess ) {
            throw new ValidationFailedException("You are not authorised to get event of another user");
        }

        Optional<EntityPreference> optionalEntityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        // Use AttendanceDataService to get single user data with all the /data API logic
        AttendanceDataResponse.UserAttendanceData userData =
                geoFenceAttendanceDataService.getSingleUserAttendanceData(orgId, request.getAccountId(), request.getDate(), timeZone, optionalEntityPreference);

        // Parse date to get holiday info
        LocalDate targetDate = LocalDate.parse(request.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate todayInUserTZ = LocalDate.now(ZoneId.of(timeZone));

        // Get holiday/leave information
        GeoFenceAttendanceDataService.HolidayInfo holidayInfo = geoFenceAttendanceDataService.getHolidayInfo(orgId, targetDate, request.getAccountId(), optionalEntityPreference);
        boolean isWeekend = holidayInfo.isWeekend();
        boolean isHoliday = holidayInfo.isPublicHoliday();
        String holidayName = holidayInfo.getLeaveName();
        boolean isOnLeave = holidayInfo.isOnLeave();
        String leaveName = holidayInfo.getLeaveName();

        // Build response

        TodaySummaryResponse response = new TodaySummaryResponse();
        response.setAccountId(request.getAccountId());
        response.setDisplayName(userData.getDisplayName());
        response.setEmail(userData.getEmail());
        response.setDate(request.getDate());
        response.setIsWeekend(isWeekend);
        response.setIsHoliday(isHoliday);
        response.setIsOnLeave(isOnLeave);
        response.setLeaveName(leaveName);
        response.setStatus(userData.getStatus());
        response.setCheckInTime(userData.getCheckInTime());
        response.setCheckOutTime(userData.getCheckOutTime());
        response.setTotalHoursMinutes(userData.getTotalHoursMinutes());
        response.setTotalEffortMinutes(userData.getTotalEffortMinutes());
        response.setTotalBreakMinutes(userData.getTotalBreakMinutes());
        response.setPrimaryFenceName(userData.getPrimaryFenceName());
        response.setFlags(userData.getFlags());

        // Convert breaks
        List<TodaySummaryResponse.BreakInterval> breaks = userData.getBreaks() == null ? null :
                userData.getBreaks().stream()
                        .map(b -> new TodaySummaryResponse.BreakInterval(
                                b.getStartTime(),
                                b.getEndTime(),
                                b.getDurationMinutes()))
                        .collect(Collectors.toList());
        response.setBreaks(breaks);

        // Convert timeline
        List<TodaySummaryResponse.PunchEvent> timeline = userData.getTimeline() == null ? null :
                userData.getTimeline().stream()
                        .map(e -> {
                            TodaySummaryResponse.PunchEvent punchEvent = new TodaySummaryResponse.PunchEvent();
                            punchEvent.setEventId(e.getEventId());
                            punchEvent.setType(e.getType());
                            punchEvent.setDateTime(e.getDateTime());
                            punchEvent.setAttemptStatus(e.getAttemptStatus());
                            punchEvent.setLocationLabel(e.getLocationLabel());
                            punchEvent.setLat(e.getLat());
                            punchEvent.setLon(e.getLon());
                            punchEvent.setWithinFence(e.getWithinFence());
                            punchEvent.setIntegrityVerdict(e.getIntegrityVerdict());
                            punchEvent.setFailReason(e.getFailReason());
                            punchEvent.setRequestorAccountInfo(e.getRequestorAccountInfo());
                            return punchEvent;
                        })
                        .collect(Collectors.toList());
        response.setTimeline(timeline);

        return response;
    }

    private void validatePunchRequest(PunchCreateRequest request) {
        if (request.getAccountId() == null || request.getAccountId() <= 0) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid accountId",
                    "accountId is required and must be positive"
            );
        }

        if (request.getEventKind() == null || request.getEventKind().isEmpty()) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid eventKind",
                    "eventKind is required"
            );
        }

        if (request.getLat() == null || request.getLon() == null) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid location",
                    "lat and lon are required"
            );
        }
    }

    /**
     * Get the nearest fence for a user based on their current location.
     * Finds all fences assigned to user's entities and returns the closest one.
     *
     * @param orgId Organization ID
     * @param accountId User account ID
     * @param userLat User's current latitude
     * @param userLon User's current longitude
     * @return The nearest GeoFence, or null if no fences are assigned
     */
    private GeoFence getNearestFenceForUser(long orgId, long accountId, double userLat, double userLon) {
        // Expand memberships
        Set<EntityRef> entities = expandMemberships(orgId, accountId);

        // Get assignments for all entities
        List<Integer> entityTypeIds = entities.stream()
                .map(e -> e.entityTypeId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> entityIds = entities.stream()
                .map(e -> e.entityId)
                .distinct()
                .collect(Collectors.toList());

        List<FenceAssignment> assignments = assignmentRepository
                .findByOrgIdAndEntityTypeIdInAndEntityIdIn(orgId, entityTypeIds, entityIds);

        if (assignments.isEmpty()) {
            return null;
        }

        // Get all unique fence IDs from assignments
        List<Long> fenceIds = assignments.stream()
                .map(FenceAssignment::getFenceId)
                .distinct()
                .collect(Collectors.toList());

        // Fetch all fences
        List<GeoFence> fences = fenceRepository.findAllById(fenceIds);

        if (fences.isEmpty()) {
            return null;
        }

        // Find the nearest fence based on distance from user's current location
        GeoFence nearestFence = fences.stream()
                .min(Comparator.comparingDouble(fence ->
                        GeoMath.distanceMeters(
                                userLat, userLon,
                                fence.getCenterLat(), fence.getCenterLng()
                        )
                ))
                .orElse(null);

        return nearestFence;
    }

    /**
     * Get default fence for user (deprecated - kept for backward compatibility).
     * Use getNearestFenceForUser instead.
     */
    @Deprecated
    private GeoFence getDefaultFenceForUser(long orgId, long accountId) {
        // Expand memberships
        Set<EntityRef> entities = expandMemberships(orgId, accountId);

        // Get assignments for all entities
        List<Integer> entityTypeIds = entities.stream()
                .map(e -> e.entityTypeId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> entityIds = entities.stream()
                .map(e -> e.entityId)
                .distinct()
                .collect(Collectors.toList());

        List<FenceAssignment> assignments = assignmentRepository
                .findByOrgIdAndEntityTypeIdInAndEntityIdIn(orgId, entityTypeIds, entityIds);

        if (assignments.isEmpty()) {
            return null;
        }

        // Compute default fence with precedence: USER > TEAM > PROJECT > ORG
        FenceAssignment defaultAssignment = assignments.stream()
                .min(Comparator
                        .comparingInt((FenceAssignment a) -> precedence(a.getEntityTypeId()))
                        .thenComparing(FenceAssignment::getCreatedDatetime))
                .orElse(null);

        if (defaultAssignment == null) {
            return null;
        }

        return fenceRepository.findById(defaultAssignment.getFenceId()).orElse(null);
    }

    private Set<EntityRef> expandMemberships(long orgId, long accountId) {
        Set<EntityRef> entities = new HashSet<>();

        // Add USER entity
        entities.add(new EntityRef(Constants.EntityTypes.USER, accountId));

        // Add TEAMs
        List<Long> teamIds = membershipProvider.listTeamsForUser(orgId, accountId);
        for (Long teamId : teamIds) {
            entities.add(new EntityRef(Constants.EntityTypes.TEAM, teamId));
        }

        // Add PROJECTs
        List<Long> projectIds = membershipProvider.listProjectsForUser(orgId, accountId, teamIds);
        for (Long projectId : projectIds) {
            entities.add(new EntityRef(Constants.EntityTypes.PROJECT, projectId));
        }

        // Add ORG entity
        entities.add(new EntityRef(Constants.EntityTypes.ORG, orgId));

        return entities;
    }

    private int precedence(int entityTypeId) {
        if (entityTypeId == Constants.EntityTypes.USER) return 1;
        if (entityTypeId == Constants.EntityTypes.TEAM) return 2;
        if (entityTypeId == Constants.EntityTypes.PROJECT) return 3;
        if (entityTypeId == Constants.EntityTypes.ORG) return 4;
        return 99;
    }

    private PunchResponse mapToResponse(AttendanceEvent eventDb, String timeZone) {

        AttendanceEvent event = new AttendanceEvent();
        BeanUtils.copyProperties(eventDb, event);
        PunchResponse response = new PunchResponse();
        response.setEventId(event.getId());
        response.setAccountId(event.getAccountId());
        response.setEventKind(event.getEventKind().name());
        response.setEventSource(event.getEventSource().name());
        // Convert timestamp from server timezone to user timezone
        response.setTsUtc(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(event.getTsUtc(), timeZone).toString());
        response.setFenceId(event.getFenceId());
        response.setUnderRange(event.getUnderRange());
        response.setSuccess(event.getSuccess());
        response.setVerdict(event.getVerdict().name());
        response.setFailReason(event.getFailReason());
        response.setFlags(event.getFlags());
        return response;
    }

    private static class EntityRef {
        final int entityTypeId;
        final long entityId;

        EntityRef(int entityTypeId, long entityId) {
            this.entityTypeId = entityTypeId;
            this.entityId = entityId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntityRef entityRef = (EntityRef) o;
            return entityTypeId == entityRef.entityTypeId && entityId == entityRef.entityId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityTypeId, entityId);
        }
    }
}
