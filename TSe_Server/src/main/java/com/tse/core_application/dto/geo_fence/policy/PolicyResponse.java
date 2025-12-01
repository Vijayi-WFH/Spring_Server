package com.tse.core_application.dto.geo_fence.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tse.core_application.model.EntityPreference;
import com.tse.core_application.model.geo_fencing.policy.AttendancePolicy;
import com.tse.core_application.utils.DateTimeUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.Column;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyResponse {

    private Long policyId;
    private Long orgId;
    private String status;
    private Boolean defaultsApplied;
    private LocalDateTime updatedAt;

    // Full policy fields
    private Boolean isActive;
    private AttendancePolicy.OutsideFencePolicy outsideFencePolicy;
    private AttendancePolicy.IntegrityPosture integrityPosture;

    private Integer allowCheckinBeforeStartMin;
    private Integer lateCheckinAfterStartMin;
    private Integer allowCheckoutBeforeEndMin;
    private Integer maxCheckoutAfterEndMin;
    private Integer notifyBeforeShiftStartMin;

    private Integer fenceRadiusM;
    private Integer accuracyGateM;

    private Integer cooldownSeconds;
    private Integer maxSuccessfulPunchesPerDay;
    private Integer maxFailedPunchesPerDay;

    private Integer maxWorkingHoursPerDay;

    private Integer punchRespondMinMinutes;
    private Integer punchRespondMaxMinutes;
    private Integer punchRespondDefaultMinutes;

    private Integer dwellInMin;
    private Integer dwellOutMin;
    private Boolean autoOutEnabled;
    private Integer autoOutDelayMin;
    private Integer undoWindowMin;

    private Long createdBy;
    private LocalDateTime createdDatetime;
    private Long updatedBy;
    private LocalDateTime updatedDatetime;
    private Boolean isGeoFencingAllowed = false;
    private Boolean isGeoFencingActive = false;

    // Factory method for full policy
    public static PolicyResponse fromEntity(AttendancePolicy policy) {
        return fromEntity(policy, null, null);
    }

    // Factory method for full policy with timezone conversion
    public static PolicyResponse fromEntity(AttendancePolicy optPolicy, EntityPreference entityPreference, String timeZone) {

        AttendancePolicy policy = new AttendancePolicy();
        BeanUtils.copyProperties(optPolicy, policy);
        PolicyResponse response = new PolicyResponse();
        response.setPolicyId(policy.getId());
        response.setOrgId(policy.getOrgId());
        response.setIsActive(policy.getIsActive());
        response.setOutsideFencePolicy(policy.getOutsideFencePolicy());
        response.setIntegrityPosture(policy.getIntegrityPosture());
        response.setAllowCheckinBeforeStartMin(policy.getAllowCheckinBeforeStartMin());
        response.setLateCheckinAfterStartMin(policy.getLateCheckinAfterStartMin());
        response.setAllowCheckoutBeforeEndMin(policy.getAllowCheckoutBeforeEndMin());
        response.setMaxCheckoutAfterEndMin(policy.getMaxCheckoutAfterEndMin());
        response.setNotifyBeforeShiftStartMin(policy.getNotifyBeforeShiftStartMin());
        response.setFenceRadiusM(policy.getFenceRadiusM());
        response.setAccuracyGateM(policy.getAccuracyGateM());
        response.setCooldownSeconds(policy.getCooldownSeconds());
        response.setMaxSuccessfulPunchesPerDay(policy.getMaxSuccessfulPunchesPerDay());
        response.setMaxFailedPunchesPerDay(policy.getMaxFailedPunchesPerDay());
        response.setPunchRespondMinMinutes(policy.getPunchRespondMinMinutes());
        response.setPunchRespondMaxMinutes(policy.getPunchRespondMaxMinutes());
        response.setPunchRespondDefaultMinutes(policy.getPunchRespondDefaultMinutes());
        response.setMaxWorkingHoursPerDay(policy.getMaxWorkingHoursPerDay());
        response.setDwellInMin(policy.getDwellInMin());
        response.setDwellOutMin(policy.getDwellOutMin());
        response.setAutoOutEnabled(policy.getAutoOutEnabled());
        response.setAutoOutDelayMin(policy.getAutoOutDelayMin());
        response.setUndoWindowMin(policy.getUndoWindowMin());
        response.setCreatedBy(policy.getCreatedBy());
        // Convert timestamps from server timezone to user timezone
        if (timeZone != null) {
            response.setCreatedDatetime(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(policy.getCreatedDatetime(), timeZone));
            response.setUpdatedDatetime(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(policy.getUpdatedDatetime(), timeZone));
        } else {
            response.setCreatedDatetime(policy.getCreatedDatetime());
            response.setUpdatedDatetime(policy.getUpdatedDatetime());
        }
        response.setUpdatedBy(policy.getUpdatedBy());
        if (entityPreference != null) {
            response.setIsGeoFencingAllowed(entityPreference.getIsGeoFencingAllowed());
            response.setIsGeoFencingActive(entityPreference.getIsGeoFencingActive());
        }
        return response;
    }
}
