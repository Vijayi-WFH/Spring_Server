package com.tse.core_application.model.geo_fencing.policy;

import com.tse.core_application.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_policy", schema= Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttendancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    // Activation & posture
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "outside_fence_policy", nullable = false)
    private OutsideFencePolicy outsideFencePolicy = OutsideFencePolicy.WARN;

    @Enumerated(EnumType.STRING)
    @Column(name = "integrity_posture", nullable = false)
    private IntegrityPosture integrityPosture = IntegrityPosture.WARN;

    // Windows (minutes)
    @Column(name = "allow_checkin_before_start_min", nullable = false)
    private Integer allowCheckinBeforeStartMin = 20;

    @Column(name = "late_checkin_after_start_min", nullable = false)
    private Integer lateCheckinAfterStartMin = 30;

    @Column(name = "allow_checkout_before_end_min", nullable = false)
    private Integer allowCheckoutBeforeEndMin = 15;

    @Column(name = "max_checkout_after_end_min", nullable = false)
    private Integer maxCheckoutAfterEndMin = 60;

    @Column(name = "notify_before_shift_start_min", nullable = false)
    private Integer notifyBeforeShiftStartMin = 10;

    // Geofence & accuracy
    @Column(name = "fence_radius_m", nullable = false)
    private Integer fenceRadiusM = 150;

    @Column(name = "accuracy_gate_m", nullable = false)
    private Integer accuracyGateM = 80;

    // Punch limits & cooldowns
    @Column(name = "cooldown_seconds", nullable = false)
    private Integer cooldownSeconds = 120;

    @Column(name = "max_successful_punches_per_day", nullable = false)
    private Integer maxSuccessfulPunchesPerDay = 6;

    @Column(name = "max_failed_punches_per_day", nullable = false)
    private Integer maxFailedPunchesPerDay = 6;

    // Working hours (soft guard)
    @Column(name = "max_working_hours_per_day", nullable = false)
    private Integer maxWorkingHoursPerDay = 10;

    // Punch request respond within minutes configuration
    @Column(name = "punch_respond_min_minutes", nullable = false)
    private Integer punchRespondMinMinutes = 2;

    @Column(name = "punch_respond_max_minutes", nullable = false)
    private Integer punchRespondMaxMinutes = 20;

    @Column(name = "punch_respond_default_minutes", nullable = false)
    private Integer punchRespondDefaultMinutes = 10;

    // Legacy placeholders
    @Column(name = "dwell_in_min", nullable = false)
    private Integer dwellInMin = 3;

    @Column(name = "dwell_out_min", nullable = false)
    private Integer dwellOutMin = 5;

    @Column(name = "auto_out_enabled", nullable = false)
    private Boolean autoOutEnabled = false;

    @Column(name = "auto_out_delay_min", nullable = false)
    private Integer autoOutDelayMin = 5;

    @Column(name = "undo_window_min", nullable = false)
    private Integer undoWindowMin = 5;

    // Audit
    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @Column(name = "updated_by")
    private Long updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_datetime", insertable = false)
    private LocalDateTime updatedDatetime;

    // Enums
    public enum OutsideFencePolicy {
        BLOCK, WARN
    }

    public enum IntegrityPosture {
        WARN, BLOCK
    }
}
