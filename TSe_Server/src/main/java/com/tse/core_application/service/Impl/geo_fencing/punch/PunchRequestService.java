package com.tse.core_application.service.Impl.geo_fencing.punch;

import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.dto.geo_fence.fence.FenceResponse;
import com.tse.core_application.dto.geo_fence.fence.FenceUpdateRequest;
import com.tse.core_application.dto.geo_fence.punch.PunchRequestCreateDto;
import com.tse.core_application.dto.geo_fence.punch.PunchRequestViewDto;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.exception.geo_fencing.ProblemException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.geo_fencing.policy.AttendancePolicy;
import com.tse.core_application.model.geo_fencing.punch.PunchRequest;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.geo_fencing.policy.AttendancePolicyRepository;
import com.tse.core_application.repository.geo_fencing.punch.PunchRequestRepository;
import com.tse.core_application.service.Impl.NotificationService;
import com.tse.core_application.service.Impl.TaskServiceImpl;
import com.tse.core_application.service.Impl.UserFeatureAccessService;
import com.tse.core_application.service.Impl.geo_fencing.attendance.OfficePolicyProvider;
import com.tse.core_application.service.Impl.geo_fencing.membership.MembershipProvider;
import com.tse.core_application.service.Impl.geo_fencing.policy.GeoFencingPolicyService;
import com.tse.core_application.service.Impl.geo_fencing.policy.PolicyGate;
import com.tse.core_application.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Service for managing punch requests.
 */
@Service
public class PunchRequestService {

    private static final Logger logger = LoggerFactory.getLogger(PunchRequestService.class);

    @Autowired
    private GeoFencingPolicyService geoFencingPolicyService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private UserFeatureAccessService userFeatureAccessService;


    @Autowired
    private AccessDomainRepository accessDomainRepository;
    private final PunchRequestRepository punchRequestRepository;
    private final MembershipProvider membershipProvider;
    private final PolicyGate policyGate;
    private final OfficePolicyProvider officePolicyProvider;
    private final AttendancePolicyRepository attendancePolicyRepository;

    @Value("${attendance.punch.max-past-skew-minutes:5}")
    private int maxPastSkewMinutes;

    @Value("${attendance.punch.max-future-days:30}")
    private int maxFutureDays;

    public PunchRequestService(PunchRequestRepository punchRequestRepository,
                               MembershipProvider membershipProvider,
                                PolicyGate policyGate,
                               OfficePolicyProvider officePolicyProvider,
                               AttendancePolicyRepository attendancePolicyRepository) {
            this.punchRequestRepository = punchRequestRepository;
            this.membershipProvider = membershipProvider;
            this.policyGate = policyGate;
            this.officePolicyProvider = officePolicyProvider;
        this.attendancePolicyRepository = attendancePolicyRepository;
        }

    @Transactional
    public PunchRequestViewDto createPunchRequest(long orgId, PunchRequestCreateDto dto, String accountId, String timeZone) {

        geoFencingPolicyService.validateOrg (orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        //here need to add her role also
        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAdminPanel(accountIdLong,orgId);
        if (!geoFencingPolicyService.validateUserRoleAccess (Constants.EntityTypes.ORG, orgId, accountIdLong, List.of(RoleEnum.ORG_ADMIN.getRoleId())) && !checkHrAccess) {
            throw new ValidationFailedException("You do not have permission to create the punch request");
        }

        dto.setRequesterAccountId(accountIdLong);
        // Check policy active
        policyGate.assertPolicyActive(orgId);

        // Fetch attendance policy to get respondWithinMinutes constraints
        AttendancePolicy policy = attendancePolicyRepository.findByOrgId(orgId)
                .orElseThrow(() -> new ProblemException(
                        HttpStatus.NOT_FOUND,
                        "POLICY_NOT_FOUND",
                        "Attendance policy not found",
                        "No attendance policy found for organization " + orgId
                ));

        // Validate entity type
        if (!Constants.EntityTypes.isValid(dto.getEntityTypeId())) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid entity type",
                    "entityTypeId must be one of: 1 (USER), 2 (ORG), 4 (PROJECT), 5 (TEAM)"
            );
        }

