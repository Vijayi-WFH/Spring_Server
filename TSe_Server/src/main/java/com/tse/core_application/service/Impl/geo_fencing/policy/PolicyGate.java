package com.tse.core_application.service.Impl.geo_fencing.policy;

import com.tse.core_application.exception.geo_fencing.ProblemException;
import com.tse.core_application.model.geo_fencing.policy.AttendancePolicy;
import com.tse.core_application.repository.geo_fencing.policy.AttendancePolicyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Helper to check if geo-fencing policy is active for an org.
 * Honors the config toggle attendance.policy.skip-activation-check.
 */
@Component
public class PolicyGate {

    private final AttendancePolicyRepository policyRepository;

    @Value("${attendance.policy.skip-activation-check:true}")
    private boolean skipActivationCheck;

    public PolicyGate(AttendancePolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    /**
     * Asserts that geo-fencing policy is active for the given org.
     * If skipActivationCheck is true (demo mode), always passes.
     * Otherwise, throws GEOFENCING_INACTIVE if policy is missing or inactive.
     *
     * @param orgId the organization ID
     * @throws com.tse.core_application.exception.geo_fencing.ProblemException if policy is inactive
     */
    public void assertPolicyActive(long orgId) {

        Optional<AttendancePolicy> policyOpt = policyRepository.findByOrgId(orgId);
        if (!policyOpt.isPresent()) {
            throw new ProblemException(
                    HttpStatus.CONFLICT,
                    "GEOFENCING_INACTIVE",
                    "Geo-fencing policy not configured",
                    "Organization " + orgId + " does not have a geo-fencing policy configured"
            );
        }

        AttendancePolicy policy = policyOpt.get();
        if (!Boolean.TRUE.equals(policy.getIsActive())) {
            throw new ProblemException(
                    HttpStatus.CONFLICT,
                    "GEOFENCING_INACTIVE",
                    "Geo-fencing policy is inactive",
                    "Geo-fencing policy for organization " + orgId + " is not active"
            );
        }
    }

    /**
     * Checks if policy is active without throwing an exception.
     *
     * @param orgId the organization ID
     * @return true if policy is active or skipActivationCheck is true
     */
    public boolean isPolicyActive(long orgId) {
        if (skipActivationCheck) {
            return true;
        }

        Optional<AttendancePolicy> policyOpt = policyRepository.findByOrgId(orgId);
        return policyOpt.isPresent() && Boolean.TRUE.equals(policyOpt.get().getIsActive());
    }
}
