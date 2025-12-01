package com.tse.core_application.service.Impl.geo_fencing.userfence;

import com.tse.core_application.dto.geo_fence.userfence.Counts;
import com.tse.core_application.dto.geo_fence.userfence.EffectiveFenceDto;
import com.tse.core_application.dto.geo_fence.userfence.SourceRef;
import com.tse.core_application.dto.geo_fence.userfence.UserFencesResponse;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.exception.geo_fencing.ProblemException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.geo_fencing.assignment.FenceAssignment;
import com.tse.core_application.model.geo_fencing.fence.GeoFence;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.repository.geo_fencing.assignment.FenceAssignmentRepository;
import com.tse.core_application.repository.geo_fencing.fence.GeoFenceRepository;
import com.tse.core_application.service.Impl.geo_fencing.membership.MembershipProvider;
import com.tse.core_application.service.Impl.geo_fencing.policy.GeoFencingPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for computing effective fences available to a user.
 */
@Service
public class UserFenceService {

    @Autowired
    private GeoFencingPolicyService geoFencingPolicyService;
    private static final Logger logger = LoggerFactory.getLogger(UserFenceService.class);

    @Autowired
    private UserAccountRepository userAccountRepository;

    private final FenceAssignmentRepository assignmentRepository;
    private final GeoFenceRepository fenceRepository;
    private final MembershipProvider membershipProvider;

    @Value("${attendance.policy.skip-org-validation:true}")
    private boolean skipOrgValidation;

    public UserFenceService(FenceAssignmentRepository assignmentRepository,
                            GeoFenceRepository fenceRepository,
                            MembershipProvider membershipProvider) {
        this.assignmentRepository = assignmentRepository;
        this.fenceRepository = fenceRepository;
        this.membershipProvider = membershipProvider;
    }

    public UserFencesResponse getUserFences(long orgId, long accountId, boolean includeInactive) {

        geoFencingPolicyService.validateOrg (orgId);

        if (!userAccountRepository.existsByAccountIdAndOrgIdAndIsActive(accountId, orgId, true)) {
            throw new ValidationFailedException("This account is either in-active or not part of org");
        }
        // Validation
        if (accountId <= 0) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid accountId",
                    "accountId must be positive"
            );
        }

        // 1. Expand memberships
        List<EntityRef> entities = expandMemberships(orgId, accountId);

        // 2. Collect all assignments
        List<FenceAssignment> allAssignments = collectAssignments(orgId, entities);

        // 3. Validate cross-org consistency
