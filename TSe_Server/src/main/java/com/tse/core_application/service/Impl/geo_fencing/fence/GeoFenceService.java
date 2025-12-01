package com.tse.core_application.service.Impl.geo_fencing.fence;

import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.dto.geo_fence.fence.FenceCreateRequest;
import com.tse.core_application.dto.geo_fence.fence.FenceResponse;
import com.tse.core_application.dto.geo_fence.fence.FenceUpdateRequest;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.exception.geo_fencing.FenceNotFoundException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.geo_fencing.fence.GeoFence;
import com.tse.core_application.repository.geo_fencing.fence.GeoFenceRepository;
import com.tse.core_application.service.Impl.UserFeatureAccessService;
import com.tse.core_application.service.Impl.geo_fencing.policy.GeoFencingPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GeoFenceService {

    private final GeoFenceRepository fenceRepository;

    @Autowired
    private GeoFencingPolicyService geoFencingPolicyService;

    @Autowired
    private UserFeatureAccessService userFeatureAccessService;

    public GeoFenceService(GeoFenceRepository fenceRepository) {
        this.fenceRepository = fenceRepository;
    }

    @Transactional
    public FenceResponse createFence(Long orgId, FenceCreateRequest request, String accountId, String timeZone) {
        geoFencingPolicyService.validateOrg (orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAdminPanel(accountIdLong,orgId);
        if (!geoFencingPolicyService.validateUserRoleAccess (Constants.EntityTypes.ORG, orgId, accountIdLong, List.of(RoleEnum.ORG_ADMIN.getRoleId())) && !checkHrAccess) {
            throw new ValidationFailedException("You do not have permission to create the fence");
        }
        GeoFence fence = new GeoFence();
        fence.setOrgId(orgId);
        fence.setName(request.getName());
        fence.setLocationKind(request.getLocationKind());
        fence.setSiteCode(request.getSiteCode());
        fence.setTz(request.getTz());
        fence.setCenterLat(request.getCenterLat());
        fence.setCenterLng(request.getCenterLng());
        fence.setRadiusM(request.getRadiusM());
        fence.setIsActive(true); // Default to active for new fences

        fence.setCreatedBy(accountIdLong);

        fence = fenceRepository.save(fence);
        return FenceResponse.fromEntity(fence, timeZone);
    }

    @Transactional
    public FenceResponse updateFence(Long orgId, FenceUpdateRequest request, String accountId, String timeZone) {
        geoFencingPolicyService.validateOrg (orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAdminPanel(accountIdLong,orgId);
        if (!geoFencingPolicyService.validateUserRoleAccess (Constants.EntityTypes.ORG, orgId, accountIdLong, List.of(RoleEnum.ORG_ADMIN.getRoleId())) && !checkHrAccess) {
            throw new ValidationFailedException("You do not have permission to update the fence");
        }
        GeoFence fence = fenceRepository.findByIdAndOrgId(request.getId(), orgId)
            .orElseThrow(() -> new FenceNotFoundException(request.getId(), orgId));

        // Update fields
        fence.setName(request.getName());
        fence.setLocationKind(request.getLocationKind());
        fence.setSiteCode(request.getSiteCode());
        fence.setTz(request.getTz());
        fence.setCenterLat(request.getCenterLat());
        fence.setCenterLng(request.getCenterLng());
        fence.setRadiusM(request.getRadiusM());
        fence.setIsActive(request.getIsActive());

        fence.setUpdatedBy(accountIdLong);

        fence = fenceRepository.save(fence);
        return FenceResponse.fromEntity(fence, timeZone);
    }

    @Transactional(readOnly = true)
    public List<FenceResponse> listFences(Long orgId, String status, String q, String siteCode, String accountId, String timeZone) {
        geoFencingPolicyService.validateOrg (orgId);
        Long accountIdLong = null;
        try {
            accountIdLong = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Boolean checkHrAccess=userFeatureAccessService.checkHrAccessForGeoFencingAdminPanel(accountIdLong,orgId);
        if (!geoFencingPolicyService.validateUserRoleAccess (Constants.EntityTypes.ORG, orgId, accountIdLong, List.of(RoleEnum.ORG_ADMIN.getRoleId())) && !checkHrAccess) {
            throw new ValidationFailedException("You do not have permission to get the fences of organization");
        }
        Specification<GeoFence> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by orgId
            predicates.add(criteriaBuilder.equal(root.get("orgId"), orgId));

            // Filter by status (active/inactive/both)
            if (status != null && !status.equalsIgnoreCase("both")) {
                if (status.equalsIgnoreCase("active")) {
                    predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
                } else if (status.equalsIgnoreCase("inactive")) {
                    predicates.add(criteriaBuilder.isFalse(root.get("isActive")));
                }
            }

            // Filter by name search (case-insensitive)
            if (q != null && !q.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + q.toLowerCase() + "%"
                ));
            }

            // Filter by siteCode
            if (siteCode != null && !siteCode.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("siteCode"), siteCode));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        List<GeoFence> fences = fenceRepository.findAll(spec);
        return fences.stream()
            .map(fence -> FenceResponse.fromEntity(fence, timeZone))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FenceResponse> getAllFences(String timeZone) {
        List<GeoFence> fences = fenceRepository.findAll();
        return fences.stream()
            .map(fence -> FenceResponse.fromEntity(fence, timeZone))
            .collect(Collectors.toList());
    }
}
