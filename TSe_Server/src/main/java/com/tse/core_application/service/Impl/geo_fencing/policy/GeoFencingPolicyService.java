package com.tse.core_application.service.Impl.geo_fencing.policy;

import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.dto.GeoFenceOrgIdOrgName;
import com.tse.core_application.dto.geo_fence.policy.PolicyCreateRequest;
import com.tse.core_application.dto.geo_fence.policy.PolicyResponse;
import com.tse.core_application.dto.geo_fence.policy.PolicyUpdateRequest;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.exception.geo_fencing.PolicyNotFoundException;
import com.tse.core_application.exception.geo_fencing.ProblemException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.EntityPreference;
import com.tse.core_application.model.Organization;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.model.geo_fencing.policy.AttendancePolicy;
import com.tse.core_application.repository.*;
import com.tse.core_application.repository.geo_fencing.policy.AttendancePolicyRepository;
import com.tse.core_application.service.Impl.UserFeatureAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeoFencingPolicyService {

    private final AttendancePolicyRepository policyRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private UserAccountRepository userAccountRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserFeatureAccessService userFeatureAccessService;

    @Autowired
    private UserFeatureAccessRepository userFeatureAccessRepository;

    public GeoFencingPolicyService(AttendancePolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional
    public PolicyResponse createPolicy(Long orgId, PolicyCreateRequest request, String accountId, String timeZone) {

        validateOrg (orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        if (!accessDomainRepository.existsByAccountIdInAndRoleIdInAndIsActive(List.of(accountIdLong), List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new ValidationFailedException("You do not have permission to assign geo-fencing to any org");
        }
        // Check if policy already exists (idempotent)
        Optional<AttendancePolicy> existing = policyRepository.findByOrgId(orgId);
        EntityPreference entityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId).orElse(null);
        if (existing.isPresent()) {
            PolicyResponse response = new PolicyResponse();
            if (entityPreference != null) {
                entityPreference.setIsGeoFencingAllowed(true);
                entityPreferenceRepository.save(entityPreference);
                response.setIsGeoFencingAllowed(entityPreference.getIsGeoFencingAllowed());
            }
            response.setStatus("NOOP");
            response.setOrgId(orgId);
            response.setPolicyId(existing.get().getId());
            return response;
        }

        try {
            // Create new policy with defaults
            AttendancePolicy policy = new AttendancePolicy();
            policy.setOrgId(orgId);
            if (request != null && request.getCreatedBy() != null) {
                policy.setCreatedBy(request.getCreatedBy());
            }
            else {
                policy.setCreatedBy(accountIdLong);
            }

            policy = policyRepository.save(policy);

            PolicyResponse response = new PolicyResponse();
            if (entityPreference != null) {
                entityPreference.setIsGeoFencingAllowed(true);
                entityPreferenceRepository.save(entityPreference);
                response.setIsGeoFencingAllowed(entityPreference.getIsGeoFencingAllowed());
            }
            response.setStatus("CREATED");
            response.setOrgId(orgId);
            response.setPolicyId(policy.getId());
            response.setDefaultsApplied(true);
            return response;

        } catch (DataIntegrityViolationException ex) {
            // Race condition: another thread created the policy
            Optional<AttendancePolicy> raceCheck = policyRepository.findByOrgId(orgId);
            if (raceCheck.isPresent()) {
                PolicyResponse response = new PolicyResponse();
                response.setStatus("NOOP");
                response.setOrgId(orgId);
                response.setPolicyId(raceCheck.get().getId());
                return response;
            }
            throw ex;
        }
    }

    public Boolean validateUserRoleAccess(Integer entityTypeId, Long entityId, Long accountIdLong, List<Integer> roleIdList) {
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(entityTypeId, entityId, List.of(accountIdLong), roleIdList, true)) {
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicy(Long orgId, String accountId, String timeZone) {
        validateOrg (orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Optional<EntityPreference> entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAdminPanel(accountIdLong,orgId);
        if (!validateUserRoleAccess (Constants.EntityTypes.ORG, orgId, accountIdLong, List.of(RoleEnum.ORG_ADMIN.getRoleId())) &&
                !accessDomainRepository.existsByAccountIdInAndRoleIdInAndIsActive(List.of(accountIdLong), List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true) && !checkHrAccess) {
            throw new ValidationFailedException("You do not have permission to get geo-fence policy of selected org");
        }
        if (entityPreferenceOptional.isPresent() && Boolean.FALSE.equals(entityPreferenceOptional.get().getIsGeoFencingAllowed())) {
            throw new ValidationFailedException("Geo fencing is not assigned to selected org");
        }
        AttendancePolicy policy = policyRepository.findByOrgId(orgId)
            .orElseThrow(() -> new PolicyNotFoundException(orgId));

        return PolicyResponse.fromEntity(policy, entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId).orElse(null), timeZone);
    }

    @Transactional
    public PolicyResponse updatePolicy(Long orgId, PolicyUpdateRequest request, String accountId, String timeZone) {
        validateOrg (orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        EntityPreference entityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId).orElse(null);
        if (entityPreference != null && entityPreference.getIsGeoFencingAllowed() == false) {
            throw new ValidationFailedException("Geo-fence policy is not assigned to this org");
        }
        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAdminPanel(accountIdLong,orgId);
        if (!validateUserRoleAccess (Constants.EntityTypes.ORG, orgId, accountIdLong, List.of(RoleEnum.ORG_ADMIN.getRoleId())) && !checkHrAccess) {
            throw new ValidationFailedException("You do not have permission to update geo-fence policy of selected org");
        }

        AttendancePolicy policy = policyRepository.findByOrgId(orgId)
            .orElseThrow(() -> new PolicyNotFoundException(orgId));

        // Additional business validation
        if (request.getFenceRadiusM() < 30) {
            throw new ProblemException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Validation failed",
                "fenceRadiusM must be >= 30"
            );
        }

        if (request.getAccuracyGateM() < 10) {
            throw new ProblemException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Validation failed",
                "accuracyGateM must be >= 10"
            );
        }

        // Validate punch respond minutes ordering: min <= default <= max
        if (request.getPunchRespondMinMinutes() > request.getPunchRespondDefaultMinutes()) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Validation failed",
                    "punchRespondMinMinutes must be <= punchRespondDefaultMinutes"
            );
        }

        if (request.getPunchRespondDefaultMinutes() > request.getPunchRespondMaxMinutes()) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Validation failed",
                    "punchRespondDefaultMinutes must be <= punchRespondMaxMinutes"
            );
        }

        // Update fields
        if (request.getIsGeoFencingActive() != null) {
            policy.setIsActive(request.getIsGeoFencingActive());
        }
        policy.setOutsideFencePolicy(request.getOutsideFencePolicy());
        policy.setIntegrityPosture(request.getIntegrityPosture());
        policy.setAllowCheckinBeforeStartMin(request.getAllowCheckinBeforeStartMin());
        policy.setLateCheckinAfterStartMin(request.getLateCheckinAfterStartMin());
        policy.setAllowCheckoutBeforeEndMin(request.getAllowCheckoutBeforeEndMin());
        policy.setMaxCheckoutAfterEndMin(request.getMaxCheckoutAfterEndMin());
        policy.setNotifyBeforeShiftStartMin(request.getNotifyBeforeShiftStartMin());
        policy.setFenceRadiusM(request.getFenceRadiusM());
        policy.setAccuracyGateM(request.getAccuracyGateM());
        policy.setCooldownSeconds(request.getCooldownSeconds());
        policy.setMaxSuccessfulPunchesPerDay(request.getMaxSuccessfulPunchesPerDay());
        policy.setMaxFailedPunchesPerDay(request.getMaxFailedPunchesPerDay());
        policy.setMaxWorkingHoursPerDay(request.getMaxWorkingHoursPerDay());
        policy.setPunchRespondMinMinutes(request.getPunchRespondMinMinutes());
        policy.setPunchRespondMaxMinutes(request.getPunchRespondMaxMinutes());
        policy.setPunchRespondDefaultMinutes(request.getPunchRespondDefaultMinutes());
        policy.setDwellInMin(request.getDwellInMin());
        policy.setDwellOutMin(request.getDwellOutMin());
        policy.setAutoOutEnabled(request.getAutoOutEnabled());
        policy.setAutoOutDelayMin(request.getAutoOutDelayMin());
        policy.setUndoWindowMin(request.getUndoWindowMin());

        if (request.getUpdatedBy() != null) {
            policy.setUpdatedBy(request.getUpdatedBy());
        }
        else {
            policy.setUpdatedBy(accountIdLong);
        }

        policy = policyRepository.save(policy);

        PolicyResponse response = new PolicyResponse();
        if (entityPreference != null && request.getIsGeoFencingActive() != null && !Objects.equals(request.getIsGeoFencingActive(), entityPreference.getIsGeoFencingActive())) {
            entityPreference.setIsGeoFencingActive(request.getIsGeoFencingActive());
            entityPreferenceRepository.save(entityPreference);
        }
        if (entityPreference != null) {
            response.setIsGeoFencingAllowed(entityPreference.getIsGeoFencingAllowed());
            response.setIsGeoFencingActive(entityPreference.getIsGeoFencingActive());
        }
        response.setStatus("UPDATED");
        response.setPolicyId(policy.getId());
        response.setUpdatedAt(policy.getUpdatedDatetime());
        response.setOrgId(orgId);
        return response;
    }

    public void validateOrg(Long orgId) {
        Organization organization = organizationRepository.findByOrgId(orgId);
        if (organization == null) {
            throw new EntityExistsException("Organization doesn't exist");
        }
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> getAllPolicies(String accountId, String timeZone) {
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        if (!accessDomainRepository.existsByAccountIdInAndRoleIdInAndIsActive(List.of(accountIdLong), List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new ValidationFailedException("You do not have permission to get all geo-fencing of the orgs");
        }
        List<AttendancePolicy> policies = policyRepository.findAll();
        return policies.stream()
            .map(policy -> PolicyResponse.fromEntity(policy, entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, policy.getOrgId()).orElse(null), timeZone))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GeoFenceOrgIdOrgName> getActiveGeofenceOrgIds(String accountIdsHeader) {
        List<Long> accountIds = Optional
                .ofNullable(jwtRequestFilter.getAccountIdsFromHeader(accountIdsHeader))
                .orElse(Collections.emptyList());
        if (accountIds.isEmpty()) return Collections.emptyList();

        List<Long> orgIds = Optional
                .ofNullable(userAccountRepository
                        .findAllOrgIdByAccountIdInAndIsActive(accountIds, true))
                .orElse(Collections.emptyList());
        if (orgIds.isEmpty()) return Collections.emptyList();

        List<Long> geofencingOrgIds = entityPreferenceRepository
                .findEntityIdsByEntityTypeIdAndEntityIdInAndIsGeoFencingAllowedAndIsGeoFencingActive(
                        Constants.EntityTypes.ORG, orgIds, true, true);

        if (geofencingOrgIds == null || geofencingOrgIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<GeoFenceOrgIdOrgName> geoFenceOrgIdOrgNameList = new ArrayList<>();
        for (Long orgId : geofencingOrgIds) {
            GeoFenceOrgIdOrgName geoFenceOrgIdOrgName = new GeoFenceOrgIdOrgName();
            Organization organization = organizationRepository.findByOrgId(orgId);
            UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(organization.getOwnerEmail(), organization.getOrgId(), true);
            geoFenceOrgIdOrgName.setOrgId(orgId);
            geoFenceOrgIdOrgName.setOrganizationName(organization.getOrganizationDisplayName());
            if (accountIds.contains(userAccount.getAccountId())) {
                geoFenceOrgIdOrgName.setIsGeoFenceAttendence(true);
                geoFenceOrgIdOrgName.setIsGeoFenceAdminPanel(true);

            } else {
                Boolean adminPanelAccess = userFeatureAccessRepository.existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(Constants.EntityTypes.ORG, orgId, accountIds, Constants.ActionId.MANAGE_GEOFENCE_ADMIN_PANEL);
                Boolean attendenceAccess = userFeatureAccessRepository.existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(Constants.EntityTypes.ORG, orgId, accountIds, Constants.ActionId.VIEW_GEOFENCE_ATTENDENCE);
                geoFenceOrgIdOrgName.setIsGeoFenceAdminPanel(adminPanelAccess);
                geoFenceOrgIdOrgName.setIsGeoFenceAttendence(attendenceAccess);
            }
            geoFenceOrgIdOrgNameList.add(geoFenceOrgIdOrgName);
        }
        return geoFenceOrgIdOrgNameList;
    }

    public String deactivateGeoFenceForOrg (Long orgId, String accountId) {

        Organization organization = organizationRepository.findByOrgId(orgId);
        if (organization == null) {
            throw new EntityExistsException("Organization doesn't exist");
        }
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        if (!accessDomainRepository.existsByAccountIdInAndRoleIdInAndIsActive(List.of(accountIdLong), List.of(RoleEnum.SUPER_ADMIN.getRoleId()), true)) {
            throw new ValidationFailedException("You do not have permission to deactivate geo-fencing to any org");
        }
        Optional<EntityPreference> entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (entityPreferenceOptional == null || entityPreferenceOptional.isEmpty()) {
            throw new EntityNotFoundException("Entity preference doesn't exist");
        }
        Optional<AttendancePolicy> attendancePolicyOptional = policyRepository.findByOrgId(orgId);
        if (attendancePolicyOptional == null || attendancePolicyOptional.isEmpty()) {
            throw new EntityNotFoundException("Geo fence policy doesn't exist for the org: " + organization.getOrganizationDisplayName());
        }

        EntityPreference entityPreference = entityPreferenceOptional.get();
        AttendancePolicy attendancePolicy = attendancePolicyOptional.get();

        if (!entityPreference.getIsGeoFencingAllowed() && !entityPreference.getIsGeoFencingActive() && !attendancePolicy.getIsActive()) {
            throw new ValidationFailedException("Geo fence is already deactivated for the org: " + organization.getOrganizationDisplayName());
        }

        entityPreference.setIsGeoFencingAllowed(false);
        entityPreference.setIsGeoFencingActive(false);
        entityPreferenceRepository.save(entityPreference);

        if (attendancePolicy.getIsActive()) {
            attendancePolicy.setIsActive(false);
            policyRepository.save(attendancePolicy);
        }

        return "Geo fence is deactivated for " + organization.getOrganizationDisplayName();
    }
}