//        validateNoOrgMismatch(orgId, allAssignments);

        // 4. Group assignments by fenceId
        Map<Long, List<FenceAssignment>> assignmentsByFence = allAssignments.stream()
                .collect(Collectors.groupingBy(FenceAssignment::getFenceId));

        // 5. Fetch fences
        Set<Long> fenceIds = assignmentsByFence.keySet();
        List<GeoFence> fences = fetchFences(orgId, fenceIds, includeInactive);

        // 6. Build response
        return buildResponse(orgId, accountId, fences, assignmentsByFence, allAssignments);
    }

    private List<EntityRef> expandMemberships(long orgId, long accountId) {
        List<EntityRef> entities = new ArrayList<>();

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

//        logger.debug("Expanded memberships for user {} in org {}: {} entities",
//                accountId, orgId, entities.size());

        return entities;
    }

    private List<FenceAssignment> collectAssignments(long orgId, List<EntityRef> entities) {
        List<FenceAssignment> allAssignments = new ArrayList<>();

        for (EntityRef entity : entities) {
            List<FenceAssignment> assignments = assignmentRepository
                    .findByOrgIdAndEntityTypeIdAndEntityId(orgId, entity.getEntityTypeId(), entity.getEntityId());
            allAssignments.addAll(assignments);
        }

//        logger.debug("Collected {} total assignments for org {}", allAssignments.size(), orgId);

        return allAssignments;
    }

    private void validateNoOrgMismatch(long orgId, List<FenceAssignment> assignments) {
        for (FenceAssignment assignment : assignments) {
            if (!Objects.equals(assignment.getOrgId(), orgId)) {
                throw new ProblemException(
                        HttpStatus.CONFLICT,
                        "CROSS_ORG_MISMATCH",
                        "Cross-organization mismatch detected",
                        "Assignment " + assignment.getId() + " belongs to different org"
                );
            }
        }
    }

    private List<GeoFence> fetchFences(long orgId, Set<Long> fenceIds, boolean includeInactive) {
        if (fenceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<GeoFence> fences;
        if (includeInactive) {
            fences = fenceRepository.findByOrgIdAndIdIn(orgId, fenceIds);
        } else {
            fences = fenceRepository.findByOrgIdAndIdInAndIsActiveTrue(orgId, fenceIds);
        }

//        logger.debug("Fetched {} fences for org {} (includeInactive={})",
//                fences.size(), orgId, includeInactive);

        return fences;
    }

    private UserFencesResponse buildResponse(long orgId,
                                             long accountId,
                                             List<GeoFence> fences,
                                             Map<Long, List<FenceAssignment>> assignmentsByFence,
                                             List<FenceAssignment> allAssignments) {
        UserFencesResponse response = new UserFencesResponse();
        response.setOrgId(orgId);
        response.setAccountId(accountId);

        List<EffectiveFenceDto> effectiveFences = new ArrayList<>();

        for (GeoFence fence : fences) {
            List<FenceAssignment> fenceAssignments = assignmentsByFence.get(fence.getId());
            if (fenceAssignments == null) {
                continue;
            }

            EffectiveFenceDto dto = new EffectiveFenceDto();
            dto.setId(fence.getId());
            dto.setName(fence.getName());
            dto.setLocationKind(fence.getLocationKind() != null ? fence.getLocationKind().name() : null);
            dto.setSiteCode(fence.getSiteCode());
            dto.setTz(fence.getTz());
            dto.setCenterLat(fence.getCenterLat());
            dto.setCenterLng(fence.getCenterLng());
            dto.setRadiusM(fence.getRadiusM());
            dto.setIsActive(fence.getIsActive());

            // Build sources
            List<SourceRef> sources = fenceAssignments.stream()
                    .map(a -> new SourceRef(a.getEntityTypeId(), a.getEntityId(), a.getIsDefault()))
                    .collect(Collectors.toList());
            dto.setSources(sources);

            effectiveFences.add(dto);
        }

        response.setFences(effectiveFences);

        // Compute default fence
        Long defaultFenceId = computeDefaultFenceId(accountId, allAssignments, assignmentsByFence.keySet());
        response.setDefaultFenceIdForUser(defaultFenceId);

        // Compute counts
        Counts counts = computeCounts(effectiveFences, allAssignments);
        response.setCounts(counts);

        return response;
    }

    private Long computeDefaultFenceId(long accountId,
                                       List<FenceAssignment> allAssignments,
                                       Set<Long> validFenceIds) {
        // Priority 1: USER-level default
        Optional<FenceAssignment> userDefault = allAssignments.stream()
                .filter(a -> a.getEntityTypeId() == Constants.EntityTypes.USER)
                .filter(a -> a.getEntityId().equals(accountId))
                .filter(FenceAssignment::getIsDefault)
                .filter(a -> validFenceIds.contains(a.getFenceId()))
                .findFirst();

        if (userDefault.isPresent()) {
            return userDefault.get().getFenceId();
        }

        // Priority 2: TEAM default (earliest by created_datetime)
        Optional<FenceAssignment> teamDefault = allAssignments.stream()
                .filter(a -> a.getEntityTypeId() == Constants.EntityTypes.TEAM)
                .filter(FenceAssignment::getIsDefault)
                .filter(a -> validFenceIds.contains(a.getFenceId()))
                .min(Comparator.comparing(FenceAssignment::getCreatedDatetime));

        if (teamDefault.isPresent()) {
            return teamDefault.get().getFenceId();
        }

        // Priority 3: PROJECT default (earliest by created_datetime)
        Optional<FenceAssignment> projectDefault = allAssignments.stream()
                .filter(a -> a.getEntityTypeId() == Constants.EntityTypes.PROJECT)
                .filter(FenceAssignment::getIsDefault)
                .filter(a -> validFenceIds.contains(a.getFenceId()))
                .min(Comparator.comparing(FenceAssignment::getCreatedDatetime));

        if (projectDefault.isPresent()) {
            return projectDefault.get().getFenceId();
        }

        // Priority 4: ORG default
        Optional<FenceAssignment> orgDefault = allAssignments.stream()
                .filter(a -> a.getEntityTypeId() == Constants.EntityTypes.ORG)
                .filter(FenceAssignment::getIsDefault)
                .filter(a -> validFenceIds.contains(a.getFenceId()))
                .findFirst();

        return orgDefault.map(FenceAssignment::getFenceId).orElse(null);
    }

    private Counts computeCounts(List<EffectiveFenceDto> fences, List<FenceAssignment> allAssignments) {
        // Count unique fences per entity type
        Set<Long> userFences = new HashSet<>();
        Set<Long> teamFences = new HashSet<>();
        Set<Long> projectFences = new HashSet<>();
        Set<Long> orgFences = new HashSet<>();

        for (FenceAssignment assignment : allAssignments) {
            Long fenceId = assignment.getFenceId();
            int entityTypeId = assignment.getEntityTypeId();

            if (entityTypeId == Constants.EntityTypes.USER) {
                userFences.add(fenceId);
            } else if (entityTypeId == Constants.EntityTypes.TEAM) {
                teamFences.add(fenceId);
            } else if (entityTypeId == Constants.EntityTypes.PROJECT) {
                projectFences.add(fenceId);
            } else if (entityTypeId == Constants.EntityTypes.ORG) {
                orgFences.add(fenceId);
            }
        }

        return new Counts(
                fences.size(),
                userFences.size(),
                teamFences.size(),
                projectFences.size(),
                orgFences.size()
        );
    }

    /**
     * Internal class for representing entity references.
     */
    private static class EntityRef {
        private final int entityTypeId;
        private final long entityId;

        EntityRef(int entityTypeId, long entityId) {
            this.entityTypeId = entityTypeId;
            this.entityId = entityId;
        }

        int getEntityTypeId() {
            return entityTypeId;
        }

        long getEntityId() {
            return entityId;
        }
    }
}