        // Validate entity ID
        if (dto.getEntityId() == null || dto.getEntityId() <= 0) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid entity ID",
                    "entityId must be positive"
            );
        }

        // Validate respond within minutes
        if (dto.getRespondWithinMinutes() == null || dto.getRespondWithinMinutes() <= 0) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid respond time",
                    "Respond within minutes must be positive"
            );
        }

        Integer minMinutes = policy.getPunchRespondMinMinutes();
        Integer maxMinutes = policy.getPunchRespondMaxMinutes();

        if (dto.getRespondWithinMinutes() < minMinutes) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Respond time too short",
                    "Respond within minutes must be at least " + minMinutes + " minutes (policy minimum)"
            );
        }

        if (dto.getRespondWithinMinutes() > maxMinutes) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Respond time too long",
                    "Respond within minutes must be at most " + maxMinutes + " minutes (policy maximum)"
            );
        }

        // Convert requested datetime from user timezone to server timezone
        LocalDateTime requestedTimeUser = dto.getRequestedDateTime();
        LocalDateTime requestedTimeServer = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(requestedTimeUser, timeZone);

        // Validate requested datetime
        LocalDateTime now = LocalDateTime.now();

        // Check not too far in the past
        LocalDateTime minAllowedPast = now.minusMinutes(maxPastSkewMinutes);
        if (requestedTimeServer.isBefore(minAllowedPast)) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Requested time too far in past",
                    "Requested Date Time cannot be more than " + maxPastSkewMinutes + " minutes in the past"
            );
        }

        // Check not too far in the future
        // Check not beyond office end time
        // Get office hours from office policy provider
        LocalTime officeEndTime = officePolicyProvider.getOfficeEndTime(orgId);
        LocalDate requestedDate = requestedTimeUser.toLocalDate();
        LocalDateTime officeEndDateTime = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(requestedDate.atTime(officeEndTime), timeZone);

        if (requestedTimeServer.isAfter(officeEndDateTime)) {
            throw new ValidationFailedException("Punch-in requests are not allowed after office end time.");
        }

        // Create entity
        PunchRequest request = new PunchRequest();
        request.setOrgId(orgId);
        request.setEntityTypeId(dto.getEntityTypeId());
        request.setEntityId(dto.getEntityId());
        request.setRequesterAccountId(dto.getRequesterAccountId());
        request.setRequestedDatetime(requestedTimeServer);
        request.setRespondWithinMinutes(dto.getRespondWithinMinutes());
        request.setExpiresAt(requestedTimeServer.plusMinutes(dto.getRespondWithinMinutes()));
        request.setState(PunchRequest.State.PENDING);

        PunchRequest saved = punchRequestRepository.save(request);

        if (Objects.equals(Constants.EntityTypes.USER, dto.getEntityTypeId())) {
            String notificationMessage = "Please punch in for geo-fencing verification";
            try {
                List<HashMap<String, String>> payloads = notificationService.getFencingPunchRequestNotification(orgId, List.of(dto.getEntityId()), notificationMessage, saved.getId());
                taskServiceImpl.sendPushNotification(payloads);
            } catch (Exception e) {
//                throw new RuntimeException(e);
            }
        }

