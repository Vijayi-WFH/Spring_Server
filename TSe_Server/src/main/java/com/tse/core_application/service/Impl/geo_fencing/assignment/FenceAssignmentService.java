package com.tse.core_application.service.Impl.geo_fencing.assignment;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.OrgId;
import com.tse.core_application.dto.geo_fence.assignment.*;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.exception.geo_fencing.FenceNotFoundException;
import com.tse.core_application.exception.geo_fencing.ProblemException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.model.geo_fencing.assignment.FenceAssignment;
import com.tse.core_application.model.geo_fencing.fence.GeoFence;
import com.tse.core_application.repository.OrganizationRepository;
import com.tse.core_application.repository.ProjectRepository;
import com.tse.core_application.repository.TeamRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.repository.geo_fencing.assignment.FenceAssignmentRepository;
import com.tse.core_application.repository.geo_fencing.fence.GeoFenceRepository;
import com.tse.core_application.service.Impl.UserFeatureAccessService;
import com.tse.core_application.service.Impl.geo_fencing.dir.DirectoryProvider;
import com.tse.core_application.service.Impl.geo_fencing.policy.GeoFencingPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FenceAssignmentService {

    private static final String UNKNOWN_NAME = "NA";

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserFeatureAccessService userFeatureAccessService;

    @Autowired
    private GeoFencingPolicyService geoFencingPolicyService;
    private final FenceAssignmentRepository assignmentRepository;
    private final GeoFenceRepository fenceRepository;
    private final DirectoryProvider directoryProvider;

    public FenceAssignmentService(FenceAssignmentRepository assignmentRepository,
                                  GeoFenceRepository fenceRepository,
                                  DirectoryProvider directoryProvider) {
        this.assignmentRepository = assignmentRepository;
        this.fenceRepository = fenceRepository;
        this.directoryProvider = directoryProvider;
    }

    @Transactional
    public AssignFenceResult assignFenceToEntity(Long orgId, AssignFenceRequest request, String accountId) {
        geoFencingPolicyService.validateOrg (orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }

        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAdminPanel(accountIdLong,orgId);
        if (!geoFencingPolicyService.validateUserRoleAccess (Constants.EntityTypes.ORG, orgId, accountIdLong, List.of(RoleEnum.ORG_ADMIN.getRoleId())) && !checkHrAccess) {
            throw new ValidationFailedException("You do not have permission to assign fence to any entity");
        }
        request.setUpdatedBy(accountIdLong);
        // Validate fence exists and is active
        GeoFence fence = fenceRepository.findByIdAndOrgId(request.getFenceId(), orgId)
                .orElseThrow(() -> new FenceNotFoundException(request.getFenceId(), orgId));

        if (!fence.getOrgId().equals(orgId)) {
            throw new ProblemException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "CROSS_ORG_MISMATCH",
                    "Cross-org mismatch",
                    "Fence org_id does not match path org_id");
        }

        if (!Boolean.TRUE.equals(fence.getIsActive())) {
            throw new ValidationFailedException("Inactive fence can't be assign to any entity");
        }

        AssignFenceResult result = new AssignFenceResult();
        result.setFenceId(request.getFenceId());
        result.setUpdatedAt(LocalDateTime.now());
        result.setUpdatedBy(accountIdLong);

        AssignmentSummary summary = new AssignmentSummary();
        List<EntityResult> results = new ArrayList<>();

        // Process add items
        if (request.getAdd() != null) {
            for (EntityActionItem item : request.getAdd()) {
                EntityResult entityResult = processAdd(orgId, request.getFenceId(), item, accountIdLong, summary);
                results.add(entityResult);
            }
        }

        // Process remove items
        if (request.getRemove() != null) {
            for (EntityActionItem item : request.getRemove()) {
                EntityResult entityResult = processRemove(orgId, request.getFenceId(), item, summary);
                results.add(entityResult);
            }
        }

        result.setSummary(summary);
        result.setResults(results);

        return result;
    }

    private EntityResult processAdd(Long orgId, Long fenceId, EntityActionItem item, Long updatedBy, AssignmentSummary summary) {
        // Validate entity type
        if (!Constants.EntityTypes.isValid(item.getEntityTypeId())) {
            summary.incrementErrors();
            return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "ERROR", false,
                    Collections.emptyList(), "Invalid entity type: " + item.getEntityTypeId());
        }

        if (item.getEntityId() == null || item.getEntityId() <= 0) {
            summary.incrementErrors();
            return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "ERROR", false,
                    Collections.emptyList(), "Invalid entity ID");
        }

        if (!isEntityTypePartOfOg (orgId, item.getEntityTypeId(), item.getEntityId())) {
            summary.incrementErrors();
            return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "ERROR", false,
                    Collections.emptyList(), "Entity don't belong to org: " + item.getEntityTypeId());
        }

        // Check if assignment already exists
        Optional<FenceAssignment> existing = assignmentRepository.findByOrgIdAndFenceIdAndEntityTypeIdAndEntityId(
                orgId, fenceId, item.getEntityTypeId(), item.getEntityId());

        if (existing.isPresent()) {
            // Assignment already exists - handle makeDefault
            FenceAssignment assignment = existing.get();
            boolean wasUpdated = false;

            if (Boolean.TRUE.equals(item.getMakeDefault()) && !Boolean.TRUE.equals(assignment.getIsDefault())) {
                // Unset other defaults
                assignmentRepository.unsetDefaultForEntity(orgId, item.getEntityTypeId(), item.getEntityId());
                assignmentRepository.flush();

                // Set this as default
                assignment.setIsDefault(true);
                assignment.setUpdatedBy(updatedBy);
                assignmentRepository.save(assignment);

                summary.incrementUpdatedDefault();
                wasUpdated = true;
            }

            List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, item.getEntityTypeId(), item.getEntityId());

            if (wasUpdated) {
                return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "UPDATED_DEFAULT", true,
                        allFenceIds, "Set as default.");
            } else {
                summary.incrementNoops();
                return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "NOOP",
                        assignment.getIsDefault(), allFenceIds, "Already assigned.");
            }
        }

        // Create new assignment
        FenceAssignment newAssignment = new FenceAssignment();
        newAssignment.setOrgId(orgId);
        newAssignment.setFenceId(fenceId);
        newAssignment.setEntityTypeId(item.getEntityTypeId());
        newAssignment.setEntityId(item.getEntityId());
        newAssignment.setCreatedBy(updatedBy);
        newAssignment.setUpdatedBy(updatedBy);

        // Check if entity has no fences - auto-set as default
        List<FenceAssignment> entityAssignments = assignmentRepository.findByOrgIdAndEntityTypeIdAndEntityId(
                orgId, item.getEntityTypeId(), item.getEntityId());

        boolean makeDefault = Boolean.TRUE.equals(item.getMakeDefault()) || entityAssignments.isEmpty();

        if (makeDefault) {
            // Unset any existing default
            assignmentRepository.unsetDefaultForEntity(orgId, item.getEntityTypeId(), item.getEntityId());
            assignmentRepository.flush();
            newAssignment.setIsDefault(true);
            summary.incrementUpdatedDefault();
        } else {
            newAssignment.setIsDefault(false);
        }

        assignmentRepository.save(newAssignment);
        summary.incrementAdded();

        List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, item.getEntityTypeId(), item.getEntityId());

        String message = makeDefault ? "Assigned and set as default." : "Assigned.";
        return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "ADDED", makeDefault,
                allFenceIds, message);
    }

    private EntityResult processRemove(Long orgId, Long fenceId, EntityActionItem item, AssignmentSummary summary) {
        // Validate entity type
        if (!Constants.EntityTypes.isValid(item.getEntityTypeId())) {
            summary.incrementErrors();
            return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "ERROR", false,
                    Collections.emptyList(), "Invalid entity type: " + item.getEntityTypeId());
        }

        if (!isEntityTypePartOfOg (orgId, item.getEntityTypeId(), item.getEntityId())) {
            summary.incrementErrors();
            return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "ERROR", false,
                    Collections.emptyList(), "Entity don't belong to org: " + item.getEntityTypeId());
        }

        // Check if assignment exists
        Optional<FenceAssignment> existing = assignmentRepository.findByOrgIdAndFenceIdAndEntityTypeIdAndEntityId(
                orgId, fenceId, item.getEntityTypeId(), item.getEntityId());

        if (!existing.isPresent()) {
            summary.incrementNoops();
            List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, item.getEntityTypeId(), item.getEntityId());
            return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "NOOP", false,
                    allFenceIds, "Fence not assigned previously.");
        }

        FenceAssignment assignment = existing.get();
        boolean wasDefault = Boolean.TRUE.equals(assignment.getIsDefault());

        // Delete the assignment
        assignmentRepository.delete(assignment);
        assignmentRepository.flush();

        // If it was default, handle default reassignment
        if (wasDefault) {
            List<FenceAssignment> remainingAssignments = assignmentRepository.findByOrgIdAndEntityTypeIdAndEntityId(
                    orgId, item.getEntityTypeId(), item.getEntityId());

            if (remainingAssignments.size() == 1) {
                // Set the only remaining fence as default
                FenceAssignment remaining = remainingAssignments.get(0);
                remaining.setIsDefault(true);
                assignmentRepository.save(remaining);
            }
            // If more than one remains or none, leave no default
        }

        summary.incrementRemoved();

        List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, item.getEntityTypeId(), item.getEntityId());

        return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "REMOVED", false,
                allFenceIds, "Removed; assignment deleted.");
    }

    public Boolean isEntityTypePartOfOg (Long orgId, Integer entityTypeId, Long entityId) {
        if (Objects.equals(Constants.EntityTypes.ORG, entityTypeId)) {
            return Objects.equals(entityId, orgId);
        }
        else if (Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
            OrgId projectOrg = projectRepository.findOrgIdByProjectId(entityId);
            return projectOrg != null && Objects.equals(orgId, projectOrg.getOrgId());
        }
        else if (Objects.equals(Constants.EntityTypes.TEAM, entityTypeId)) {
            Long orgIdOfTeam = teamRepository.findFkOrgIdOrgIdByTeamId(entityId);
            return orgIdOfTeam != null && Objects.equals(orgId, orgIdOfTeam);
        }
        else if (Objects.equals(Constants.EntityTypes.USER, entityTypeId)) {
            return userAccountRepository.existsByAccountIdAndOrgIdAndIsActive(entityId, orgId, true);
        }
        return false;
    }

    @Transactional(readOnly = true)
    public AssignedEntitiesResponse getAssignedEntities(Long orgId, Long fenceId, String accountId) {
        geoFencingPolicyService.validateOrg (orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAdminPanel(accountIdLong,orgId);
        if (!geoFencingPolicyService.validateUserRoleAccess (Constants.EntityTypes.ORG, orgId, accountIdLong, List.of(RoleEnum.ORG_ADMIN.getRoleId())) && !checkHrAccess) {
            throw new ValidationFailedException("You do not have permission to assign fence to any entity");
        }
        // 1) Validate fence exists
        GeoFence fence = fenceRepository.findByIdAndOrgId(fenceId, orgId)
                .orElseThrow(() -> new FenceNotFoundException(fenceId, orgId));

        AssignedEntitiesResponse response = new AssignedEntitiesResponse();
        response.setFenceId(fenceId);

        // 2) Fetch all assignments for this fence
        List<FenceAssignment> assignments =
                Optional.ofNullable(assignmentRepository.findByOrgIdAndFenceId(orgId, fenceId))
                        .orElseGet(Collections::emptyList);

        // 3) Build assigned lists (already filters out "NA" using your buildAssignedLists)
        EntityLists assigned = buildAssignedLists(orgId, assignments);
        response.setAssigned(assigned);

        // 4) Prepare "universe" lists (directory/provider lookups or DB queries)
        List<Long> orgIdList            = Optional.ofNullable(List.of(orgId)).orElseGet(Collections::emptyList);
        List<Long> projectIdList        = Optional.ofNullable(projectRepository.findProjectIdsByOrgIds(List.of(orgId)))
                .orElseGet(Collections::emptyList);
        List<Long> teamIdList           = Optional.ofNullable(teamRepository.findTeamIdsByOrgId(orgId))
                .orElseGet(Collections::emptyList);
        List<Long> userAccountIdList    = Optional.ofNullable(userAccountRepository.findAllAccountIdByOrgIdAndIsActive(orgId, true))
                .orElseGet(Collections::emptyList);

        // 5) Build fast lookup sets of assigned IDs by type
        Set<Long> assignedUserIds    = assigned.getUsers().stream().map(AssignedEntity::getEntityId).collect(Collectors.toSet());
        Set<Long> assignedTeamIds    = assigned.getTeams().stream().map(AssignedEntity::getEntityId).collect(Collectors.toSet());
        Set<Long> assignedProjectIds = assigned.getProjects().stream().map(AssignedEntity::getEntityId).collect(Collectors.toSet());
        Set<Long> assignedOrgIds     = assigned.getOrgs().stream().map(AssignedEntity::getEntityId).collect(Collectors.toSet());

        // 6) Fill UNASSIGNED lists (skip entities whose resolved name is "NA")
        EntityLists unassigned = new EntityLists();

        // Orgs
        List<AssignedEntity> unassignedOrgs = orgIdList.stream()
                .filter(id -> !assignedOrgIds.contains(id))
                .map(id -> {
                    String name = getEntityName(Constants.EntityTypes.ORG, id);
                    if ("NA".equals(name)) return null;
                    List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, Constants.EntityTypes.ORG, id);
                    return new AssignedEntity(id, name, false, Optional.ofNullable(allFenceIds).orElseGet(Collections::emptyList));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        unassigned.setOrgs(unassignedOrgs);

        // Projects
        List<AssignedEntity> unassignedProjects = projectIdList.stream()
                .filter(id -> !assignedProjectIds.contains(id))
                .map(id -> {
                    String name = getEntityName(Constants.EntityTypes.PROJECT, id);
                    if ("NA".equals(name)) return null;
                    List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, Constants.EntityTypes.PROJECT, id);
                    return new AssignedEntity(id, name, false, Optional.ofNullable(allFenceIds).orElseGet(Collections::emptyList));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        unassigned.setProjects(unassignedProjects);

        // Teams
        List<AssignedEntity> unassignedTeams = teamIdList.stream()
                .filter(id -> !assignedTeamIds.contains(id))
                .map(id -> {
                    String name = getEntityName(Constants.EntityTypes.TEAM, id);
                    if ("NA".equals(name)) return null;
                    List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, Constants.EntityTypes.TEAM, id);
                    return new AssignedEntity(id, name, false, Optional.ofNullable(allFenceIds).orElseGet(Collections::emptyList));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        unassigned.setTeams(unassignedTeams);

        // Users
        List<AssignedEntity> unassignedUsers = userAccountIdList.stream()
                .filter(id -> !assignedUserIds.contains(id))
                .map(id -> {
                    String name = getEntityName(Constants.EntityTypes.USER, id);
                    if ("NA".equals(name)) return null;
                    List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, Constants.EntityTypes.USER, id);
                    return new AssignedEntity(id, name, false, Optional.ofNullable(allFenceIds).orElseGet(Collections::emptyList));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        unassigned.setUsers(unassignedUsers);

        response.setUnassigned(unassigned);

        // 7) Counts (assigned only, as per your structure)
        EntityCounts count = new EntityCounts(
                assigned.getUsers().size(),
                assigned.getTeams().size(),
                assigned.getProjects().size(),
                assigned.getOrgs().size()
        );
        response.setCount(count);

        return response;
    }


    private EntityLists buildAssignedLists(Long orgId, List<FenceAssignment> assignments) {
        EntityLists lists = new EntityLists();

        Map<Integer, List<FenceAssignment>> byType = assignments.stream()
                .collect(Collectors.groupingBy(FenceAssignment::getEntityTypeId));

        lists.setUsers(assignedEntitiesForType(orgId, byType.get(Constants.EntityTypes.USER)));
        lists.setTeams(assignedEntitiesForType(orgId, byType.get(Constants.EntityTypes.TEAM)));
        lists.setProjects(assignedEntitiesForType(orgId, byType.get(Constants.EntityTypes.PROJECT)));
        lists.setOrgs(assignedEntitiesForType(orgId, byType.get(Constants.EntityTypes.ORG)));

        return lists;
    }

    private List<AssignedEntity> assignedEntitiesForType(Long orgId, List<FenceAssignment> items) {
        if (items == null) return Collections.emptyList();

        return items.stream()
                // compute name once and drop if NA
                .map(a -> {
                    String name = getEntityName(a.getEntityTypeId(), a.getEntityId());
                    return UNKNOWN_NAME.equals(name) ? null : buildAssignedEntity(orgId, a, name);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // Build using pre-fetched name to avoid duplicate repository call
    private AssignedEntity buildAssignedEntity(Long orgId, FenceAssignment assignment, String name) {
        List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(
                orgId, assignment.getEntityTypeId(), assignment.getEntityId());

        return new AssignedEntity(
                assignment.getEntityId(),
                name,
                assignment.getIsDefault(),
                allFenceIds
        );
    }

    // If name is NA, caller will skip it
    private String getEntityName(Integer entityTypeId, Long entityId) {
        String entityName = null;

        if (Objects.equals(Constants.EntityTypes.ORG, entityTypeId)) {
            entityName = organizationRepository.findOrganizationNameByOrgId(entityId);
        } else if (Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
            entityName = projectRepository.findProjectNameByProjectId(entityId);
        } else if (Objects.equals(Constants.EntityTypes.TEAM, entityTypeId)) {
            entityName = teamRepository.findTeamNameByTeamId(entityId);
        } else if (Objects.equals(Constants.EntityTypes.USER, entityTypeId)) {
            UserAccount ua = userAccountRepository.findByAccountIdAndIsActive(entityId, true);
            if (ua != null && ua.getFkUserId() != null) {
                entityName = (ua.getFkUserId().getFirstName() + " " + ua.getFkUserId().getLastName()).trim();
            }
        }

        return (entityName == null || entityName.isBlank()) ? UNKNOWN_NAME : entityName;
    }

}
