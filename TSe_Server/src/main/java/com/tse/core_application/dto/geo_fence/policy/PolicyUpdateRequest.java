package com.tse.core_application.dto.geo_fence.policy;

import com.tse.core_application.model.geo_fencing.policy.AttendancePolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PolicyUpdateRequest {

    private Long updatedBy;

    // Activation & posture
    @NotNull
    private Boolean isActive;

    @NotNull
    private AttendancePolicy.OutsideFencePolicy outsideFencePolicy;

    @NotNull
    private AttendancePolicy.IntegrityPosture integrityPosture;

    // Windows (minutes)
    @NotNull
    @Min(0)
    private Integer allowCheckinBeforeStartMin;

    @NotNull
    @Min(0)
    private Integer lateCheckinAfterStartMin;

    @NotNull
    @Min(0)
    private Integer allowCheckoutBeforeEndMin;

    @NotNull
    @Min(0)
    private Integer maxCheckoutAfterEndMin;

    @NotNull
    @Min(0)
    private Integer notifyBeforeShiftStartMin;

    // Geofence & accuracy
    @NotNull
    @Min(30)
    private Integer fenceRadiusM;

    @NotNull
    @Min(10)
    private Integer accuracyGateM;

    // Punch limits & cooldowns
    @NotNull
    @Min(0)
    private Integer cooldownSeconds;

    @NotNull
    @Min(0)
    private Integer maxSuccessfulPunchesPerDay;

    @NotNull
    @Min(0)
    private Integer maxFailedPunchesPerDay;

    // Working hours (soft guard)
    @NotNull
    @Min(0)
    private Integer maxWorkingHoursPerDay;

    // Legacy placeholders
    @NotNull
    @Min(0)
    private Integer dwellInMin;

    @NotNull
    @Min(0)
    private Integer dwellOutMin;

    @NotNull
    private Boolean autoOutEnabled;

    @NotNull
    @Min(0)
    private Integer autoOutDelayMin;

    @NotNull
    @Min(0)
    private Integer undoWindowMin;

    private Boolean isGeoFencingActive;

    @NotNull
    @Min(1)
    private Integer punchRespondMinMinutes;

    @NotNull
    @Min(1)
    private Integer punchRespondMaxMinutes;

    @NotNull
    @Min(1)
    private Integer punchRespondDefaultMinutes;
}