//        logger.info("Created punch request {} for org {} targeting {}/{} by requester {}",
//                saved.getId(), orgId, dto.getEntityTypeId(), dto.getEntityId(), dto.getRequesterAccountId());



        return toViewDto(saved, now, Collections.emptyList(), timeZone);
    }

    public List<PunchRequestViewDto> getPendingRequestsForAccounts(long orgId, List<Long> accountIds, String headerAccountId, String timeZone) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyList();
        }

        geoFencingPolicyService.validateOrg(orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(headerAccountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAdminPanel(accountIdLong,orgId);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, orgId, List.of(accountIdLong), List.of(RoleEnum.ORG_ADMIN.getRoleId()), true) && !checkHrAccess) {
            accountIds = new ArrayList<>();
            accountIds.add(accountIdLong);
        }

        LocalDateTime now = LocalDateTime.now();

        // Expand memberships for each account
        Map<Long, Set<EntityRef>> accountToEntities = new HashMap<>();
        for (Long accountId : accountIds) {
            accountToEntities.put(accountId, expandMembership(orgId, accountId));
        }

        // Collect all requests
        List<PunchRequest> allRequests = new ArrayList<>();

        // Group entities by type to minimize queries
        Map<Integer, Set<Long>> entitiesByType = new HashMap<>();
        for (Set<EntityRef> entities : accountToEntities.values()) {
            for (EntityRef ref : entities) {
                entitiesByType.computeIfAbsent(ref.entityTypeId, k -> new HashSet<>()).add(ref.entityId);
            }
        }

        // Query for each entity type
        for (Map.Entry<Integer, Set<Long>> entry : entitiesByType.entrySet()) {
            List<PunchRequest> requests = punchRequestRepository.findPendingForEntities(
                    orgId, now, entry.getKey(), entry.getValue()
            );
            allRequests.addAll(requests);
        }

        // De-duplicate and map to DTOs with account associations
        Map<Long, PunchRequestViewDto> requestMap = new HashMap<>();
        for (PunchRequest req : allRequests) {
            if (!requestMap.containsKey(req.getId())) {
                // Find which accounts this request applies to
                List<Long> appliesToAccounts = new ArrayList<>();
                for (Long accountId : accountIds) {
                    Set<EntityRef> entities = accountToEntities.get(accountId);
                    if (entities.stream().anyMatch(e -> e.matches(req.getEntityTypeId(), req.getEntityId()))) {
                        appliesToAccounts.add(accountId);
                    }
                }
                requestMap.put(req.getId(), toViewDto(req, now, appliesToAccounts, timeZone));
            }
        }

        return new ArrayList<>(requestMap.values());
    }

    public PunchRequestViewDto getPunchRequestById(long orgId, long requestId, String timeZone) {
        Optional<PunchRequest> requestOpt = punchRequestRepository.findByIdAndOrgId(requestId, orgId);
        if (!requestOpt.isPresent()) {
            throw new ProblemException(
                    HttpStatus.NOT_FOUND,
                    "REQUEST_NOT_FOUND",
                    "Punch request not found",
                    "Punch request " + requestId + " not found in org " + orgId
            );
        }

        PunchRequest request = requestOpt.get();

        // Verify no cross-org leakage
        if (!Objects.equals(request.getOrgId(), orgId)) {
            throw new ProblemException(
                    HttpStatus.CONFLICT,
                    "CROSS_ORG_MISMATCH",
                    "Cross-organization mismatch",
                    "Request belongs to different organization"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        return toViewDto(request, now, Collections.emptyList(), timeZone);
    }

    public List<PunchRequestViewDto> getPendingRequestHistory(long orgId, LocalDateTime from, LocalDateTime to, List<Long> accountIds, String headerAccountId, String timeZone) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyList();
        }

        geoFencingPolicyService.validateOrg(orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(headerAccountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }

        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAdminPanel(accountIdLong,orgId);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, orgId, List.of(accountIdLong), List.of(RoleEnum.ORG_ADMIN.getRoleId()), true) && !checkHrAccess) {
            accountIds = new ArrayList<>();
            accountIds.add(accountIdLong);
        }

        // Default to today if not specified
        if (from == null) {
            from = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (to == null) {
            to = from.plusDays(1);
        }

        // Convert from/to from user timezone to server timezone
        LocalDateTime fromServer = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(from, timeZone);
        LocalDateTime toServer = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(to, timeZone);

        // Expand memberships for each account
        Map<Long, Set<EntityRef>> accountToEntities = new HashMap<>();
        for (Long accountId : accountIds) {
            accountToEntities.put(accountId, expandMembership(orgId, accountId));
        }

        // Collect all requests
        List<PunchRequest> allRequests = new ArrayList<>();

        // Group entities by type
        Map<Integer, Set<Long>> entitiesByType = new HashMap<>();
        for (Set<EntityRef> entities : accountToEntities.values()) {
            for (EntityRef ref : entities) {
                entitiesByType.computeIfAbsent(ref.entityTypeId, k -> new HashSet<>()).add(ref.entityId);
            }
        }

        // Query for each entity type
        for (Map.Entry<Integer, Set<Long>> entry : entitiesByType.entrySet()) {
            List<PunchRequest> requests = punchRequestRepository.findHistoryForEntities(
                    orgId, fromServer, toServer, entry.getKey(), entry.getValue()
            );
            allRequests.addAll(requests);
        }

        // De-duplicate and map to DTOs
        LocalDateTime now = LocalDateTime.now();
        Map<Long, PunchRequestViewDto> requestMap = new HashMap<>();
        for (PunchRequest req : allRequests) {
            if (!requestMap.containsKey(req.getId())) {
                // Find which accounts this request applies to
                List<Long> appliesToAccounts = new ArrayList<>();
                for (Long accountId : accountIds) {
                    Set<EntityRef> entities = accountToEntities.get(accountId);
                    if (entities.stream().anyMatch(e -> e.matches(req.getEntityTypeId(), req.getEntityId()))) {
                        appliesToAccounts.add(accountId);
                    }
                }
                requestMap.put(req.getId(), toViewDto(req, now, appliesToAccounts, timeZone));
            }
        }

        return new ArrayList<>(requestMap.values());
    }

    private Set<EntityRef> expandMembership(long orgId, long accountId) {
        Set<EntityRef> entities = new HashSet<>();

        // USER
        entities.add(new EntityRef(Constants.EntityTypes.USER, accountId));

        // TEAM
        List<Long> teamIds = membershipProvider.listTeamsForUser(orgId, accountId);
        teamIds.forEach(teamId -> entities.add(new EntityRef(Constants.EntityTypes.TEAM, teamId)));

        // PROJECT
        List<Long> projectIds = membershipProvider.listProjectsForUser(orgId, accountId, teamIds);
        projectIds.forEach(projectId -> entities.add(new EntityRef(Constants.EntityTypes.PROJECT, projectId)));

        // ORG
        entities.add(new EntityRef(Constants.EntityTypes.ORG, orgId));

        return entities;
    }

    private PunchRequestViewDto toViewDto(PunchRequest request, LocalDateTime now, List<Long> appliesToAccountIds, String timeZone) {
        PunchRequestViewDto dto = new PunchRequestViewDto();
        dto.setId(request.getId());
        dto.setOrgId(request.getOrgId());
        dto.setEntityTypeId(request.getEntityTypeId());
        dto.setEntityId(request.getEntityId());
        dto.setRequesterAccountId(request.getRequesterAccountId());

        // Convert timestamps from server timezone to user timezone
        dto.setRequestedDateTime(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(request.getRequestedDatetime(), timeZone));
        dto.setRespondWithinMinutes(request.getRespondWithinMinutes());
        dto.setExpiresAt(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(request.getExpiresAt(), timeZone));
        dto.setState(request.getState().name());

        // Compute activeNow and secondsRemaining
        boolean activeNow = !now.isBefore(request.getRequestedDatetime()) && now.isBefore(request.getExpiresAt());
        dto.setActiveNow(activeNow);

        if (activeNow) {
            long secondsRemaining = Duration.between(now, request.getExpiresAt()).getSeconds();
            dto.setSecondsRemaining(Math.max(0, secondsRemaining));
        } else {
            dto.setSecondsRemaining(0L);
        }

        dto.setAppliesToAccountIds(appliesToAccountIds);

        return dto;
    }

    /**
     * Internal class for entity reference.
     */
    private static class EntityRef {
        final int entityTypeId;
        final long entityId;

        EntityRef(int entityTypeId, long entityId) {
            this.entityTypeId = entityTypeId;
            this.entityId = entityId;
        }

        boolean matches(int typeId, long entId) {
            return this.entityTypeId == typeId && this.entityId == entId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EntityRef)) return false;
            EntityRef entityRef = (EntityRef) o;
            return entityTypeId == entityRef.entityTypeId && entityId == entityRef.entityId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityTypeId, entityId);
        }
    }
}
